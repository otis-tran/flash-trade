# Phase 2: Paging Infrastructure

**Estimated Effort:** 3-4 hours
**Dependencies:** Phase 1 (Database Layer)
**Status:** Pending

---

## Objectives

1. Implement TokenRemoteMediator for network-to-database synchronization
2. Handle LoadType.REFRESH, PREPEND, APPEND logic
3. Integrate SafeApiCall pattern for error handling
4. Manage TokenRemoteKeys for pagination state
5. Implement TTL-based cache invalidation (5-10 min)
6. Implement scroll position restoration (getRefreshKey)

---

## Architecture Pattern

RemoteMediator acts as the bridge between network and local database:

```
┌─────────────────────────────────────────────────────────┐
│             TokenRemoteMediator.load()                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. Determine load type (REFRESH/APPEND/PREPEND)        │
│  2. Calculate page to fetch                            │
│  3. Fetch from API using SafeApiCall                   │
│  4. Map DTOs to Entities                               │
│  5. Insert to Room in transaction:                     │
│     - Update TokenEntity table                         │
│     - Update TokenRemoteKeysEntity table               │
│  6. Return MediatorResult (Success/Error)              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Implementation Steps

### Step 2.1: Create TokenRemoteMediator

**File:** `app/src/main/java/com/otistran/flash_trade/data/paging/token-remote-mediator.kt`

**Content:**

```kotlin
package com.otistran.flash_trade.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.otistran.flash_trade.core.network.NetworkResult
import com.otistran.flash_trade.core.network.safeApiCall
import com.otistran.flash_trade.data.local.database.FlashTradeDatabase
import com.otistran.flash_trade.data.local.entity.TokenEntity
import com.otistran.flash_trade.data.local.entity.TokenRemoteKeysEntity
import com.otistran.flash_trade.data.mapper.TokenMapper
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.domain.model.TokenFilter
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "TokenRemoteMediator"
private const val STARTING_PAGE_INDEX = 1

/**
 * RemoteMediator for Token pagination.
 * Handles network-to-database sync with TTL-based cache invalidation.
 *
 * @param filter Token filter criteria (minTvl, sort order, etc.)
 * @param database Room database instance
 * @param kyberApi Kyber API service
 */
@OptIn(ExperimentalPagingApi::class)
class TokenRemoteMediator(
    private val filter: TokenFilter,
    private val database: FlashTradeDatabase,
    private val kyberApi: KyberApiService
) : RemoteMediator<Int, TokenEntity>() {

    private val tokenDao = database.tokenDao()

    companion object {
        // Cache TTL: 5 minutes (300 seconds)
        private val CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(5)
    }

    /**
     * Determines if cache should be invalidated based on TTL.
     */
    private suspend fun shouldInvalidateCache(): Boolean {
        val oldestKeyTime = tokenDao.getOldestKeyCreationTime() ?: return false
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - oldestKeyTime

        return cacheAge > CACHE_TTL_MILLIS
    }

    /**
     * Called before loading to determine if we should skip loading.
     * Returns Initialize.SKIP_INITIAL_REFRESH to use cached data if fresh.
     */
    override suspend fun initialize(): InitializeAction {
        val shouldRefresh = shouldInvalidateCache()

        Log.d(TAG, "initialize() - shouldRefresh: $shouldRefresh")

        return if (shouldRefresh) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    /**
     * Main load function called by Paging library.
     * Handles REFRESH, PREPEND, APPEND load types.
     */
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TokenEntity>
    ): MediatorResult {
        return try {
            Log.d(TAG, "load() - loadType: $loadType, state: ${state.pages.size} pages")

            // Determine page to load based on LoadType
            val page = when (loadType) {
                LoadType.REFRESH -> {
                    // Get refresh key from getRefreshKey() or start at page 1
                    val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                    remoteKeys?.nextPage?.minus(1) ?: STARTING_PAGE_INDEX
                }
                LoadType.PREPEND -> {
                    // We don't support prepending for this use case
                    // (only forward pagination from API)
                    val remoteKeys = getRemoteKeyForFirstItem(state)
                    val prevPage = remoteKeys?.prevPage
                    if (prevPage == null) {
                        // Already at first page
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    prevPage
                }
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    val nextPage = remoteKeys?.nextPage
                    if (nextPage == null) {
                        // End of pagination reached
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    nextPage
                }
            }

            Log.d(TAG, "Fetching page: $page with filter: $filter")

            // Fetch data from API using SafeApiCall
            val result = safeApiCall {
                kyberApi.getTokens(
                    minTvl = filter.minTvl,
                    maxTvl = filter.maxTvl,
                    minVolume = filter.minVolume,
                    maxVolume = filter.maxVolume,
                    sort = filter.sort.value,
                    page = page,
                    limit = filter.limit
                )
            }

            // Handle API result
            when (result) {
                is NetworkResult.Success -> {
                    val response = result.data
                    val tokens = TokenMapper.toDomainList(response.data)
                    val endOfPaginationReached = tokens.isEmpty() ||
                                                 page >= response.totalPages

                    Log.d(TAG, "API success - fetched ${tokens.size} tokens, " +
                              "endOfPagination: $endOfPaginationReached")

                    // Sync to database in transaction
                    database.withTransaction {
                        // Clear cache on REFRESH
                        if (loadType == LoadType.REFRESH) {
                            Log.d(TAG, "REFRESH - clearing cache")
                            tokenDao.clearAll()
                        }

                        // Calculate remote keys
                        val prevPage = if (page == STARTING_PAGE_INDEX) null else page - 1
                        val nextPage = if (endOfPaginationReached) null else page + 1

                        val keys = tokens.map { token ->
                            TokenRemoteKeysEntity(
                                tokenAddress = token.address,
                                prevPage = prevPage,
                                nextPage = nextPage,
                                createdAt = System.currentTimeMillis()
                            )
                        }

                        // Insert tokens
                        val entities = tokens.map { token ->
                            TokenEntity(
                                address = token.address,
                                name = token.name,
                                symbol = token.symbol,
                                decimals = token.decimals,
                                logoUrl = token.logoUrl,
                                isVerified = token.isVerified,
                                isWhitelisted = token.isWhitelisted,
                                isStable = token.isStable,
                                isHoneypot = token.isHoneypot,
                                totalTvl = token.totalTvl,
                                poolCount = token.poolCount,
                                cgkRank = token.cgkRank,
                                cmcRank = token.cmcRank,
                                cachedAt = System.currentTimeMillis()
                            )
                        }

                        tokenDao.insertRemoteKeys(keys)
                        tokenDao.insertTokens(entities)

                        Log.d(TAG, "Inserted ${entities.size} tokens and ${keys.size} keys")
                    }

                    MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
                }
                is NetworkResult.Error -> {
                    val exception = result.exception
                    Log.e(TAG, "API error: ${exception.message}", exception)

                    // Convert NetworkResult.Error to IOException for Paging library
                    // (Paging expects exceptions to be thrown)
                    MediatorResult.Error(
                        IOException("Failed to fetch tokens: ${exception.message}")
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "IOException during load", e)
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            Log.e(TAG, "HttpException during load", e)
            MediatorResult.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during load", e)
            MediatorResult.Error(e)
        }
    }

    /**
     * Get remote key for the item closest to current scroll position.
     * Used for REFRESH to restore scroll position.
     */
    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, TokenEntity>
    ): TokenRemoteKeysEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { token ->
                tokenDao.getRemoteKeyByTokenAddress(token.address)
            }
        }
    }

    /**
     * Get remote key for the first item in the list.
     * Used for PREPEND (though not needed for this use case).
     */
    private suspend fun getRemoteKeyForFirstItem(
        state: PagingState<Int, TokenEntity>
    ): TokenRemoteKeysEntity? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()?.let { token ->
            tokenDao.getRemoteKeyByTokenAddress(token.address)
        }
    }

    /**
     * Get remote key for the last item in the list.
     * Used for APPEND to determine next page.
     */
    private suspend fun getRemoteKeyForLastItem(
        state: PagingState<Int, TokenEntity>
    ): TokenRemoteKeysEntity? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { token ->
            tokenDao.getRemoteKeyByTokenAddress(token.address)
        }
    }
}
```

**Key Implementation Details:**

1. **TTL-based Cache Invalidation:**
   - `CACHE_TTL_MILLIS = 5 minutes`
   - `initialize()` checks oldest key timestamp
   - Returns `LAUNCH_INITIAL_REFRESH` if cache stale

2. **LoadType Handling:**
   - **REFRESH:** Clear cache, fetch page 1 (or refresh key page)
   - **APPEND:** Fetch next page based on last item's remote key
   - **PREPEND:** Return early (not needed for forward-only pagination)

3. **SafeApiCall Integration:**
   - Uses `safeApiCall { kyberApi.getTokens(...) }`
   - Maps `NetworkResult.Error` to `MediatorResult.Error(IOException)`
   - Maintains project's error handling patterns

4. **Transaction Safety:**
   - `database.withTransaction { }` ensures atomic updates
   - Clear cache → Insert keys → Insert tokens

5. **Remote Key Management:**
   - `prevPage = null` for first page
   - `nextPage = null` for last page (endOfPagination = true)
   - All tokens from same page share same prev/next keys

---

## Error Handling Strategy

### Network Errors
```kotlin
NetworkResult.Error → MediatorResult.Error(IOException)
```
Paging library retries automatically with exponential backoff.

### Empty Response
```kotlin
tokens.isEmpty() → endOfPaginationReached = true
```
Stops pagination gracefully.

### Database Errors
Caught by try-catch, returned as `MediatorResult.Error`.

---

## Testing Scenarios (Manual)

1. **First Load (REFRESH):**
   - Clear app data
   - Launch app
   - Verify first 100 tokens loaded from API
   - Verify database has 100 TokenEntity + 100 RemoteKeys

2. **Scroll to Load More (APPEND):**
   - Scroll to bottom
   - Verify next page loads
   - Verify database appends 100 more tokens

3. **Cache Hit (Initialize):**
   - Close and reopen app within 5 minutes
   - Verify tokens load from Room (no API call)
   - Verify scroll position restored

4. **Cache Miss (TTL Expiry):**
   - Wait 5+ minutes
   - Reopen app
   - Verify API called to refresh cache

5. **Network Error:**
   - Enable airplane mode
   - Pull to refresh
   - Verify error state shown
   - Verify cached data remains visible

6. **End of Pagination:**
   - Scroll to last page
   - Verify no more loads triggered
   - Verify UI shows "end of list" appropriately

---

## Performance Considerations

### Memory Optimization
- Only loads visible + prefetch window (default 20 items)
- Database pages data efficiently via PagingSource
- No full list in memory

### Database Optimization
- Indices on `address` (PK) and `total_tvl` (ORDER BY)
- UPSERT strategy (REPLACE on conflict)
- Transaction batching for inserts

### Network Optimization
- TTL cache reduces unnecessary API calls
- Page size 100 balances network/database overhead
- SafeApiCall handles retries and timeouts

---

## Verification Checklist

- [ ] TokenRemoteMediator created in `data/paging/` package
- [ ] `load()` method handles REFRESH/APPEND/PREPEND
- [ ] `initialize()` implements TTL-based cache check
- [ ] SafeApiCall integrated for API calls
- [ ] Database transactions used for atomic updates
- [ ] Remote keys calculated correctly (prev/next pages)
- [ ] Error handling returns MediatorResult.Error
- [ ] Logging added for debugging
- [ ] Project builds without errors

---

## Files Created (Summary)

1. `data/paging/token-remote-mediator.kt` (~195 lines)

**Total:** 1 new file, ~195 lines

---

## Files Modified (Summary)

None (self-contained implementation)

---

## Next Phase

Proceed to **Phase 3: Repository Integration** to wire RemoteMediator into TokenRepository.

# Phase 3: Repository Integration

**Estimated Effort:** 1-2 hours
**Dependencies:** Phase 1 (Database), Phase 2 (RemoteMediator)
**Status:** ✅ Complete (Reviewed 2025-12-26)

**Review Summary:**
- ✅ Build passed (compileDebugKotlin - 6s)
- ✅ API design follows Paging 3 best practices
- ✅ Dependency injection configured correctly
- ✅ Entity-domain mapping verified
- ✅ Backward compatibility maintained
- ⚠️ Minor suggestion: Remove redundant `tokenDao` constructor param
- 📄 Full review: `reports/251226-from-code-reviewer-to-orchestrator-phase-03-repository-integration-review.md`

---

## Objectives

1. Add `getPagedTokens()` method to TokenRepository interface
2. Implement method in TokenRepositoryImpl using Pager configuration
3. Wire RemoteMediator and PagingSource together
4. Map TokenEntity → Token domain model
5. Maintain backward compatibility with existing methods

---

## Implementation Steps

### Step 3.1: Update TokenRepository Interface

**File:** `app/src/main/java/com/otistran/flash_trade/domain/repository/TokenRepository.kt`

**Add new method:**

```kotlin
package com.otistran.flash_trade.domain.repository

import androidx.paging.PagingData
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.model.TokenListResult
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for token data operations.
 */
interface TokenRepository {

    /**
     * Get paginated token list with filters.
     */
    suspend fun getTokens(filter: TokenFilter = TokenFilter()): Result<TokenListResult>

    /**
     * Get single token by address.
     */
    suspend fun getTokenByAddress(address: String): Result<Token?>

    /**
     * Search tokens by name or symbol.
     */
    suspend fun searchTokens(query: String, limit: Int = 20): Result<List<Token>>

    /**
     * Get verified/whitelisted tokens only (safe to trade).
     */
    suspend fun getSafeTokens(page: Int = 1, limit: Int = 50): Result<TokenListResult>

    /**
     * Observe cached tokens (for offline support).
     */
    fun observeTokens(): Flow<List<Token>>

    // ==================== NEW: Paging 3 Support ====================

    /**
     * Get paginated token stream using Paging 3.
     * Supports offline-first with automatic network sync via RemoteMediator.
     *
     * @param filter Token filter criteria (minTvl, sort, etc.)
     * @return Flow of PagingData for Compose UI (LazyPagingItems)
     */
    fun getPagedTokens(filter: TokenFilter = TokenFilter()): Flow<PagingData<Token>>
}
```

---

### Step 3.2: Implement getPagedTokens() in TokenRepositoryImpl

**File:** `app/src/main/java/com/otistran/flash_trade/data/repository/TokenRepositoryImpl.kt`

**Add imports:**

```kotlin
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.otistran.flash_trade.data.local.database.FlashTradeDatabase
import com.otistran.flash_trade.data.mapper.TokenEntityMapper.toDomain
import com.otistran.flash_trade.data.paging.TokenRemoteMediator
```

**Update constructor to inject database:**

```kotlin
@Singleton
class TokenRepositoryImpl @Inject constructor(
    private val kyberApi: KyberApiService,
    private val database: FlashTradeDatabase // NEW: Add database injection
) : TokenRepository {

    private val tokenDao = database.tokenDao() // NEW: Get DAO

    // Existing in-memory cache (keep for backward compatibility)
    private val _cachedTokens = MutableStateFlow<List<Token>>(emptyList())

    // ... existing methods remain unchanged ...
```

**Add new method implementation:**

```kotlin
    /**
     * Get paginated token stream using Paging 3.
     * Room as single source of truth, RemoteMediator handles network sync.
     */
    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedTokens(filter: TokenFilter): Flow<PagingData<Token>> {
        return Pager(
            config = PagingConfig(
                pageSize = filter.limit,
                prefetchDistance = 20, // Prefetch 20 items before end
                enablePlaceholders = false, // Don't show null placeholders
                initialLoadSize = filter.limit * 2, // Load 2 pages initially (200 items)
                maxSize = 500 // Max items in memory (5 pages)
            ),
            remoteMediator = TokenRemoteMediator(
                filter = filter,
                database = database,
                kyberApi = kyberApi
            ),
            pagingSourceFactory = {
                // Room provides PagingSource automatically
                tokenDao.pagingSource()
            }
        ).flow.map { pagingData ->
            // Map TokenEntity → Token domain model
            pagingData.map { entity -> entity.toDomain() }
        }
    }
```

**Complete Modified File (TokenRepositoryImpl.kt):**

```kotlin
package com.otistran.flash_trade.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.otistran.flash_trade.data.local.database.FlashTradeDatabase
import com.otistran.flash_trade.data.mapper.TokenEntityMapper.toDomain
import com.otistran.flash_trade.data.mapper.TokenMapper.toDomain
import com.otistran.flash_trade.data.mapper.TokenMapper.toDomainList
import com.otistran.flash_trade.data.paging.TokenRemoteMediator
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.model.TokenListResult
import com.otistran.flash_trade.domain.model.TokenSortOrder
import com.otistran.flash_trade.domain.repository.TokenRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepositoryImpl @Inject constructor(
    private val kyberApi: KyberApiService,
    private val database: FlashTradeDatabase
) : TokenRepository {

    private val tokenDao = database.tokenDao()

    // In-memory cache (backward compatibility)
    private val _cachedTokens = MutableStateFlow<List<Token>>(emptyList())

    // ==================== Existing Methods (Unchanged) ====================

    override suspend fun getTokens(filter: TokenFilter): Result<TokenListResult> {
        return try {
            val response = kyberApi.getTokens(
                minTvl = filter.minTvl,
                maxTvl = filter.maxTvl,
                minVolume = filter.minVolume,
                maxVolume = filter.maxVolume,
                sort = filter.sort.value,
                page = filter.page,
                limit = filter.limit
            )

            val result = response.toDomain()

            // Update cache on first page
            if (filter.page == 1) {
                _cachedTokens.value = result.tokens
            } else {
                _cachedTokens.value = _cachedTokens.value + result.tokens
            }

            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch tokens", e)
        }
    }

    override suspend fun getTokenByAddress(address: String): Result<Token?> {
        return try {
            // First check cache
            val cached = _cachedTokens.value.find {
                it.address.equals(address, ignoreCase = true)
            }
            if (cached != null) {
                return Result.Success(cached)
            }

            // Fetch from API
            val response = kyberApi.getTokens(
                minTvl = 0.0,
                limit = 1
            )

            val token = response.data.find {
                it.address.equals(address, ignoreCase = true)
            }?.toDomain()

            Result.Success(token)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch token", e)
        }
    }

    override suspend fun searchTokens(query: String, limit: Int): Result<List<Token>> {
        return try {
            // Search in cache first
            val cached = _cachedTokens.value.filter { token ->
                token.name.contains(query, ignoreCase = true) ||
                token.symbol.contains(query, ignoreCase = true) ||
                token.address.contains(query, ignoreCase = true)
            }.take(limit)

            if (cached.isNotEmpty()) {
                return Result.Success(cached)
            }

            // Fetch more tokens if cache doesn't have results
            val response = kyberApi.getTokens(
                minTvl = 100.0,
                sort = TokenSortOrder.TVL_DESC.value,
                limit = 500
            )

            val filtered = response.data
                .toDomainList()
                .filter { token ->
                    token.name.contains(query, ignoreCase = true) ||
                    token.symbol.contains(query, ignoreCase = true) ||
                    token.address.contains(query, ignoreCase = true)
                }
                .take(limit)

            Result.Success(filtered)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Search failed", e)
        }
    }

    override suspend fun getSafeTokens(page: Int, limit: Int): Result<TokenListResult> {
        return try {
            val response = kyberApi.getTokens(
                minTvl = 10000.0,
                sort = TokenSortOrder.TVL_DESC.value,
                page = page,
                limit = limit
            )

            val result = response.toDomain()

            val safeTokens = result.tokens.filter { it.isSafe }

            Result.Success(
                result.copy(
                    tokens = safeTokens,
                    total = safeTokens.size
                )
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch safe tokens", e)
        }
    }

    override fun observeTokens(): Flow<List<Token>> = _cachedTokens.asStateFlow()

    // ==================== NEW: Paging 3 Implementation ====================

    /**
     * Get paginated token stream using Paging 3.
     * Room as single source of truth, RemoteMediator handles network sync.
     */
    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedTokens(filter: TokenFilter): Flow<PagingData<Token>> {
        return Pager(
            config = PagingConfig(
                pageSize = filter.limit,
                prefetchDistance = 20,
                enablePlaceholders = false,
                initialLoadSize = filter.limit * 2,
                maxSize = 500
            ),
            remoteMediator = TokenRemoteMediator(
                filter = filter,
                database = database,
                kyberApi = kyberApi
            ),
            pagingSourceFactory = {
                tokenDao.pagingSource()
            }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }
}
```

---

## Configuration Explanation

### PagingConfig Parameters

```kotlin
PagingConfig(
    pageSize = 100,              // Items per page (matches API)
    prefetchDistance = 20,       // Load next page when 20 items from end
    enablePlaceholders = false,  // Don't show null items (simpler UI)
    initialLoadSize = 200,       // Load 2 pages on first load (faster UX)
    maxSize = 500                // Max items in memory (prevents OOM)
)
```

**Rationale:**
- **pageSize = 100:** Matches API page size for 1:1 network/UI mapping
- **prefetchDistance = 20:** Prefetch next page when user scrolls to 80th item
- **enablePlaceholders = false:** Simplifies UI (no null handling needed)
- **initialLoadSize = 200:** Loads 2 pages initially for smoother scrolling
- **maxSize = 500:** Keeps memory bounded (5 pages max, ~2.5MB)

### Flow Mapping

```kotlin
.flow.map { pagingData ->
    pagingData.map { entity -> entity.toDomain() }
}
```

Two-level mapping:
1. Outer `map`: Transforms `Flow<PagingData<TokenEntity>>` → `Flow<PagingData<Token>>`
2. Inner `map`: Transforms each `TokenEntity` → `Token` domain model

---

## Dependency Injection Update

### Step 3.3: Update Hilt Module (if needed)

**File:** `di/database-module.kt` or `di/token-module.kt`

Ensure FlashTradeDatabase is provided:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FlashTradeDatabase {
        return Room.databaseBuilder(
            context,
            FlashTradeDatabase::class.java,
            "flash_trade_db"
        )
        .fallbackToDestructiveMigration() // OK for development
        .build()
    }

    @Provides
    fun provideTokenDao(database: FlashTradeDatabase): TokenDao {
        return database.tokenDao()
    }

    @Provides
    fun provideTransactionDao(database: FlashTradeDatabase): TransactionDao {
        return database.transactionDao()
    }
}
```

---

## Testing Strategy (Manual)

### Test 1: Paging Flow Works
1. Open TradingScreen (after Phase 4 implementation)
2. Verify first 200 tokens load (initialLoadSize)
3. Scroll to bottom
4. Verify next 100 tokens load automatically
5. Continue scrolling → verify continuous loading

### Test 2: Offline Support
1. Load tokens with network on
2. Enable airplane mode
3. Kill and restart app
4. Verify tokens load from Room cache
5. Verify scroll position restored

### Test 3: Backward Compatibility
1. Call old `getTokens()` method
2. Verify in-memory cache still works
3. Verify no conflicts with paged version

### Test 4: Error Handling
1. Enable airplane mode with empty cache
2. Launch app
3. Verify error state shown
4. Re-enable network
5. Pull to refresh → verify loads

---

## Verification Checklist

- [ ] TokenRepository interface updated with `getPagedTokens()`
- [ ] TokenRepositoryImpl constructor injects FlashTradeDatabase
- [ ] `getPagedTokens()` implemented with Pager configuration
- [ ] Flow mapping applied (TokenEntity → Token)
- [ ] Existing methods remain unchanged (backward compatibility)
- [ ] Hilt module provides FlashTradeDatabase
- [ ] Project builds without errors
- [ ] No compilation warnings

---

## Files Modified (Summary)

1. `domain/repository/TokenRepository.kt` (+12 lines for new method)
2. `data/repository/TokenRepositoryImpl.kt` (+30 lines for implementation)
3. `di/database-module.kt` (if not already providing database, +20 lines)

**Total:** 2-3 files modified, ~60 lines added

---

## Next Phase

Proceed to **Phase 4: UI Integration** to update ViewModel and Composable.

package com.otistran.flash_trade.data.sync

import android.util.Log
import com.otistran.flash_trade.data.datastore.SyncPreferences
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.local.entity.TokenEntity
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.data.remote.dto.kyber.TokenDto
import com.otistran.flash_trade.domain.model.SyncState
import com.otistran.flash_trade.domain.sync.TokenSyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenSyncManagerImpl @Inject constructor(
    private val kyberApi: KyberApiService,
    private val tokenDao: TokenDao,
    private val syncPreferences: SyncPreferences
) : TokenSyncManager {
    
    companion object {
        private const val TAG = "TokenSyncManager"
        
        // Sync config
        private const val MIN_TVL_THRESHOLD = 10_000.0
        private const val PAGE_SIZE = 100
        private const val CACHE_TTL_MS = 60 * 60 * 1000L  // 1 hour
        private const val REQUEST_DELAY_MS = 100L         // Delay between API calls
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val syncMutex = Mutex()
    
    override suspend fun shouldSync(): Boolean {
        val lastSync = syncPreferences.getLastSyncTime()
        val tokenCount = tokenDao.getTokenCount()
        
        return when {
            tokenCount == 0 -> {
                Log.d(TAG, "shouldSync: true (DB empty)")
                true
            }
            System.currentTimeMillis() - lastSync > CACHE_TTL_MS -> {
                Log.d(TAG, "shouldSync: true (cache stale)")
                true
            }
            else -> {
                Log.d(TAG, "shouldSync: false (cache fresh)")
                false
            }
        }
    }
    
    override suspend fun checkAndStartSync() {
        if (shouldSync()) {
            startSync()
        }
    }
    
    override suspend fun forceSync() {
        startSync()
    }
    
    private suspend fun startSync() = withContext(Dispatchers.IO) {
        // Prevent concurrent syncs
        if (!syncMutex.tryLock()) {
            Log.d(TAG, "Sync already in progress, skipping")
            return@withContext
        }
        
        try {
            performSync()
        } finally {
            syncMutex.unlock()
        }
    }
    
    private suspend fun performSync() {
        val startTime = System.currentTimeMillis()
        var currentPage = 1
        var totalTokensFetched = 0
        var totalPages: Int? = null
        
        Log.d(TAG, "Starting token sync...")
        
        // Increment generation for this sync session
        val generation = syncPreferences.incrementGeneration()
        Log.d(TAG, "Sync generation: $generation")
        
        try {
            while (true) {
                _syncState.value = SyncState.Syncing(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    tokensFetched = totalTokensFetched
                )
                
                // Fetch page with retry
                val response = fetchPageWithRetry(currentPage)
                
                if (response == null) {
                    // Max retries exceeded
                    _syncState.value = SyncState.Error(
                        message = "Failed to fetch page $currentPage after $MAX_RETRIES retries",
                        lastSuccessfulPage = currentPage - 1,
                        canRetry = true
                    )
                    return
                }
                
                // Update total pages from response
                if (totalPages == null) {
                    totalPages = response.totalPages
                    Log.d(TAG, "Total pages to sync: $totalPages")
                }
                
                // Check for empty data - sync complete
                if (response.data.isEmpty()) {
                    Log.d(TAG, "Empty response at page $currentPage - sync complete")
                    break
                }
                
                // Convert and save to DB
                val entities = response.data
                    .filter { isValidToken(it) }
                    .map { it.toEntity(generation) }
                
                if (entities.isNotEmpty()) {
                    tokenDao.upsertTokens(entities)
                    totalTokensFetched += entities.size
                    Log.d(TAG, "Page $currentPage: saved ${entities.size} tokens (total: $totalTokensFetched)")
                }
                
                // Check if we've reached the last page
                if (currentPage >= (totalPages ?: Int.MAX_VALUE)) {
                    Log.d(TAG, "Reached last page ($currentPage)")
                    break
                }
                
                currentPage++
                
                // Small delay to avoid rate limiting
                delay(REQUEST_DELAY_MS)
            }
            
            // Cleanup: Delete tokens not seen in this sync
            val deletedCount = tokenDao.deleteStaleTokens(generation)
            Log.d(TAG, "Deleted $deletedCount stale tokens")
            
            // Update sync metadata
            syncPreferences.updateLastSyncTime()
            syncPreferences.updateTotalTokensCached(tokenDao.getTokenCount())
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Sync completed: $totalTokensFetched tokens in ${duration}ms")
            
            _syncState.value = SyncState.Completed(
                totalTokensSynced = totalTokensFetched,
                tokensDeleted = deletedCount,
                durationMs = duration
            )
            
        } catch (e: CancellationException) {
            Log.d(TAG, "Sync cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            _syncState.value = SyncState.Error(
                message = e.message ?: "Unknown error",
                lastSuccessfulPage = currentPage - 1,
                canRetry = true
            )
        }
    }
    
    private suspend fun fetchPageWithRetry(page: Int): com.otistran.flash_trade.data.remote.dto.kyber.TokenListResponse? {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                return kyberApi.getTokens(
                    minTvl = MIN_TVL_THRESHOLD,
                    sort = "tvl_desc",
                    page = page,
                    limit = PAGE_SIZE
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Fetch page $page failed (attempt ${attempt + 1}/$MAX_RETRIES): ${e.message}")
                
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        
        Log.e(TAG, "Failed to fetch page $page after $MAX_RETRIES retries", lastException)
        return null
    }
    
    private fun isValidToken(dto: TokenDto): Boolean {
        // Filter out tokens without name or symbol
        return !dto.name.isNullOrBlank() && !dto.symbol.isNullOrBlank()
    }
    
    private fun TokenDto.toEntity(generation: Int): TokenEntity {
        return TokenEntity(
            address = address,
            name = name,
            symbol = symbol,
            decimals = decimals,
            logoUrl = logoUrl,
            isVerified = isVerified,
            isWhitelisted = isWhitelisted,
            isStable = isStable,
            isHoneypot = isHoneypot ?: false,
            isFot = isFot ?: false,
            tax = tax ?: 0.0,
            totalTvl = totalTvlAllPools?.toDoubleOrNull() ?: 0.0,
            poolCount = poolCount,
            maxPoolTvl = maxPoolTvl?.toDoubleOrNull(),
            maxPoolVolume = maxPoolVolume?.toDoubleOrNull(),
            avgPoolTvl = avgPoolTvl?.toDoubleOrNull(),
            cgkRank = cgkRank,
            cmcRank = cmcRank,
            websites = websites,
            earliestPoolCreatedAt = earliestPoolCreatedAt,
            cachedAt = System.currentTimeMillis(),
            syncGeneration = generation
        )
    }
}
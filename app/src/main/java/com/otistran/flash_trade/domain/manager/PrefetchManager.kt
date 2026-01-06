package com.otistran.flash_trade.domain.manager

import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.mapper.TokenMapper.toEntityList
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.domain.model.TokenSortOrder
import com.otistran.flash_trade.domain.repository.SwapRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFETCH_TOKEN_COUNT = 100
private const val PREFETCH_MIN_TVL = 10000.0
private const val CACHE_TTL_MILLIS = 5 * 60 * 1000L // 5 minutes

/**
 * Orchestrates data prefetching during app startup.
 * Fetches tokens to Room and swap routes in parallel for instant display.
 */
@Singleton
class PrefetchManager @Inject constructor(
    private val kyberApi: KyberApiService,
    private val tokenDao: TokenDao,
    private val swapRepository: SwapRepository
) {
    private val mutex = Mutex()
    private var isLoading = false

    /**
     * Prefetch tokens and quotes in parallel.
     * Thread-safe - will skip if already loading.
     */
    suspend fun prefetch() {
        mutex.withLock {
            if (isLoading) {
                Timber.d("Prefetch already in progress, skipping")
                return
            }
            isLoading = true
        }

        Timber.d("Starting prefetch...")
        val startTime = System.currentTimeMillis()

        try {
            coroutineScope {
                // Only prefetch tokens - routes fetched on-demand during swap
                async { prefetchTokens() }.await()
                // Route prefetch disabled - only fetch when user performs swap
                // async { prefetchRoutes() }.await()
            }
        } finally {
            mutex.withLock { isLoading = false }
        }

        val duration = System.currentTimeMillis() - startTime
        Timber.d("Prefetch completed in ${duration}ms")
    }

    /**
     * Prefetch tokens directly to Room for instant display on TradingScreen.
     * Skips if cache is fresh (within TTL), clears and refetches if stale.
     */
    private suspend fun prefetchTokens() {
        try {
            val startTime = System.currentTimeMillis()

            // Check if Room has fresh tokens (skip if within TTL)
            val existingCount = tokenDao.getTokenCount()
            if (existingCount > 0) {
                // Check TTL - if any token is older than 5 min, clear and refetch
                val ttlThreshold = System.currentTimeMillis() - CACHE_TTL_MILLIS
                val staleToken = tokenDao.getStaleToken(ttlThreshold)

                if (staleToken == null) {
                    Timber.d("Token cache fresh ($existingCount tokens), skipping prefetch")
                    return
                }

                Timber.d("Token cache stale, clearing and refetching...")
                tokenDao.clearAll()
            }

            // Fetch first page from API
            val response = kyberApi.getTokens(
                minTvl = PREFETCH_MIN_TVL,
                sort = TokenSortOrder.TVL_DESC.value,
                page = 1,
                limit = PREFETCH_TOKEN_COUNT
            )

            // Insert directly to Room
            val entities = response.data.toEntityList()
            tokenDao.insertTokens(entities)

            val duration = System.currentTimeMillis() - startTime
            Timber.d("Prefetched ${entities.size} tokens to Room in ${duration}ms")
        } catch (e: Exception) {
            Timber.w("Token prefetch failed: ${e.message}")
        }
    }
}

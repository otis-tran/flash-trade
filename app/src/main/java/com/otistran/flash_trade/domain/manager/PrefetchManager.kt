package com.otistran.flash_trade.domain.manager

import android.util.Log
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.mapper.TokenMapper.toEntityList
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.domain.model.TokenSortOrder
import com.otistran.flash_trade.domain.repository.SwapRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PrefetchManager"
private const val PREFETCH_TOKEN_COUNT = 100
private const val PREFETCH_MIN_TVL = 10000.0
private const val CACHE_TTL_MILLIS = 5 * 60 * 1000L // 5 minutes

/**
 * Orchestrates data prefetching during app startup.
 * Fetches tokens to Room and swap quotes in parallel for instant display.
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
                Log.d(TAG, "Prefetch already in progress, skipping")
                return
            }
            isLoading = true
        }

        Log.d(TAG, "Starting prefetch...")
        val startTime = System.currentTimeMillis()

        try {
            coroutineScope {
                val tokenJob = async { prefetchTokens() }
                val quoteJob = async { prefetchQuotes() }

                tokenJob.await()
                quoteJob.await()
            }
        } finally {
            mutex.withLock { isLoading = false }
        }

        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Prefetch completed in ${duration}ms")
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
                    Log.d(TAG, "Token cache fresh ($existingCount tokens), skipping prefetch")
                    return
                }

                Log.d(TAG, "Token cache stale, clearing and refetching...")
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
            Log.d(TAG, "Prefetched ${entities.size} tokens to Room in ${duration}ms")
        } catch (e: Exception) {
            Log.w(TAG, "Token prefetch failed: ${e.message}")
        }
    }

    private suspend fun prefetchQuotes() = coroutineScope {
        // Fetch all quotes in parallel for faster prefetch
        QuoteCacheManager.getPopularPairs().map { (tokenIn, tokenOut) ->
            async {
                try {
                    swapRepository.getQuote(
                        chain = "base",
                        tokenIn = tokenIn,
                        tokenOut = tokenOut,
                        amountIn = QuoteCacheManager.DEFAULT_PRELOAD_AMOUNT,
                        userAddress = null
                    )
                    Log.d(TAG, "Quote prefetched: $tokenIn → $tokenOut")
                } catch (e: Exception) {
                    Log.w(TAG, "Quote prefetch failed for $tokenIn → $tokenOut: ${e.message}")
                }
            }
        }.awaitAll()
    }
}

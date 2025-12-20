package com.otistran.flash_trade.domain.manager

import android.util.Log
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.domain.repository.TokenRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PrefetchManager"

/**
 * Orchestrates data prefetching during app startup.
 * Fetches tokens and swap quotes in parallel for instant display.
 */
@Singleton
class PrefetchManager @Inject constructor(
    private val tokenRepository: TokenRepository,
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

    private suspend fun prefetchTokens() {
        try {
            val filter = TokenFilter(minTvl = 10000.0, limit = 50)
            tokenRepository.getTokens(filter)
            Log.d(TAG, "Tokens prefetched successfully")
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

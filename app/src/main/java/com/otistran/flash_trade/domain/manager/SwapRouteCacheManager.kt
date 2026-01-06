package com.otistran.flash_trade.domain.manager

import com.otistran.flash_trade.domain.model.RouteSummaryResponse
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe cache for swap routes with TTL.
 * Caches RouteSummary responses from GET /api/v1/routes.
 */
@Singleton
class SwapRouteCacheManager @Inject constructor() {

    private data class CachedRoute(
        val route: RouteSummaryResponse,
        val expiresAt: Long
    )

    private val cache = ConcurrentHashMap<String, CachedRoute>()

    companion object {
        private const val TTL_SECONDS = 5L

        // Native ETH address for KyberSwap
        const val NATIVE_TOKEN = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"

        // Popular tokens on Base chain
        const val USDC_BASE = "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"
        const val WETH_BASE = "0x4200000000000000000000000000000000000006"

        // Default amount for preloading (0.1 ETH in wei)
        val DEFAULT_PRELOAD_AMOUNT: BigInteger = BigInteger("100000000000000000")

        /**
         * Get popular trading pairs for preloading.
         * Returns list of (tokenIn, tokenOut) pairs.
         */
        fun getPopularPairs(): List<Pair<String, String>> = listOf(
            Pair(NATIVE_TOKEN, USDC_BASE),  // ETH → USDC
            Pair(USDC_BASE, NATIVE_TOKEN),  // USDC → ETH
            Pair(NATIVE_TOKEN, WETH_BASE)   // ETH → WETH
        )
    }

    /**
     * Get cached route if not expired.
     */
    fun get(tokenIn: String, tokenOut: String, amountIn: String): RouteSummaryResponse? {
        val key = createKey(tokenIn, tokenOut, amountIn)
        val cached = cache[key] ?: return null

        val now = System.currentTimeMillis() / 1000
        return if (now < cached.expiresAt) {
            cached.route
        } else {
            cache.remove(key)
            null
        }
    }

    /**
     * Invalidate cached route.
     */
    fun invalidate(tokenIn: String, tokenOut: String, amountIn: String) {
        val key = createKey(tokenIn, tokenOut, amountIn)
        cache.remove(key)
    }

    /**
     * Clear all cached routes.
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Get current cache size (for monitoring).
     */
    fun size(): Int = cache.size

    /**
     * Check if a route is cached (without retrieving).
     */
    fun isCached(tokenIn: String, tokenOut: String, amountIn: String): Boolean {
        val key = createKey(tokenIn, tokenOut, amountIn)
        val cached = cache[key] ?: return false
        val now = System.currentTimeMillis() / 1000
        return now < cached.expiresAt
    }

    private fun createKey(tokenIn: String, tokenOut: String, amountIn: String): String {
        return "${tokenIn.lowercase()}_${tokenOut.lowercase()}_$amountIn"
    }
}

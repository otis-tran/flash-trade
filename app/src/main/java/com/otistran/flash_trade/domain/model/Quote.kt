package com.otistran.flash_trade.domain.model

import java.math.BigInteger

/**
 * Swap quote with pricing and routing information.
 * @param tokenIn Input token address
 * @param tokenOut Output token address
 * @param amountIn Input amount in wei
 * @param amountOut Expected output amount in wei
 * @param amountOutUsd Output value in USD
 * @param gas Estimated gas units
 * @param gasUsd Gas cost in USD
 * @param routerAddress KyberSwap router contract address
 * @param routeId Unique route identifier
 * @param timestamp Quote creation timestamp (Unix seconds)
 */
data class Quote(
    val tokenIn: String,
    val tokenOut: String,
    val amountIn: BigInteger,
    val amountOut: BigInteger,
    val amountOutUsd: String,
    val gas: BigInteger,
    val gasUsd: String,
    val routerAddress: String,
    val routeId: String,
    val timestamp: Long
) {
    companion object {
        /** Quote time-to-live in seconds */
        private const val TTL_SECONDS = 5L
    }

    /**
     * Check if quote is expired (5s TTL).
     */
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis() / 1000
        return (now - timestamp) > TTL_SECONDS
    }
}

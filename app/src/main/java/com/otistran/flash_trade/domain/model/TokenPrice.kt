package com.otistran.flash_trade.domain.model

/**
 * Domain model for token price data.
 */
data class TokenPrice(
    val address: String,
    val priceUsd: Double,
    val lastUpdatedAt: Long
) {
    /**
     * Format price for display.
     */
    val formattedPrice: String
        get() = when {
            priceUsd >= 1000 -> "$${String.format("%,.2f", priceUsd)}"
            priceUsd >= 1 -> "$${String.format("%.2f", priceUsd)}"
            priceUsd >= 0.01 -> "$${String.format("%.4f", priceUsd)}"
            priceUsd >= 0.0001 -> "$${String.format("%.6f", priceUsd)}"
            else -> "$${String.format("%.8f", priceUsd)}"
        }
}

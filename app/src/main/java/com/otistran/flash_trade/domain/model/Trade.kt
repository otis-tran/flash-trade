package com.otistran.flash_trade.domain.model

/**
 * Domain entity representing a trade transaction.
 */
data class Trade(
    val id: String,
    val tokenAddress: String,
    val tokenSymbol: String,
    val amount: Double,
    val buyPriceUsd: Double,
    val sellPriceUsd: Double? = null,
    val status: TradeStatus,
    val autoSellTimestamp: Long,
    val createdAt: Long,
    val walletAddress: String
)

enum class TradeStatus {
    PENDING,
    COMPLETED,
    SOLD,
    FAILED
}

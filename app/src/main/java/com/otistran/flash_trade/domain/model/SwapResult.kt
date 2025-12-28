package com.otistran.flash_trade.domain.model

/**
 * Result of swap execution.
 * @param txHash Transaction hash
 * @param status Transaction status
 * @param timestamp Submission timestamp
 */
data class SwapResult(
    val txHash: String,
    val status: SwapStatus,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Swap transaction status.
 */
enum class SwapStatus {
    /** Submitted to network */
    PENDING,
    /** Mined and confirmed */
    CONFIRMED,
    /** Reverted or rejected */
    FAILED,
    /** User cancelled */
    CANCELLED
}

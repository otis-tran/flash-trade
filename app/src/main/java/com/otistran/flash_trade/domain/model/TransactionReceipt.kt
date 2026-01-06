package com.otistran.flash_trade.domain.model

import java.math.BigInteger

/**
 * Domain model for Ethereum transaction receipt.
 * Used to verify on-chain execution status after transaction broadcast.
 */
data class TransactionReceipt(
    /** Transaction hash (0x-prefixed) */
    val transactionHash: String,

    /** Execution status: true=success (0x1), false=reverted (0x0) */
    val status: Boolean,

    /** Block number where tx was included */
    val blockNumber: BigInteger,

    /** Actual gas consumed by execution */
    val gasUsed: BigInteger
)

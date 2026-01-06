package com.otistran.flash_trade.domain.model

import java.math.BigInteger

/**
 * Result of quick buy operation.
 */
data class QuickBuyResult(
    val txHash: String,
    val tokenSymbol: String,
    val amountIn: BigInteger,
    val estimatedAmountOut: BigInteger,
    val autoSellTime: Long
)

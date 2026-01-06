package com.otistran.flash_trade.domain.model

import com.otistran.flash_trade.core.util.StablecoinType
import java.math.BigInteger

/**
 * Parameters for quick buy operation.
 */
data class QuickBuyParams(
    val chain: NetworkMode,
    val stablecoin: StablecoinType,
    val amountIn: BigInteger,      // Amount in stablecoin (6 decimals)
    val targetToken: Token
)

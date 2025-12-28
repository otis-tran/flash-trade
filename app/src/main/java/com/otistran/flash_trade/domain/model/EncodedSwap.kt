package com.otistran.flash_trade.domain.model

import java.math.BigInteger

/**
 * Encoded swap transaction ready for signing.
 * @param calldata Hex-encoded contract call data
 * @param routerAddress Target contract address
 * @param value Native token value to send (wei)
 * @param gas Estimated gas limit
 * @param amountOut Expected output amount
 */
data class EncodedSwap(
    val calldata: String,
    val routerAddress: String,
    val value: BigInteger,
    val gas: BigInteger,
    val amountOut: BigInteger
)

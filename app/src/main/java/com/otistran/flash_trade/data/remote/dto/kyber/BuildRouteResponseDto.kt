package com.otistran.flash_trade.data.remote.dto.kyber

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from KyberSwap Aggregator POST /{chain}/api/v1/route/build.
 * Contains encoded transaction data for swap execution.
 */
@JsonClass(generateAdapter = true)
data class BuildRouteResponseDto(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: EncodedSwapDataDto?,
    @Json(name = "requestId") val requestId: String?
)

/**
 * Encoded swap data ready for blockchain transaction.
 */
@JsonClass(generateAdapter = true)
data class EncodedSwapDataDto(
    /** Final input amount in wei */
    @Json(name = "amountIn") val amountIn: String,
    /** Final input amount in USD */
    @Json(name = "amountInUsd") val amountInUsd: String?,
    /** Final output amount in wei */
    @Json(name = "amountOut") val amountOut: String,
    /** Final output amount in USD */
    @Json(name = "amountOutUsd") val amountOutUsd: String?,
    /** Estimated gas limit */
    @Json(name = "gas") val gas: String,
    /** Estimated gas cost in USD */
    @Json(name = "gasUsd") val gasUsd: String,
    /** Additional costs (L1 fee for L2s) */
    @Json(name = "additionalCostUsd") val additionalCostUsd: String?,
    /** Description of additional costs */
    @Json(name = "additionalCostMessage") val additionalCostMessage: String?,
    /** Hex-encoded calldata for router contract */
    @Json(name = "data") val data: String,
    /** KyberSwap router contract address */
    @Json(name = "routerAddress") val routerAddress: String,
    /** Native token value to send (for ETH swaps) */
    @Json(name = "transactionValue") val transactionValue: String
)

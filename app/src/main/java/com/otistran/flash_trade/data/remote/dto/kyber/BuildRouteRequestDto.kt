package com.otistran.flash_trade.data.remote.dto.kyber

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for KyberSwap Aggregator POST /{chain}/api/v1/route/build.
 * Generates encoded transaction data for swap execution.
 */
@JsonClass(generateAdapter = true)
data class BuildRouteRequestDto(
    /** Route summary from GET /routes response - must be exact match */
    @Json(name = "routeSummary") val routeSummary: RouteSummaryDto,
    /** Address transferring input tokens (user wallet) */
    @Json(name = "sender") val sender: String,
    /** Address receiving output tokens (usually same as sender) */
    @Json(name = "recipient") val recipient: String,
    /** Slippage tolerance in bps (10 = 0.1%, max 2000 = 20%) */
    @Json(name = "slippageTolerance") val slippageTolerance: Int = 50,
    /** Transaction deadline in Unix seconds (default +20min if null) */
    @Json(name = "deadline") val deadline: Long? = null,
    /** Encoded ERC20 permit for gasless approval (optional) */
    @Json(name = "permit") val permit: String? = null,
    /** Enable RPC gas estimation to detect reverts */
    @Json(name = "enableGasEstimation") val enableGasEstimation: Boolean = true,
    /** Client identifier for tracking (matches x-client-id header) */
    @Json(name = "source") val source: String = "FlashTrade",
    /** User wallet address for rate limit optimization */
    @Json(name = "origin") val origin: String? = null
)

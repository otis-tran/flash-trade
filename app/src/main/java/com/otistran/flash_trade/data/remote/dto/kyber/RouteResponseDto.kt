package com.otistran.flash_trade.data.remote.dto.kyber

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from KyberSwap Aggregator GET /{chain}/api/v1/routes.
 * Contains optimal swap route with pricing and gas estimates.
 */
@JsonClass(generateAdapter = true)
data class RouteResponseDto(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: RouteDataDto?,
    @Json(name = "requestId") val requestId: String?
)

@JsonClass(generateAdapter = true)
data class RouteDataDto(
    @Json(name = "routeSummary") val routeSummary: RouteSummaryDto,
    @Json(name = "routerAddress") val routerAddress: String
)

/**
 * Route summary containing swap details.
 * This object is reused in BuildRouteRequestDto.
 */
@JsonClass(generateAdapter = true)
data class RouteSummaryDto(
    @Json(name = "tokenIn") val tokenIn: String,
    @Json(name = "amountIn") val amountIn: String,
    @Json(name = "amountInUsd") val amountInUsd: String,
    @Json(name = "tokenOut") val tokenOut: String,
    @Json(name = "amountOut") val amountOut: String,
    @Json(name = "amountOutUsd") val amountOutUsd: String,
    @Json(name = "gas") val gas: String,
    @Json(name = "gasPrice") val gasPrice: String,
    @Json(name = "gasUsd") val gasUsd: String,
    @Json(name = "route") val route: List<List<SwapSequenceDto>>? = null,
    @Json(name = "routeID") val routeID: String,
    @Json(name = "checksum") val checksum: String,
    @Json(name = "timestamp") val timestamp: Long
)

/**
 * Individual swap step in a multi-hop route.
 */
@JsonClass(generateAdapter = true)
data class SwapSequenceDto(
    @Json(name = "pool") val pool: String,
    @Json(name = "tokenIn") val tokenIn: String,
    @Json(name = "tokenOut") val tokenOut: String,
    @Json(name = "swapAmount") val swapAmount: String,
    @Json(name = "amountOut") val amountOut: String,
    @Json(name = "exchange") val exchange: String,
    @Json(name = "poolType") val poolType: String
)

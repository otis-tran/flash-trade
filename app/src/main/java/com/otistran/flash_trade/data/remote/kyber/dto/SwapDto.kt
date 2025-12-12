package com.otistran.flash_trade.data.remote.kyber.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SwapRouteRequest(
    @Json(name = "tokenIn") val tokenIn: String,
    @Json(name = "tokenOut") val tokenOut: String,
    @Json(name = "amountIn") val amountIn: String,
    @Json(name = "saveGas") val saveGas: Boolean = false,
    @Json(name = "gasInclude") val gasInclude: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SwapRouteResponse(
    @Json(name = "routeSummary") val routeSummary: RouteSummary?,
    @Json(name = "routerAddress") val routerAddress: String?
)

@JsonClass(generateAdapter = true)
data class RouteSummary(
    @Json(name = "tokenIn") val tokenIn: String,
    @Json(name = "tokenOut") val tokenOut: String,
    @Json(name = "amountIn") val amountIn: String,
    @Json(name = "amountOut") val amountOut: String,
    @Json(name = "amountOutUsd") val amountOutUsd: String?,
    @Json(name = "gas") val gas: String?,
    @Json(name = "gasUsd") val gasUsd: String?
)

@JsonClass(generateAdapter = true)
data class SwapBuildRequest(
    @Json(name = "routeSummary") val routeSummary: RouteSummary,
    @Json(name = "sender") val sender: String,
    @Json(name = "recipient") val recipient: String,
    @Json(name = "slippageTolerance") val slippageTolerance: Int = 50 // 0.5%
)

@JsonClass(generateAdapter = true)
data class SwapBuildResponse(
    @Json(name = "data") val data: String?,
    @Json(name = "routerAddress") val routerAddress: String?
)

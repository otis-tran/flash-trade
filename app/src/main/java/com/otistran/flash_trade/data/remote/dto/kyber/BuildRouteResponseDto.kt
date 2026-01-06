package com.otistran.flash_trade.data.remote.dto.kyber

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BuildRouteResponseDto(
    val code: Int?,
    val message: String?,
    val data: BuildRouteData?,
    @Json(name = "requestId")
    val requestId: String?
)

@JsonClass(generateAdapter = true)
data class BuildRouteData(
    @Json(name = "amountIn")
    val amountIn: String?,
    @Json(name = "amountInUsd")
    val amountInUsd: String?,
    @Json(name = "amountOut")
    val amountOut: String?,
    @Json(name = "amountOutUsd")
    val amountOutUsd: String?,
    val gas: String?,
    @Json(name = "gasUsd")
    val gasUsd: String?,
    @Json(name = "additionalCostUsd")
    val additionalCostUsd: String?,
    @Json(name = "additionalCostMessage")
    val additionalCostMessage: String?,
    val data: String?,
    @Json(name = "routerAddress")
    val routerAddress: String?,
    @Json(name = "transactionValue")
    val transactionValue: String
)
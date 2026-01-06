package com.otistran.flash_trade.data.remote.dto.kyber

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BuildRouteRequestDto(
    @Json(name = "routeSummary")
    val routeSummary: RouteSummaryRequest,
    @Json(name = "sender")
    val sender: String,
    @Json(name = "recipient")
    val recipient: String,
    @Json(name = "permit")
    val permit: String? = null,
    @Json(name = "deadline")
    val deadline: Long? = null
)



@JsonClass(generateAdapter = true)
data class RouteSummaryRequest(
    @Json(name = "tokenIn")
    val tokenIn: String?,
    @Json(name = "amountIn")
    val amountIn: String?,
    @Json(name = "amountInUsd")
    val amountInUsd: String?,
    @Json(name = "tokenOut")
    val tokenOut: String?,
    @Json(name = "amountOut")
    val amountOut: String?,
    @Json(name = "amountOutUsd")
    val amountOutUsd: String?,
    val gas: String?,
    @Json(name = "gasPrice")
    val gasPrice: String?,
    @Json(name = "gasUsd")
    val gasUsd: String?,
    @Json(name = "extraFee")
    val extraFee: ExtraFeeRequest?,
    val route: List<List<RouteStepRequest>>?,
    @Json(name = "routeID")
    val routeId: String?,
    val checksum: String?,
    val timestamp: Long?
)

@JsonClass(generateAdapter = true)
data class ExtraFeeRequest(
    @Json(name = "feeAmount")
    val feeAmount: String?,
    @Json(name = "chargeFeeBy")
    val chargeFeeBy: String?,
    @Json(name = "isInBps")
    val isInBps: Boolean?,
    @Json(name = "feeReceiver")
    val feeReceiver: String?
)

@JsonClass(generateAdapter = true)
data class RouteStepRequest(
    val pool: String?,
    @Json(name = "tokenIn")
    val tokenIn: String?,
    @Json(name = "tokenOut")
    val tokenOut: String?,
    @Json(name = "swapAmount")
    val swapAmount: String?,
    @Json(name = "amountOut")
    val amountOut: String?,
    val exchange: String?,
    @Json(name = "poolType")
    val poolType: String?,
    @Json(name = "poolExtra")
    val poolExtra: PoolExtraRequest?,
    val extra: StepExtraRequest?
)

@JsonClass(generateAdapter = true)
data class PoolExtraRequest(
    val type: String?,
    @Json(name = "dodoV1SellHelper")
    val dodoV1SellHelper: String?,
    @Json(name = "baseToken")
    val baseToken: String?,
    @Json(name = "quoteToken")
    val quoteToken: String?
)

@JsonClass(generateAdapter = true)
data class StepExtraRequest(
    @Json(name = "amountIn")
    val amountIn: String?,
    @Json(name = "filledOrders")
    val filledOrders: List<FilledOrderRequest>?,
    @Json(name = "swapSide")
    val swapSide: String?
)

@JsonClass(generateAdapter = true)
data class FilledOrderRequest(
    @Json(name = "allowedSenders")
    val allowedSenders: String?,
    @Json(name = "feeAmount")
    val feeAmount: String?,
    @Json(name = "feeRecipient")
    val feeRecipient: String?,
    @Json(name = "filledMakingAmount")
    val filledMakingAmount: String?,
    @Json(name = "filledTakingAmount")
    val filledTakingAmount: String?,
    @Json(name = "getMakerAmount")
    val getMakerAmount: String?,
    @Json(name = "getTakerAmount")
    val getTakerAmount: String?,
    val interaction: String?,
    @Json(name = "isFallback")
    val isFallback: Boolean?,
    val maker: String?,
    @Json(name = "makerAsset")
    val makerAsset: String?,
    @Json(name = "makerAssetData")
    val makerAssetData: String?,
    @Json(name = "makerTokenFeePercent")
    val makerTokenFeePercent: Int?,
    @Json(name = "makingAmount")
    val makingAmount: String?,
    @Json(name = "orderId")
    val orderId: Int?,
    val permit: String?,
    val predicate: String?,
    val receiver: String?,
    val salt: String?,
    val signature: String?,
    @Json(name = "takerAsset")
    val takerAsset: String?,
    @Json(name = "takerAssetData")
    val takerAssetData: String?,
    @Json(name = "takingAmount")
    val takingAmount: String?
)

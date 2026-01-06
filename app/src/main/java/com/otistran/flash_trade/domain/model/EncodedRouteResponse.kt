package com.otistran.flash_trade.domain.model

data class EncodedRouteResponse(
    val code: Int?,
    val message: String?,
    val data: BuildRouteData?,
    val requestId: String?
)

data class BuildRouteData(
    val amountIn: String?,
    val amountInUsd: String?,
    val amountOut: String?,
    val amountOutUsd: String?,
    val gas: String?,
    val gasUsd: String?,
    val additionalCostUsd: String?,
    val additionalCostMessage: String?,
    val data: String?,
    val routerAddress: String?,
    val transactionValue: String
)
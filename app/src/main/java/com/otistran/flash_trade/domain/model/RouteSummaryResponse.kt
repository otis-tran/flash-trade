package com.otistran.flash_trade.domain.model

data class RouteSummaryResponse(
    val tokenIn: String?,
    val amountIn: String?,
    val amountInUsd: String?,
    val tokenOut: String?,
    val amountOut: String?,
    val amountOutUsd: String?,
    val gas: String?,
    val gasPrice: String?,
    val gasUsd: String?,
    val extraFee: ExtraFee?,
    val route: List<List<Route>>?,
    val routeId: String?,
    val checksum: String?,
    val timestamp: Long?,
    val routerAddress: String?
)

data class ExtraFee(
    val feeAmount: String?,
    val chargeFeeBy: String?,
    val isInBps: Boolean?,
    val feeReceiver: String?
)

data class Route(
    val pool: String?,
    val tokenIn: String?,
    val tokenOut: String?,
    val swapAmount: String?,
    val amountOut: String?,
    val exchange: String?,
    val poolType: String?,
    val poolExtra: PoolExtra?,
    val extra: Extra?
)

data class PoolExtra(
    val type: String?,
    val dodoV1SellHelper: String?,
    val baseToken: String?,
    val quoteToken: String?
)

data class Extra(
    val amountIn: String?,
    val filledOrders: List<FilledOrder>?,
    val swapSide: String?
)

data class FilledOrder(
    val allowedSenders: String?,
    val feeAmount: String?,
    val feeRecipient: String?,
    val filledMakingAmount: String?,
    val filledTakingAmount: String?,
    val getMakerAmount: String?,
    val getTakerAmount: String?,
    val interaction: String?,
    val isFallback: Boolean?,
    val maker: String?,
    val makerAsset: String?,
    val makerAssetData: String?,
    val makerTokenFeePercent: Int?,
    val makingAmount: String?,
    val orderId: Int?,
    val permit: String?,
    val predicate: String?,
    val receiver: String?,
    val salt: String?,
    val signature: String?,
    val takerAsset: String?,
    val takerAssetData: String?,
    val takingAmount: String?
)
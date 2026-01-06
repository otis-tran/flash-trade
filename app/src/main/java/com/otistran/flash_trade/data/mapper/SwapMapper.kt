package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.data.remote.dto.kyber.BuildRouteRequestDto
import com.otistran.flash_trade.data.remote.dto.kyber.DataWrapper
import com.otistran.flash_trade.data.remote.dto.kyber.ExtraFeeRequest
import com.otistran.flash_trade.data.remote.dto.kyber.FilledOrderRequest
import com.otistran.flash_trade.data.remote.dto.kyber.PoolExtraRequest
import com.otistran.flash_trade.data.remote.dto.kyber.RouteStepRequest
import com.otistran.flash_trade.data.remote.dto.kyber.RouteSummaryRequest
import com.otistran.flash_trade.data.remote.dto.kyber.StepExtraRequest
import com.otistran.flash_trade.domain.model.Extra
import com.otistran.flash_trade.domain.model.ExtraFee
import com.otistran.flash_trade.domain.model.FilledOrder
import com.otistran.flash_trade.domain.model.PoolExtra
import com.otistran.flash_trade.domain.model.Route
import com.otistran.flash_trade.domain.model.RouteSummaryResponse

fun DataWrapper.toRouteSummary(): RouteSummaryResponse {
    return RouteSummaryResponse(
        tokenIn = routeSummary?.tokenIn,
        amountIn = routeSummary?.amountIn,
        amountInUsd = routeSummary?.amountInUsd,
        tokenOut = routeSummary?.tokenOut,
        amountOut = routeSummary?.amountOut,
        amountOutUsd = routeSummary?.amountOutUsd,
        gas = routeSummary?.gas,
        gasPrice = routeSummary?.gasPrice,
        gasUsd = routeSummary?.gasUsd,
        extraFee = routeSummary?.extraFee?.let {
            ExtraFee(
                feeAmount = it.feeAmount,
                chargeFeeBy = it.chargeFeeBy,
                isInBps = it.isInBps,
                feeReceiver = it.feeReceiver,
            )
        },
        route = routeSummary?.route?.map { routes ->
            routes.map { routeInfo ->
                Route(
                    pool = routeInfo.pool,
                    tokenIn = routeInfo.tokenIn,
                    tokenOut = routeInfo.tokenOut,
                    swapAmount = routeInfo.swapAmount,
                    amountOut = routeInfo.amountOut,
                    exchange = routeInfo.exchange,
                    poolType = routeInfo.poolType,
                    poolExtra = routeInfo.poolExtra?.let {
                        PoolExtra(
                            type = it.type,
                            dodoV1SellHelper = it.dodoV1SellHelper,
                            baseToken = it.baseToken,
                            quoteToken = it.quoteToken,
                        )
                    },
                    extra = routeInfo.extra?.let { extra ->
                        Extra(
                            amountIn = extra.amountIn,
                            filledOrders = extra.filledOrders?.map {
                                FilledOrder(
                                    allowedSenders = it.allowedSenders,
                                    feeAmount = it.feeAmount,
                                    feeRecipient = it.feeRecipient,
                                    filledMakingAmount = it.filledMakingAmount,
                                    filledTakingAmount = it.filledTakingAmount,
                                    getMakerAmount = it.getMakerAmount,
                                    getTakerAmount = it.getTakerAmount,
                                    interaction = it.interaction,
                                    isFallback = it.isFallback,
                                    maker = it.maker,
                                    makerAsset = it.makerAsset,
                                    makerAssetData = it.makerAssetData,
                                    makerTokenFeePercent = it.makerTokenFeePercent,
                                    makingAmount = it.makingAmount,
                                    orderId = it.orderId,
                                    permit = it.permit,
                                    predicate = it.predicate,
                                    receiver = it.receiver,
                                    salt = it.salt,
                                    signature = it.signature,
                                    takerAsset = it.takerAsset,
                                    takerAssetData = it.takerAssetData,
                                    takingAmount = it.takingAmount,
                                )
                            },
                            swapSide = extra.swapSide,
                        )
                    },
                )
            }
        },
        routeId = routeSummary?.routeId,
        checksum = routeSummary?.checksum,
        timestamp = routeSummary?.timestamp,
        routerAddress = routerAddress,
    )
}


fun RouteSummaryResponse.toRequest(
    sender: String,
    receipt: String,
    permit: String? = null,
    deadline: Long? = null
): BuildRouteRequestDto {
    return BuildRouteRequestDto(
        routeSummary = RouteSummaryRequest(
            tokenIn = this.tokenIn,
            amountIn = this.amountIn,
            amountInUsd = this.amountInUsd,
            tokenOut = this.tokenOut,
            amountOut = this.amountOut,
            amountOutUsd = this.amountOutUsd,
            gas = this.gas,
            gasPrice = this.gasPrice,
            gasUsd = this.gasUsd,
            extraFee = this.extraFee?.let {
                ExtraFeeRequest(
                    feeAmount = it.feeAmount,
                    chargeFeeBy = it.chargeFeeBy,
                    isInBps = it.isInBps,
                    feeReceiver = it.feeReceiver
                )
            },
            route = this.route?.map { routes ->
                routes.map { routeInfo ->
                    RouteStepRequest(
                        pool = routeInfo.pool,
                        tokenIn = routeInfo.tokenIn,
                        tokenOut = routeInfo.tokenOut,
                        swapAmount = routeInfo.swapAmount,
                        amountOut = routeInfo.amountOut,
                        exchange = routeInfo.exchange,
                        poolType = routeInfo.poolType,
                        poolExtra = routeInfo.poolExtra?.let {
                            PoolExtraRequest(
                                type = it.type,
                                dodoV1SellHelper = it.dodoV1SellHelper,
                                baseToken = it.baseToken,
                                quoteToken = it.quoteToken
                            )
                        },
                        extra = routeInfo.extra?.let { extra ->
                            StepExtraRequest(
                                amountIn = extra.amountIn,
                                filledOrders = extra.filledOrders?.map {
                                    FilledOrderRequest(
                                        allowedSenders = it.allowedSenders,
                                        feeAmount = it.feeAmount,
                                        feeRecipient = it.feeRecipient,
                                        filledMakingAmount = it.filledMakingAmount,
                                        filledTakingAmount = it.filledTakingAmount,
                                        getMakerAmount = it.getMakerAmount,
                                        getTakerAmount = it.getTakerAmount,
                                        interaction = it.interaction,
                                        isFallback = it.isFallback,
                                        maker = it.maker,
                                        makerAsset = it.makerAsset,
                                        makerAssetData = it.makerAssetData,
                                        makerTokenFeePercent = it.makerTokenFeePercent,
                                        makingAmount = it.makingAmount,
                                        orderId = it.orderId,
                                        permit = it.permit,
                                        predicate = it.predicate,
                                        receiver = it.receiver,
                                        salt = it.salt,
                                        signature = it.signature,
                                        takerAsset = it.takerAsset,
                                        takerAssetData = it.takerAssetData,
                                        takingAmount = it.takingAmount
                                    )
                                },
                                swapSide = extra.swapSide
                            )
                        }
                    )
                }
            },
            routeId = this.routeId,
            checksum = this.checksum,
            timestamp = this.timestamp
        ),
        sender = sender,
        recipient = receipt,
        permit = permit,
        deadline = deadline
    )
}


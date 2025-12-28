package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.data.remote.dto.kyber.BuildRouteResponseDto
import com.otistran.flash_trade.data.remote.dto.kyber.RouteResponseDto
import com.otistran.flash_trade.data.remote.dto.kyber.RouteSummaryDto
import com.otistran.flash_trade.domain.model.EncodedSwap
import com.otistran.flash_trade.domain.model.Quote
import java.math.BigInteger
import javax.inject.Inject

/**
 * Mapper for swap DTOs to domain models.
 */
class SwapMapper @Inject constructor() {

    /**
     * Map RouteResponseDto to Quote domain model.
     * @throws IllegalStateException if data is null
     */
    fun toQuote(dto: RouteResponseDto): Quote {
        val data = dto.data ?: throw IllegalStateException("Route data is null")
        val summary = data.routeSummary
        return Quote(
            tokenIn = summary.tokenIn,
            tokenOut = summary.tokenOut,
            amountIn = summary.amountIn.toBigIntegerSafe(),
            amountOut = summary.amountOut.toBigIntegerSafe(),
            amountOutUsd = summary.amountOutUsd,
            gas = summary.gas.toBigIntegerSafe(),
            gasUsd = summary.gasUsd,
            routerAddress = data.routerAddress,
            routeId = summary.routeID,
            timestamp = summary.timestamp
        )
    }

    /**
     * Map BuildRouteResponseDto to EncodedSwap domain model.
     * @throws IllegalStateException if data is null
     */
    fun toEncodedSwap(dto: BuildRouteResponseDto): EncodedSwap {
        val data = dto.data ?: throw IllegalStateException("Build data is null")
        return EncodedSwap(
            calldata = data.data,
            routerAddress = data.routerAddress,
            value = data.transactionValue.toBigIntegerHexSafe(),
            gas = data.gas.toBigIntegerSafe(),
            amountOut = data.amountOut.toBigIntegerSafe()
        )
    }

    /**
     * Convert Quote back to RouteSummaryDto for build request.
     */
    fun toRouteSummaryDto(quote: Quote, originalChecksum: String): RouteSummaryDto {
        return RouteSummaryDto(
            tokenIn = quote.tokenIn,
            amountIn = quote.amountIn.toString(),
            amountInUsd = "0",
            tokenOut = quote.tokenOut,
            amountOut = quote.amountOut.toString(),
            amountOutUsd = quote.amountOutUsd,
            gas = quote.gas.toString(),
            gasPrice = "0",
            gasUsd = quote.gasUsd,
            routeID = quote.routeId,
            checksum = originalChecksum,
            timestamp = quote.timestamp
        )
    }

    private fun String.toBigIntegerSafe(): BigInteger {
        return try {
            BigInteger(this)
        } catch (e: Exception) {
            BigInteger.ZERO
        }
    }

    private fun String.toBigIntegerHexSafe(): BigInteger {
        return try {
            if (startsWith("0x") || startsWith("0X")) {
                BigInteger(substring(2), 16)
            } else {
                BigInteger(this)
            }
        } catch (e: Exception) {
            BigInteger.ZERO
        }
    }
}

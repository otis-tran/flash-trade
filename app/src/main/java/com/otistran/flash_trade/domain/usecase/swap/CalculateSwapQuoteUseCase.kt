package com.otistran.flash_trade.domain.usecase.swap

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Calculates swap quote from token prices.
 * Pure calculation - no network calls.
 */
class CalculateSwapQuoteUseCase @Inject constructor() {

    data class SwapQuote(
        val buyAmount: BigDecimal,
        val exchangeRate: Double,
        val sellAmountUsd: BigDecimal,
        val buyAmountUsd: BigDecimal
    )

    operator fun invoke(
        sellAmount: BigDecimal,
        sellPrice: Double,
        buyPrice: Double,
        buyDecimals: Int
    ): SwapQuote? {
        if (sellAmount <= BigDecimal.ZERO) return null
        if (sellPrice <= 0 || buyPrice <= 0) return null

        val rate = sellPrice / buyPrice
        val buyAmount = sellAmount.multiply(BigDecimal(rate))
            .setScale(buyDecimals.coerceAtMost(6), RoundingMode.DOWN)

        val sellUsd = sellAmount.multiply(BigDecimal(sellPrice))
            .setScale(2, RoundingMode.HALF_UP)
        val buyUsd = buyAmount.multiply(BigDecimal(buyPrice))
            .setScale(2, RoundingMode.HALF_UP)

        return SwapQuote(
            buyAmount = buyAmount,
            exchangeRate = rate,
            sellAmountUsd = sellUsd,
            buyAmountUsd = buyUsd
        )
    }
}

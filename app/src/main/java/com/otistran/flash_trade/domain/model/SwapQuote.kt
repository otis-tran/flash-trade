package com.otistran.flash_trade.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Quote data from Kyber API for a swap.
 */
data class SwapQuote(
    val amountIn: BigDecimal,
    val amountInUsd: BigDecimal,
    val amountOut: BigDecimal,
    val amountOutUsd: BigDecimal,
    val exchangeRate: BigDecimal,       // amountOut / amountIn
    val networkFeeUsd: BigDecimal,      // gasUsd from Kyber
    val networkFeeNative: BigDecimal,   // gas in native token
    val priceImpactPercent: BigDecimal  // Calculated from USD values
) {
    /**
     * Format exchange rate with appropriate decimal places based on value:
     * - Large values (>1): 2-4 decimals
     * - Small values (<1): up to 6 decimals
     * - Very small values (<0.0001): scientific notation avoided, show up to 8
     */
    val formattedExchangeRate: String
        get() {
            val scale = when {
                exchangeRate >= BigDecimal("1000") -> 2
                exchangeRate >= BigDecimal.ONE -> 4
                exchangeRate >= BigDecimal("0.0001") -> 6
                else -> 8
            }
            return exchangeRate.setScale(scale, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }

    /**
     * Format amount out with appropriate decimal places:
     * - Large values (>1000): 2 decimals
     * - Medium values (>1): 4 decimals
     * - Small values (<1): 6 decimals
     */
    val formattedAmountOut: String
        get() {
            val scale = when {
                amountOut >= BigDecimal("1000") -> 2
                amountOut >= BigDecimal.ONE -> 4
                amountOut >= BigDecimal("0.0001") -> 6
                else -> 8
            }
            return amountOut.setScale(scale, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }

    val formattedNetworkFee: String
        get() = "~$${networkFeeUsd.setScale(2, RoundingMode.HALF_UP)}"

    val formattedPriceImpact: String
        get() = "${priceImpactPercent.setScale(2, RoundingMode.HALF_UP)}%"

    val isPriceImpactHigh: Boolean
        get() = priceImpactPercent.abs() > BigDecimal("5.0")

    val isPriceImpactMedium: Boolean
        get() = priceImpactPercent.abs() > BigDecimal("1.0") && !isPriceImpactHigh
}

enum class PriceImpactLevel { LOW, MEDIUM, HIGH }


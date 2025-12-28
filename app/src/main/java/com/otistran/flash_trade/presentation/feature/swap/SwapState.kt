package com.otistran.flash_trade.presentation.feature.swap

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.model.Token
import java.math.BigDecimal
import java.math.RoundingMode

@Stable
data class SwapState(
    val tokenFrom: Token? = null,
    val tokenTo: Token? = null,
    val amount: String = "",
    val quote: Quote? = null,
    val isLoadingQuote: Boolean = false,
    val isExecuting: Boolean = false,
    val error: String? = null,
    val txHash: String? = null,
    val quoteExpired: Boolean = false,
    val userBalance: String = "0.0",

    // Slippage settings
    val slippageTolerance: Double = 0.5, // Default 0.5%
    val showSlippageSettings: Boolean = false,

    // Price impact
    val priceImpact: Double? = null
) : UiState {

    val isValid: Boolean
        get() = tokenFrom != null &&
                tokenTo != null &&
                amount.isNotEmpty() &&
                amount.toDoubleOrNull() != null &&
                amount.toDoubleOrNull()!! > 0 &&
                quote != null &&
                !quoteExpired

    val canSwap: Boolean
        get() = isValid && !isExecuting && !isLoadingQuote

    val displayRate: String
        get() {
            if (quote == null || tokenFrom == null || tokenTo == null) return "-"
            val amountIn = quote.amountIn.toBigDecimal()
            val amountOut = quote.amountOut.toBigDecimal()
            if (amountIn == BigDecimal.ZERO) return "-"
            val rate = amountOut.divide(amountIn, 8, RoundingMode.HALF_UP)
            return "1 ${tokenFrom.symbol} ≈ ${rate.stripTrailingZeros().toPlainString()} ${tokenTo.symbol}"
        }

    val estimatedOutput: String
        get() {
            if (quote == null || tokenTo == null) return "-"
            val amountOut = quote.amountOut.toBigDecimal()
            val divisor = BigDecimal.TEN.pow(tokenTo.decimals)
            val formatted = amountOut.divide(divisor, 6, RoundingMode.HALF_DOWN)
            return formatted.stripTrailingZeros().toPlainString()
        }

    val formattedSlippage: String
        get() = "${slippageTolerance}%"

    val formattedPriceImpact: String
        get() = priceImpact?.let { "${String.format("%.2f", it)}%" } ?: "-"

    val isPriceImpactHigh: Boolean
        get() = (priceImpact ?: 0.0) > 3.0

    val isPriceImpactVeryHigh: Boolean
        get() = (priceImpact ?: 0.0) > 10.0

    val minimumReceived: String
        get() {
            if (quote == null || tokenTo == null) return "-"
            val amountOut = quote.amountOut.toBigDecimal()
            val divisor = BigDecimal.TEN.pow(tokenTo.decimals)
            val formatted = amountOut.divide(divisor, 6, RoundingMode.HALF_DOWN)
            val withSlippage = formatted.multiply(BigDecimal.ONE.minus(BigDecimal(slippageTolerance / 100)))
            return "${withSlippage.setScale(6, RoundingMode.HALF_DOWN).stripTrailingZeros().toPlainString()} ${tokenTo.symbol}"
        }
}

/**
 * Preset slippage options.
 */
enum class SlippageOption(val value: Double, val label: String) {
    LOW(0.1, "0.1%"),
    MEDIUM(0.5, "0.5%"),
    HIGH(1.0, "1.0%")
}

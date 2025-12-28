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
    val userBalance: String = "0.0"
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
}

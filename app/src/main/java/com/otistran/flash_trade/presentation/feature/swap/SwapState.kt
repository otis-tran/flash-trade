package com.otistran.flash_trade.presentation.feature.swap

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.core.util.TokenConstants
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.PriceImpactLevel
import com.otistran.flash_trade.domain.model.SwapQuote
import com.otistran.flash_trade.domain.model.Token
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * State for Swap screen.
 *
 * Design doc: Section 2 - Swap Screen
 * - Two sections: "Sell Token" (top) and "Buy Token" (bottom)
 * - Swap trigger button between sections
 * - Quote details box when amount entered
 */
@Stable
data class SwapState(
    // Network
    val network: NetworkMode = NetworkMode.DEFAULT,

    // Tokens
    val sellToken: SwapToken? = null,
    val buyToken: SwapToken? = null,

    // Amounts
    val sellAmount: String = "",
    val buyAmount: String = "",
    val sellAmountUsd: String = "",
    val buyAmountUsd: String = "",

    // Token prices (for display)
    val sellTokenPrice: Double = 0.0,
    val buyTokenPrice: Double = 0.0,

    // Quote from Kyber API
    val quote: SwapQuote? = null,
    val quoteTimestamp: Long = 0L,
    val quoteExpiresInSeconds: Int = 0,
    val isQuoteStale: Boolean = false,

    // Slippage
    val slippage: Double = 5.0, // Default 5% for volatile meme tokens
    val showSlippageDialog: Boolean = false,

    // UI State
    val isLoadingQuote: Boolean = false,
    val isPricesLoading: Boolean = false,
    val isBalancesLoading: Boolean = false,
    val isExecuting: Boolean = false,
    val isApproving: Boolean = false,
    val showTokenSelector: Boolean = false,
    val isSelectingSellToken: Boolean = true, // true = selecting sell token, false = selecting buy token
    val tokenSearchQuery: String = "",

    // Token filter state
    val showSafeTokensOnly: Boolean = false,  // Default shows all tokens

    // Error
    val error: String? = null
) : UiState {

    // Computed properties from quote
    val displayExchangeRate: String
        get() = quote?.let { q ->
            "1 ${sellToken?.symbol} = ${q.formattedExchangeRate} ${buyToken?.symbol}"
        } ?: ""

    val displayNetworkFee: String
        get() = quote?.formattedNetworkFee ?: ""

    val displayPriceImpact: String
        get() = quote?.formattedPriceImpact ?: ""

    val priceImpactColor: PriceImpactLevel
        get() = when {
            quote?.isPriceImpactHigh == true -> PriceImpactLevel.HIGH
            quote?.isPriceImpactMedium == true -> PriceImpactLevel.MEDIUM
            else -> PriceImpactLevel.LOW
        }

    val showQuoteCountdown: Boolean
        get() = quote != null && quoteExpiresInSeconds > 0

    val hasValidSellAmount: Boolean
        get() = sellAmount.isNotBlank() && sellAmount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true

    val hasValidQuote: Boolean
        get() = hasValidSellAmount && quote != null && buyAmount.isNotBlank()

    val canSwap: Boolean
        get() = !isExecuting && !isApproving &&
                sellToken != null &&
                buyToken != null &&
                hasValidQuote &&
                !insufficientBalance

    /**
     * Maximum amount that can be swapped.
     * For native tokens (ETH), limit to 80% to reserve gas.
     */
    val maxSwapAmount: BigDecimal
        get() {
            val balance = sellToken?.balance ?: BigDecimal.ZERO
            return if (TokenConstants.isNativeToken(sellToken?.address ?: "")) {
                balance.multiply(BigDecimal.valueOf(TokenConstants.NATIVE_TOKEN_MAX_SWAP_PERCENTAGE))
            } else {
                balance
            }
        }

    val insufficientBalance: Boolean
        get() {
            val sellAmt = sellAmount.toBigDecimalOrNull() ?: return false
            return sellAmt > maxSwapAmount
        }

    val ctaButtonText: String
        get() = when {
            isApproving -> "Approving..."
            isExecuting -> "Swapping..."
            sellToken == null -> "Select token to sell"
            buyToken == null -> "Select token to buy"
            !hasValidSellAmount -> "Enter amount"
            insufficientBalance && TokenConstants.isNativeToken(sellToken.address) -> "Max 80% for gas"
            insufficientBalance -> "Insufficient balance"
            else -> "Swap"
        }

    val ctaButtonEnabled: Boolean
        get() = canSwap
}

/**
 * Token representation for swap UI.
 */
@Stable
@Immutable
data class SwapToken(
    val address: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val logoUrl: String?,
    val balance: BigDecimal = BigDecimal.ZERO,
    val priceUsd: Double = 0.0
) {
    val formattedBalance: String
        get() = if (balance < BigDecimal("0.0001")) "<0.0001" else balance.setScale(4, java.math.RoundingMode.DOWN).toPlainString()
}

/**
 * Convert domain Token to SwapToken.
 */
fun Token.toSwapToken(balance: BigDecimal = BigDecimal.ZERO, priceUsd: Double = 0.0): SwapToken {
    return SwapToken(
        address = address,
        symbol = symbol ?: "???",
        name = name ?: symbol ?: "Unknown",
        decimals = decimals,
        logoUrl = logoUrl,
        balance = balance,
        priceUsd = priceUsd
    )
}

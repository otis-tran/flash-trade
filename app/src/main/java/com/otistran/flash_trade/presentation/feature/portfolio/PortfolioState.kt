package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.NetworkMode
import java.math.BigDecimal
import java.math.RoundingMode

@Stable
data class PortfolioState(
    val isLoadingTokens: Boolean = true,
    val isRefreshing: Boolean = false,
    val userName: String = "",
    val userEmail: String? = null,
    val walletAddress: String? = null,

    // Balance - computed from tokens
    val totalBalanceUsd: BigDecimal?= null,

    // Tokens
    val tokens: List<TokenHolding> = emptyList(),

    // Network - observe from Settings (read-only)
    val currentNetwork: NetworkMode = NetworkMode.ETHEREUM,

    // Error
    val error: String? = null,

    // UI State
    val showQRSheet: Boolean = false
) : UiState {

    val shortWalletAddress: String?
        get() = walletAddress?.let {
            if (it.length > 10) "${it.take(6)}...${it.takeLast(4)}" else it
        }

    val displayAddress: String?
        get() = walletAddress

    val displayShortAddress: String?
        get() = shortWalletAddress

    val formattedTotalBalance: String
        get() = "$${String.format("%,.2f", totalBalanceUsd)}"

    val hasWallet: Boolean
        get() = !walletAddress.isNullOrBlank()

    val canRefresh: Boolean
        get() = !isLoadingTokens && !isRefreshing
}

/**
 * Token holding with price data.
 */
@Stable
data class TokenHolding(
    val id: Int,
    val symbol: String?,
    val name: String?,
    val balance: Double,
    val priceUsd: BigDecimal? = null,
    val iconUrl: String? = null,
    val address: String? = null, // null is ETH native token
    val autoSellTime: Long? = null // Unix millis when auto-sell triggers
) {
    // Value = balance × price
    val valueUsd: BigDecimal?
        get() = priceUsd?.multiply(BigDecimal.valueOf(balance))

    val formattedBalance: String
        get() = when {
            balance <= 0 -> "0"
            balance < 0.0001 -> "<0.0001"
            balance < 1 -> "%.6f".format(balance).trimEnd('0').trimEnd('.')
            else -> "%,.4f".format(balance).trimEnd('0').trimEnd('.')
        }

    val formattedPrice: String
        get() = priceUsd?.let {
            "$%,.2f".format(it.setScale(2, RoundingMode.HALF_UP))
        } ?: "—"

    val formattedValue: String
        get() = valueUsd?.let {
            "$%,.2f".format(it.setScale(2, RoundingMode.HALF_UP))
        } ?: "—"

    val hasPrice: Boolean
        get() = priceUsd != null

    val hasAutoSell: Boolean
        get() = autoSellTime != null && autoSellTime > System.currentTimeMillis()

    val autoSellRemainingMs: Long
        get() = (autoSellTime ?: 0) - System.currentTimeMillis()
}

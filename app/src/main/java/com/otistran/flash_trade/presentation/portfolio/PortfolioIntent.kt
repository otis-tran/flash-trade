package com.otistran.flash_trade.presentation.portfolio

import androidx.compose.runtime.Immutable

/**
 * User intents for portfolio screen.
 */
@Immutable
sealed interface PortfolioIntent {
    data object LoadPortfolio : PortfolioIntent
    data object RefreshPortfolio : PortfolioIntent
    data object CopyWalletAddress : PortfolioIntent
    data class SelectNetwork(val network: Network) : PortfolioIntent
}

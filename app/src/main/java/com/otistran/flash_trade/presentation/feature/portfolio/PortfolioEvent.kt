package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.core.base.UiEvent

/**
 * User events for portfolio screen.
 */
@Immutable
sealed class PortfolioEvent : UiEvent {
    data object LoadPortfolio : PortfolioEvent()
    data object RefreshPortfolio : PortfolioEvent()
    data object CopyWalletAddress : PortfolioEvent()
    data class SelectNetwork(val network: Network) : PortfolioEvent()
    data object DismissError : PortfolioEvent()
}

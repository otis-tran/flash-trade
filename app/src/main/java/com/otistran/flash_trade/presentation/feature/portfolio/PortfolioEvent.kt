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
    data class SelectTimeframe(val timeframe: Timeframe) : PortfolioEvent()
    data class OpenTransactionDetails(val txHash: String) : PortfolioEvent()
    data object LoadMoreTransactions : PortfolioEvent()
    data object DismissError : PortfolioEvent()
}
package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.core.base.UiEvent

/**
 * User events for portfolio screen.
 */
@Immutable
sealed class PortfolioEvent : UiEvent {
    data object LoadPortfolio : PortfolioEvent()
    data object CopyWalletAddress : PortfolioEvent()
    data object DismissError : PortfolioEvent()

    // Quick Actions
    data object OnSwapClick : PortfolioEvent()
    data object OnSendClick : PortfolioEvent()
    data object OnReceiveClick : PortfolioEvent()

    // QR Sheet
    data object ShowQRSheet : PortfolioEvent()
    data object HideQRSheet : PortfolioEvent()
}

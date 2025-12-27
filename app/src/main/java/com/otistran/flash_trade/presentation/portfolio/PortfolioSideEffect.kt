package com.otistran.flash_trade.presentation.portfolio

import androidx.compose.runtime.Immutable

/**
 * Side effects for portfolio screen.
 */
@Immutable
sealed interface PortfolioSideEffect {
    data class ShowToast(val message: String) : PortfolioSideEffect
    data class CopyToClipboard(val text: String) : PortfolioSideEffect
}
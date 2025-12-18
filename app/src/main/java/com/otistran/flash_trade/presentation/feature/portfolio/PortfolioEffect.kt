package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.core.base.UiEffect

/**
 * One-time effects for portfolio screen.
 */
@Immutable
sealed class PortfolioEffect : UiEffect {
    data class ShowToast(val message: String) : PortfolioEffect()
    data class CopyToClipboard(val text: String) : PortfolioEffect()
}
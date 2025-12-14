package com.otistran.flash_trade.presentation.portfolio

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.presentation.base.MviSideEffect

/**
 * Side effects for portfolio screen.
 */
@Immutable
sealed class PortfolioSideEffect : MviSideEffect {
    /** Show toast message. */
    data class ShowToast(val message: String) : PortfolioSideEffect()
}

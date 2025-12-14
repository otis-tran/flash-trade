package com.otistran.flash_trade.presentation.portfolio

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.presentation.base.MviIntent

/**
 * User intents for portfolio screen.
 */
@Immutable
sealed class PortfolioIntent : MviIntent {
    /** Refresh user data. */
    data object Refresh : PortfolioIntent()

    /** Dismiss error message. */
    data object DismissError : PortfolioIntent()
}

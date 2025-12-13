package com.otistran.flash_trade.presentation.auth

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.presentation.base.MviSideEffect

/**
 * One-time side effects for login screen.
 */
@Immutable
sealed class LoginSideEffect : MviSideEffect {
    /** Navigate to Trading screen after successful auth. */
    data object NavigateToTrading : LoginSideEffect()

    /** Navigate back to Welcome screen. */
    data object NavigateBack : LoginSideEffect()

    /** Show error toast (for non-critical errors). */
    data class ShowToast(val message: String) : LoginSideEffect()
}

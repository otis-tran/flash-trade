package com.otistran.flash_trade.presentation.feature.auth
import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.core.base.UiEffect

/**
 * One-time side effects for login screen.
 */
@Immutable
sealed class LoginEffect : UiEffect {
    /** Navigate to Trading screen after successful auth. */
    data object NavigateToTrading : LoginEffect()

    /** Navigate back to Welcome screen. */
    data object NavigateBack : LoginEffect()

    /** Show error toast. */
    data class ShowToast(val message: String) : LoginEffect()
}
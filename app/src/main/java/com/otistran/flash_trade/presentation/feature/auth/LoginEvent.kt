package com.otistran.flash_trade.presentation.feature.auth

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.core.base.UiEvent

/**
 * User events for login screen.
 */
@Immutable
sealed class LoginEvent : UiEvent {
    /** User tapped "Continue with Google". */
    data object GoogleLogin : LoginEvent()

    /** User tapped retry after error. */
    data object Retry : LoginEvent()

    /** Dismiss error message. */
    data object DismissError : LoginEvent()

    /** User navigated back. */
    data object NavigateBack : LoginEvent()
}

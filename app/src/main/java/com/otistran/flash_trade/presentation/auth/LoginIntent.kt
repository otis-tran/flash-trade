package com.otistran.flash_trade.presentation.auth

import com.otistran.flash_trade.presentation.base.MviIntent

/**
 * User intents for login screen.
 */
sealed class LoginIntent : MviIntent {
    /** User tapped "Continue with Passkey" for login. */
    data object PasskeyLogin : LoginIntent()

    /** User tapped "Continue with Passkey" for first-time signup. */
    data object PasskeySignup : LoginIntent()

    /** User tapped "Continue with Google". */
    data object GoogleLogin : LoginIntent()

    /** User tapped retry after error. */
    data object Retry : LoginIntent()

    /** User navigated back. */
    data object NavigateBack : LoginIntent()
}

package com.otistran.flash_trade.presentation.auth

import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.presentation.base.MviState

/**
 * UI state for login screen.
 */
data class LoginState(
    val isPasskeyLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null
) : MviState {
    /** True if authentication succeeded. */
    val isAuthenticated: Boolean get() = user != null

    /** True if any auth operation in progress. */
    val isAnyLoading: Boolean get() = isPasskeyLoading || isGoogleLoading
}

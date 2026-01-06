package com.otistran.flash_trade.presentation.feature.auth

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.User

/**
 * UI state for login screen.
 */
@Stable
data class LoginState(
    val isPasskeyLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val error: String? = null,
    val user: User? = null
) : UiState {

    /** True if authentication succeeded. */
    val isAuthenticated: Boolean get() = user != null

    /** True if any auth operation in progress. */
    val isAnyLoading: Boolean get() = isPasskeyLoading || isGoogleLoading

    /** True if screen is idle and can accept login. */
    val canLogin: Boolean get() = !isAnyLoading && !isCheckingSession
}
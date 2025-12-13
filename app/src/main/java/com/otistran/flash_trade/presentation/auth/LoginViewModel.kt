package com.otistran.flash_trade.presentation.auth

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.BuildConfig
import com.otistran.flash_trade.domain.model.AuthMethod
import com.otistran.flash_trade.domain.model.OAuthProvider
import com.otistran.flash_trade.domain.usecase.LoginUseCase
import com.otistran.flash_trade.presentation.base.MviContainer
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for login screen.
 * Handles passkey and OAuth authentication.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : MviContainer<LoginState, LoginIntent, LoginSideEffect>(
    initialState = LoginState()
) {

    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            LoginIntent.PasskeyLogin -> handlePasskeyLogin(isSignup = false)
            LoginIntent.PasskeySignup -> handlePasskeyLogin(isSignup = true)
            LoginIntent.GoogleLogin -> handleGoogleLogin()
            LoginIntent.Retry -> handleRetry()
            LoginIntent.NavigateBack -> emitSideEffect(LoginSideEffect.NavigateBack)
        }
    }

    private fun handlePasskeyLogin(isSignup: Boolean) {
        viewModelScope.launch {
            reduce { copy(isPasskeyLoading = true, error = null) }

            val method = AuthMethod.Passkey(relyingParty = BuildConfig.PRIVY_RELYING_PARTY)
            when (val result = loginUseCase(method, isSignup)) {
                is Result.Success -> {
                    reduce { copy(isPasskeyLoading = false, user = result.data) }
                    emitSideEffect(LoginSideEffect.NavigateToTrading)
                }
                is Result.Error -> {
                    reduce { copy(isPasskeyLoading = false, error = result.message) }
                }
                Result.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    private fun handleGoogleLogin() {
        viewModelScope.launch {
            reduce { copy(isGoogleLoading = true, error = null) }

            val method = AuthMethod.OAuth(
                provider = OAuthProvider.GOOGLE,
                scheme = BuildConfig.PRIVY_OAUTH_SCHEME
            )
            when (val result = loginUseCase(method, isSignup = false)) {
                is Result.Success -> {
                    reduce { copy(isGoogleLoading = false, user = result.data) }
                    emitSideEffect(LoginSideEffect.NavigateToTrading)
                }
                is Result.Error -> {
                    reduce { copy(isGoogleLoading = false, error = result.message) }
                }
                Result.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    private fun handleRetry() {
        reduce { copy(error = null) }
    }
}

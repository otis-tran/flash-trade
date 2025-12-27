package com.otistran.flash_trade.presentation.feature.auth
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.BuildConfig
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.domain.model.AuthMethod
import com.otistran.flash_trade.domain.model.OAuthProvider
import com.otistran.flash_trade.domain.usecase.auth.CheckLoginStatusUseCase
import com.otistran.flash_trade.domain.usecase.auth.LoginUseCase
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "LoginViewModel"

/**
 * ViewModel for login screen.
 * Handles passkey and OAuth authentication.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val checkLoginStatusUseCase: CheckLoginStatusUseCase
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(
    initialState = LoginState()
) {

    init {
        checkExistingSession()
    }

    override fun onEvent(event: LoginEvent) {
        when (event) {
            LoginEvent.PasskeyLogin -> handlePasskeyLogin(isSignup = false)
            LoginEvent.PasskeySignup -> handlePasskeyLogin(isSignup = true)
            LoginEvent.GoogleLogin -> handleGoogleLogin()
            LoginEvent.Retry -> handleRetry()
            LoginEvent.DismissError -> setState { copy(error = null) }
            LoginEvent.NavigateBack -> setEffect(LoginEffect.NavigateBack)
        }
    }

    // ==================== Session Check ====================

    private fun checkExistingSession() {
        viewModelScope.launch {
            setState { copy(isCheckingSession = true) }

            when (val result = checkLoginStatusUseCase()) {
                is Result.Success -> {
                    val authState = result.data
                    setState { copy(isCheckingSession = false) }

                    if (authState.isLoggedIn && authState.isSessionValid) {
                        Log.d(TAG, "Valid session found, navigating to trading")
                        setEffect(LoginEffect.NavigateToTrading)
                    }
                }

                is Result.Error -> {
                    Log.w(TAG, "Check login status failed: ${result.message}")
                    setState { copy(isCheckingSession = false) }
                }

                Result.Loading -> { /* Already checking */ }
            }
        }
    }

    // ==================== Passkey Login ====================

    private fun handlePasskeyLogin(isSignup: Boolean) {
        viewModelScope.launch {
            setState { copy(isPasskeyLoading = true, error = null) }

            val method = AuthMethod.Passkey(
                relyingParty = BuildConfig.PRIVY_RELYING_PARTY
            )

            when (val result = loginUseCase(method, isSignup)) {
                is Result.Success -> {
                    setState { copy(isPasskeyLoading = false, user = result.data) }
                    setEffect(LoginEffect.NavigateToTrading)
                }

                is Result.Error -> {
                    setState { copy(isPasskeyLoading = false, error = result.message) }
                }

                Result.Loading -> { /* Already loading */ }
            }
        }
    }

    // ==================== Google Login ====================

    private fun handleGoogleLogin() {
        viewModelScope.launch {
            setState { copy(isGoogleLoading = true, error = null) }

            val method = AuthMethod.OAuth(
                provider = OAuthProvider.GOOGLE,
                scheme = BuildConfig.PRIVY_OAUTH_SCHEME
            )

            when (val result = loginUseCase(method, isSignup = false)) {
                is Result.Success -> {
                    setState { copy(isGoogleLoading = false, user = result.data) }
                    setEffect(LoginEffect.NavigateToTrading)
                }

                is Result.Error -> {
                    setState { copy(isGoogleLoading = false, error = result.message) }
                }

                Result.Loading -> { /* Already loading */ }
            }
        }
    }

    // ==================== Retry ====================

    private fun handleRetry() {
        setState { copy(error = null) }
    }
}
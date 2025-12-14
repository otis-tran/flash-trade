package com.otistran.flash_trade.presentation.portfolio

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.presentation.base.MviContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for portfolio screen.
 * Loads and displays user info from local auth state.
 */
@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : MviContainer<PortfolioState, PortfolioIntent, PortfolioSideEffect>(
    initialState = PortfolioState()
) {

    init {
        loadUserData()
    }

    override fun onIntent(intent: PortfolioIntent) {
        when (intent) {
            PortfolioIntent.Refresh -> loadUserData()
            PortfolioIntent.DismissError -> reduce { copy(error = null) }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            reduce { copy(isLoading = true) }

            authRepository.observeUserAuthState()
                .catch { e ->
                    reduce { copy(isLoading = false, error = e.message) }
                }
                .collect { authState ->
                    reduce {
                        copy(
                            displayName = authState.displayName,
                            userEmail = authState.userEmail,
                            walletAddress = authState.walletAddress,
                            isLoading = false
                        )
                    }
                }
        }
    }
}

package com.otistran.flash_trade.presentation.feature.settings

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.core.datastore.UserPreferences
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.domain.repository.SettingsRepository
import com.otistran.flash_trade.domain.usecase.auth.LogoutUseCase
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userPreferences: UserPreferences,
    private val logoutUseCase: LogoutUseCase
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(
    initialState = SettingsState()
) {

    init {
        observeSettings()
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            // Network
            SettingsEvent.ToggleNetworkSelector -> toggleNetworkSelector()
            is SettingsEvent.SelectNetwork -> selectNetwork(event.network)

            // Theme
            is SettingsEvent.ChangeThemeMode -> changeThemeMode(event.mode)

            // Logout
            SettingsEvent.RequestLogout -> setState { copy(showLogoutConfirmSheet = true) }
            SettingsEvent.ConfirmLogout -> confirmLogout()
            SettingsEvent.CancelLogout -> setState { copy(showLogoutConfirmSheet = false) }

            // Error
            SettingsEvent.DismissError -> setState { copy(error = null) }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .catch { e ->
                    setState { copy(isLoading = false, error = e.message) }
                }
                .collect { settings ->
                    setState {
                        copy(
                            networkMode = settings.networkMode,
                            themeMode = settings.themeMode,
                            isAutoSellEnabled = settings.isAutoSellEnabled,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun toggleNetworkSelector() {
        setState { copy(showNetworkSelector = !showNetworkSelector) }
    }

    private fun selectNetwork(network: NetworkMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setNetworkMode(network)
                setState { copy(showNetworkSelector = false) }
                setEffect(SettingsEffect.ShowToast("Switched to ${network.displayName}"))
            } catch (e: Exception) {
                setState { copy(error = e.message) }
            }
        }
    }

    private fun changeThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setThemeMode(mode)
            } catch (e: Exception) {
                setState { copy(error = e.message) }
            }
        }
    }

    private fun confirmLogout() {
        viewModelScope.launch {
            setState { copy(isLoggingOut = true, showLogoutConfirmSheet = false) }

            when (val result = logoutUseCase()) {
                is Result.Success -> {
                    userPreferences.clear()
                    settingsRepository.clearSettings()
                    setEffect(SettingsEffect.NavigateToLogin)
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoggingOut = false,
                            error = "Logout failed: ${result.message}"
                        )
                    }
                }
                Result.Loading -> { /* Already in loading state */ }
            }
        }
    }
}
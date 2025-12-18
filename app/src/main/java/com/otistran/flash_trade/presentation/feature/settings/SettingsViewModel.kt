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

/**
 * ViewModel for Settings screen.
 * Manages network mode, theme mode, and logout flow.
 */
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
            // Network mode
            is SettingsEvent.ChangeNetworkMode -> handleNetworkModeChange(event.mode)
            SettingsEvent.ConfirmMainnetSwitch -> handleMainnetConfirm()
            SettingsEvent.CancelMainnetSwitch -> handleMainnetCancel()

            // Theme mode
            is SettingsEvent.ChangeThemeMode -> handleThemeModeChange(event.mode)

            // Logout
            SettingsEvent.RequestLogout -> handleLogoutRequest()
            SettingsEvent.ConfirmLogout -> handleLogoutConfirm()
            SettingsEvent.CancelLogout -> handleLogoutCancel()

            // Error
            SettingsEvent.DismissError -> setState { copy(error = null) }
        }
    }

    // ==================== Settings Observation ====================

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

    // ==================== Network Mode ====================

    private fun handleNetworkModeChange(newMode: NetworkMode) {
        // Show confirmation only when switching TO mainnet
        if (newMode == NetworkMode.MAINNET && currentState.networkMode != NetworkMode.MAINNET) {
            setState {
                copy(
                    showMainnetConfirmDialog = true,
                    pendingNetworkMode = newMode
                )
            }
        } else {
            applyNetworkMode(newMode)
        }
    }

    private fun handleMainnetConfirm() {
        currentState.pendingNetworkMode?.let { mode ->
            applyNetworkMode(mode)
        }
        setState {
            copy(
                showMainnetConfirmDialog = false,
                pendingNetworkMode = null
            )
        }
    }

    private fun handleMainnetCancel() {
        setState {
            copy(
                showMainnetConfirmDialog = false,
                pendingNetworkMode = null
            )
        }
    }

    private fun applyNetworkMode(mode: NetworkMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setNetworkMode(mode)
                setEffect(SettingsEffect.ShowToast("Switched to ${mode.displayName}"))
            } catch (e: Exception) {
                setState { copy(error = e.message) }
            }
        }
    }

    // ==================== Theme Mode ====================

    private fun handleThemeModeChange(newMode: ThemeMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setThemeMode(newMode)
            } catch (e: Exception) {
                setState { copy(error = e.message) }
            }
        }
    }

    // ==================== Logout ====================

    private fun handleLogoutRequest() {
        setState { copy(showLogoutConfirmSheet = true) }
    }

    private fun handleLogoutConfirm() {
        viewModelScope.launch {
            setState {
                copy(
                    isLoggingOut = true,
                    showLogoutConfirmSheet = false
                )
            }

            when (val result = logoutUseCase()) {
                is Result.Success -> {
                    // Clear all local data
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

                Result.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    private fun handleLogoutCancel() {
        setState { copy(showLogoutConfirmSheet = false) }
    }
}
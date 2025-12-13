package com.otistran.flash_trade.presentation.settings

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.data.local.datastore.UserPreferences
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.domain.repository.SettingsRepository
import com.otistran.flash_trade.presentation.base.MviContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for settings screen.
 * Manages network mode, theme mode, and logout flow.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userPreferences: UserPreferences
) : MviContainer<SettingsState, SettingsIntent, SettingsSideEffect>(
    initialState = SettingsState()
) {

    init {
        loadSettings()
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ToggleNetworkMode -> handleNetworkModeToggle(intent.newMode)
            SettingsIntent.ConfirmMainnetSwitch -> handleMainnetConfirm()
            SettingsIntent.CancelMainnetSwitch -> handleMainnetCancel()
            is SettingsIntent.ToggleThemeMode -> handleThemeModeToggle(intent.newMode)
            SettingsIntent.RequestLogout -> handleLogoutRequest()
            SettingsIntent.ConfirmLogout -> handleLogoutConfirm()
            SettingsIntent.CancelLogout -> handleLogoutCancel()
            SettingsIntent.DismissError -> reduce { copy(error = null) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .catch { e ->
                    reduce { copy(isLoading = false, error = e.message) }
                }
                .collect { settings ->
                    reduce {
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

    private fun handleNetworkModeToggle(newMode: NetworkMode) {
        // Show confirmation only when switching TO mainnet
        if (newMode == NetworkMode.MAINNET && state.value.networkMode != NetworkMode.MAINNET) {
            reduce {
                copy(
                    showMainnetConfirmDialog = true,
                    pendingNetworkMode = newMode
                )
            }
        } else {
            // Instant switch for testnet
            applyNetworkMode(newMode)
        }
    }

    private fun handleMainnetConfirm() {
        state.value.pendingNetworkMode?.let { mode ->
            applyNetworkMode(mode)
        }
        reduce {
            copy(
                showMainnetConfirmDialog = false,
                pendingNetworkMode = null
            )
        }
    }

    private fun handleMainnetCancel() {
        reduce {
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
                emitSideEffect(
                    SettingsSideEffect.ShowToast("Switched to ${mode.displayName}")
                )
            } catch (e: Exception) {
                reduce { copy(error = e.message) }
            }
        }
    }

    private fun handleThemeModeToggle(newMode: ThemeMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setThemeMode(newMode)
            } catch (e: Exception) {
                reduce { copy(error = e.message) }
            }
        }
    }

    private fun handleLogoutRequest() {
        reduce { copy(showLogoutConfirmSheet = true) }
    }

    private fun handleLogoutConfirm() {
        viewModelScope.launch {
            reduce { copy(isLoggingOut = true, showLogoutConfirmSheet = false) }
            try {
                // Clear all user data
                userPreferences.clear()
                settingsRepository.clearSettings()

                // Navigate to login
                emitSideEffect(SettingsSideEffect.NavigateToLogin)
            } catch (e: Exception) {
                reduce { copy(isLoggingOut = false, error = e.message) }
            }
        }
    }

    private fun handleLogoutCancel() {
        reduce { copy(showLogoutConfirmSheet = false) }
    }
}

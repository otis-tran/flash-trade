package com.otistran.flash_trade.presentation.feature.settings

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode

@Stable
data class SettingsState(
    val networkMode: NetworkMode = NetworkMode.ETHEREUM,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isAutoSellEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val isLoggingOut: Boolean = false,
    val showNetworkSelector: Boolean = false,
    val showLogoutConfirmSheet: Boolean = false,
    val error: String? = null
) : UiState {
    val hasActiveOperation: Boolean get() = isLoading || isLoggingOut
}
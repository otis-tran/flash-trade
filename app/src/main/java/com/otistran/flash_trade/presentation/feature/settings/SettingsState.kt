package com.otistran.flash_trade.presentation.feature.settings

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode

@Stable
data class SettingsState(
    val networkMode: NetworkMode = NetworkMode.TESTNET,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isAutoSellEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val isLoggingOut: Boolean = false,
    val showMainnetConfirmDialog: Boolean = false,
    val showLogoutConfirmSheet: Boolean = false,
    val pendingNetworkMode: NetworkMode? = null,
    val error: String? = null
) : UiState {
    val isMainnet: Boolean get() = networkMode == NetworkMode.MAINNET
    val hasActiveOperation: Boolean get() = isLoading || isLoggingOut
}
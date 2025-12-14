package com.otistran.flash_trade.presentation.settings

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.presentation.base.MviState

/**
 * UI state for settings screen.
 */
@Stable
data class SettingsState(
    val networkMode: NetworkMode = NetworkMode.TESTNET,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isAutoSellEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val showMainnetConfirmDialog: Boolean = false,
    val showLogoutConfirmSheet: Boolean = false,
    val pendingNetworkMode: NetworkMode? = null,
    val isLoggingOut: Boolean = false,
    val error: String? = null
) : MviState {
    /** True if network is production (show warning). */
    val isMainnet: Boolean get() = networkMode == NetworkMode.MAINNET

    /** True if any operation in progress. */
    val hasActiveOperation: Boolean get() = isLoading || isLoggingOut
}

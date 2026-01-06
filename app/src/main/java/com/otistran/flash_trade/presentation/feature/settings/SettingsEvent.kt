package com.otistran.flash_trade.presentation.feature.settings

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.core.base.UiEvent
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode

@Immutable
sealed class SettingsEvent : UiEvent {
    // Network
    data object ToggleNetworkSelector : SettingsEvent()
    data class SelectNetwork(val network: NetworkMode) : SettingsEvent()

    // Theme
    data class ChangeThemeMode(val mode: ThemeMode) : SettingsEvent()

    // Logout
    data object RequestLogout : SettingsEvent()
    data object ConfirmLogout : SettingsEvent()
    data object CancelLogout : SettingsEvent()

    // Error
    data object DismissError : SettingsEvent()
}
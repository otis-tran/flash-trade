package com.otistran.flash_trade.presentation.feature.settings

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.core.base.UiEvent
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode

@Immutable
sealed class SettingsEvent : UiEvent {
    data class ChangeNetworkMode(val mode: NetworkMode) : SettingsEvent()
    data object ConfirmMainnetSwitch : SettingsEvent()
    data object CancelMainnetSwitch : SettingsEvent()
    data class ChangeThemeMode(val mode: ThemeMode) : SettingsEvent()
    data object RequestLogout : SettingsEvent()
    data object ConfirmLogout : SettingsEvent()
    data object CancelLogout : SettingsEvent()
    data object DismissError : SettingsEvent()
}
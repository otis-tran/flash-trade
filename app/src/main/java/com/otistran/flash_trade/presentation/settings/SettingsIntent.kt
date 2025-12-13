package com.otistran.flash_trade.presentation.settings

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.presentation.base.MviIntent

/**
 * User intents for settings screen.
 */
sealed class SettingsIntent : MviIntent {
    /** User toggled network mode switch. */
    data class ToggleNetworkMode(val newMode: NetworkMode) : SettingsIntent()

    /** User confirmed mainnet switch in dialog. */
    data object ConfirmMainnetSwitch : SettingsIntent()

    /** User canceled mainnet switch. */
    data object CancelMainnetSwitch : SettingsIntent()

    /** User toggled theme mode. */
    data class ToggleThemeMode(val newMode: ThemeMode) : SettingsIntent()

    /** User tapped logout button. */
    data object RequestLogout : SettingsIntent()

    /** User confirmed logout in bottom sheet. */
    data object ConfirmLogout : SettingsIntent()

    /** User canceled logout. */
    data object CancelLogout : SettingsIntent()

    /** Dismiss error message. */
    data object DismissError : SettingsIntent()
}

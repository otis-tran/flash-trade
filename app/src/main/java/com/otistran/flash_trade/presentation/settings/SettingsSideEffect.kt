package com.otistran.flash_trade.presentation.settings

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.presentation.base.MviSideEffect

/**
 * One-time side effects for settings screen.
 */
@Immutable
sealed class SettingsSideEffect : MviSideEffect {
    /** Navigate back to login after logout. */
    data object NavigateToLogin : SettingsSideEffect()

    /** Show toast message. */
    data class ShowToast(val message: String) : SettingsSideEffect()
}

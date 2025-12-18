package com.otistran.flash_trade.presentation.feature.settings

import androidx.compose.runtime.Immutable
import com.otistran.flash_trade.core.base.UiEffect

@Immutable
sealed class SettingsEffect : UiEffect {
    data object NavigateToLogin : SettingsEffect()
    data class ShowToast(val message: String) : SettingsEffect()
}
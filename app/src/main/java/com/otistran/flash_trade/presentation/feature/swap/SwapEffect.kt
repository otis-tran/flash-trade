package com.otistran.flash_trade.presentation.feature.swap

import com.otistran.flash_trade.core.base.UiEffect

sealed interface SwapEffect : UiEffect {
    data class ShowToast(val message: String) : SwapEffect
    data class NavigateToTxDetails(val txHash: String) : SwapEffect
    data object NavigateBack : SwapEffect
    data class OpenTokenSelector(val isFrom: Boolean) : SwapEffect
}

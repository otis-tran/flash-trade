package com.otistran.flash_trade.presentation.feature.swap

import com.otistran.flash_trade.core.base.UiEffect

/**
 * Side effects for Swap screen.
 */
sealed interface SwapEffect : UiEffect {
    data class ShowToast(val message: String) : SwapEffect
    data class NavigateToTxDetails(val txHash: String) : SwapEffect
    data object NavigateBack : SwapEffect
    data object NavigateToHome : SwapEffect
}

package com.otistran.flash_trade.presentation.feature.swap

import com.otistran.flash_trade.core.base.UiEvent

/**
 * Events for Swap screen.
 */
sealed interface SwapEvent : UiEvent {
    // Token Selection
    data object OpenSellTokenSelector : SwapEvent
    data object OpenBuyTokenSelector : SwapEvent
    data object CloseTokenSelector : SwapEvent
    data class SelectSellToken(val token: SwapToken) : SwapEvent
    data class SelectBuyToken(val token: SwapToken) : SwapEvent
    data class SearchTokens(val query: String) : SwapEvent

    // Amount Input
    data class SetSellAmount(val amount: String) : SwapEvent

    // Swap Direction
    data object SwapTokens : SwapEvent

    // Slippage
    data object OpenSlippageDialog : SwapEvent
    data object CloseSlippageDialog : SwapEvent
    data class SetSlippage(val slippage: Double) : SwapEvent

    // Token filter
    data object ToggleTokenFilter : SwapEvent

    // Actions
    data object ExecuteSwap : SwapEvent
    data object RefreshBalances : SwapEvent
    data object RefreshQuote : SwapEvent

    // Navigation
    data object NavigateBack : SwapEvent
    data object Cancel : SwapEvent

    // Error
    data object DismissError : SwapEvent
}

package com.otistran.flash_trade.presentation.feature.swap

import com.otistran.flash_trade.core.base.UiEvent
import com.otistran.flash_trade.domain.model.Token

sealed interface SwapEvent : UiEvent {
    data class LoadToken(val address: String) : SwapEvent
    data class SelectTokenFrom(val token: Token) : SwapEvent
    data class SelectTokenTo(val token: Token) : SwapEvent
    data class SetAmount(val amount: String) : SwapEvent
    data object FetchQuote : SwapEvent
    data object ExecuteSwap : SwapEvent
    data object SwapTokens : SwapEvent
    data object SetMaxAmount : SwapEvent
    data object DismissError : SwapEvent
    data object RefreshQuote : SwapEvent
}

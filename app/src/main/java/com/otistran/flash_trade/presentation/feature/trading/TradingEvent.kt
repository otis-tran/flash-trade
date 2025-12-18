package com.otistran.flash_trade.presentation.feature.trading

import com.otistran.flash_trade.core.base.UiEvent
import com.otistran.flash_trade.domain.model.Token

sealed interface TradingEvent : UiEvent {
    data object LoadTokens : TradingEvent
    data object LoadMore : TradingEvent
    data object Refresh : TradingEvent
    data class Search(val query: String) : TradingEvent
    data class SelectToken(val token: Token) : TradingEvent
    data object DismissError : TradingEvent
}
package com.otistran.flash_trade.presentation.feature.trading

import com.otistran.flash_trade.core.base.UiEvent
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenDisplayFilter

sealed interface TradingEvent : UiEvent {
    data class Search(val query: String) : TradingEvent
    data class UpdateFilter(val filter: TokenDisplayFilter) : TradingEvent
    data object ToggleFilterSheet : TradingEvent
    data class SelectToken(val token: Token) : TradingEvent
    data object DismissError : TradingEvent
}
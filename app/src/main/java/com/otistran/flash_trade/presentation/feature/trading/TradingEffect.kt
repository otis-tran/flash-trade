package com.otistran.flash_trade.presentation.feature.trading

import com.otistran.flash_trade.core.base.UiEffect

sealed interface TradingEffect : UiEffect {
    data class ShowToast(val message: String) : TradingEffect
    data class NavigateToTradeDetails(val tokenAddress: String) : TradingEffect
}
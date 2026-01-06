package com.otistran.flash_trade.presentation.feature.activity

import com.otistran.flash_trade.core.base.UiEffect

sealed interface ActivityEffect : UiEffect {
    data class ShowToast(val message: String) : ActivityEffect
    data class OpenExplorerTx(val txHash: String, val explorerUrl: String) : ActivityEffect
}
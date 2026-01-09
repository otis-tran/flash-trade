package com.otistran.flash_trade.presentation.feature.activity

import com.otistran.flash_trade.core.base.UiEvent

sealed interface ActivityEvent : UiEvent {
    data object LoadTransactions : ActivityEvent
    data object RefreshTransactions : ActivityEvent
    data object LoadMoreTransactions : ActivityEvent
    data class OpenTransactionDetails(val txHash: String) : ActivityEvent
    data class SelectTab(val tab: ActivityTab) : ActivityEvent
    data class RetryAutoSell(val txHash: String) : ActivityEvent
    data object DismissError : ActivityEvent
}

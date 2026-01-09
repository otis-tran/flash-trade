package com.otistran.flash_trade.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-wide event bus for cross-screen communication.
 * Uses SharedFlow to emit events that can be collected from any screen.
 */
@Singleton
class AppEventBus @Inject constructor() {

    private val _toastEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _refreshPortfolio = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val refreshPortfolio: SharedFlow<Unit> = _refreshPortfolio.asSharedFlow()

    suspend fun showToast(message: String) {
        _toastEvents.emit(message)
    }

    suspend fun triggerRefreshPortfolio() {
        _refreshPortfolio.emit(Unit)
    }
}

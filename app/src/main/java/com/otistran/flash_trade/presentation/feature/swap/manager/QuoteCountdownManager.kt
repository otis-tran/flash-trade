package com.otistran.flash_trade.presentation.feature.swap.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages quote countdown timer for swap screen.
 * Handles auto-refresh when quote expires.
 */
class QuoteCountdownManager(
    private val coroutineScope: CoroutineScope
) {
    private var countdownJob: Job? = null

    private val _secondsRemaining = MutableStateFlow(0)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _isExpired = MutableStateFlow(false)
    val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    /**
     * Start countdown from given seconds.
     * Calls onExpired when countdown reaches 0.
     */
    fun start(
        seconds: Int,
        onExpired: () -> Unit
    ) {
        stop()
        _isExpired.value = false
        _secondsRemaining.value = seconds

        countdownJob = coroutineScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _secondsRemaining.value = remaining
            }
            _isExpired.value = true
            onExpired()
        }
    }

    /**
     * Stop countdown and reset state.
     */
    fun stop() {
        countdownJob?.cancel()
        countdownJob = null
        _secondsRemaining.value = 0
    }

    /**
     * Cancel countdown without resetting state (for cleanup).
     */
    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
    }
}

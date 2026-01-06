package com.otistran.flash_trade.domain.manager

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics manager for swap flow tracking.
 * Tracks funnel: select → quote → confirm → success/fail
 */
@Singleton
class SwapAnalyticsManager @Inject constructor() {

    private var swapStartTime: Long = 0
    private var quoteStartTime: Long = 0

    /**
     * Track when user starts swap flow (enters SwapScreen).
     */
    fun trackSwapStarted(tokenIn: String, tokenOut: String) {
        swapStartTime = System.currentTimeMillis()
        Timber.d("Swap started: $tokenIn → $tokenOut")

    }

    /**
     * Track quote fetch started.
     */
    fun trackQuoteFetchStarted() {
        quoteStartTime = System.currentTimeMillis()
    }

    /**
     * Track quote fetch completed.
     */
    fun trackQuoteFetched(success: Boolean, cached: Boolean = false) {
        val durationMs = System.currentTimeMillis() - quoteStartTime
        Timber.d("Quote fetched: success=$success, cached=$cached, duration=${durationMs}ms")

    }

    /**
     * Track swap execution started.
     */
    fun trackSwapExecutionStarted(amountIn: String, amountOut: String) {
        Timber.d("Swap execution: in=$amountIn, out=$amountOut")

    }

    /**
     * Track swap completed successfully.
     */
    fun trackSwapCompleted(txHash: String) {
        val totalDurationMs = System.currentTimeMillis() - swapStartTime
        Timber.d("Swap completed: txHash=$txHash, total=${totalDurationMs}ms")

    }

    /**
     * Track swap failed.
     */
    fun trackSwapFailed(error: String, stage: SwapStage) {
        Timber.e("Swap failed at $stage: $error")

    }

    /**
     * Track user cancelled swap.
     */
    fun trackSwapCancelled(stage: SwapStage) {
        Timber.d("Swap cancelled at $stage")

    }
}

/**
 * Stages in the swap flow for analytics tracking.
 */
enum class SwapStage {
    TOKEN_SELECTION,
    QUOTE_FETCH,
    USER_CONFIRMATION,
    TRANSACTION_SIGNING,
    TRANSACTION_BROADCAST,
    TRANSACTION_CONFIRMATION
}

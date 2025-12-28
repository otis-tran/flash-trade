package com.otistran.flash_trade.domain.manager

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SwapAnalytics"

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
        Log.d(TAG, "Swap started: $tokenIn → $tokenOut")
        // TODO: Send to Firebase Analytics
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
        Log.d(TAG, "Quote fetched: success=$success, cached=$cached, duration=${durationMs}ms")
        // TODO: Send to Firebase Analytics
    }

    /**
     * Track swap execution started.
     */
    fun trackSwapExecutionStarted(amountIn: String, amountOut: String) {
        Log.d(TAG, "Swap execution: in=$amountIn, out=$amountOut")
        // TODO: Send to Firebase Analytics
    }

    /**
     * Track swap completed successfully.
     */
    fun trackSwapCompleted(txHash: String) {
        val totalDurationMs = System.currentTimeMillis() - swapStartTime
        Log.d(TAG, "Swap completed: txHash=$txHash, total=${totalDurationMs}ms")
        // TODO: Send to Firebase Analytics
    }

    /**
     * Track swap failed.
     */
    fun trackSwapFailed(error: String, stage: SwapStage) {
        Log.e(TAG, "Swap failed at $stage: $error")
        // TODO: Send to Firebase Analytics
    }

    /**
     * Track user cancelled swap.
     */
    fun trackSwapCancelled(stage: SwapStage) {
        Log.d(TAG, "Swap cancelled at $stage")
        // TODO: Send to Firebase Analytics
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

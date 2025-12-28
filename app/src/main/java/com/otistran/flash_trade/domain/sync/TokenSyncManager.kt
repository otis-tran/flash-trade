package com.otistran.flash_trade.domain.sync

import com.otistran.flash_trade.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages background synchronization of tokens from API to local database.
 * Uses WorkManager for background sync that survives app backgrounding.
 */
interface TokenSyncManager {

    /**
     * Observable sync state.
     */
    val syncState: StateFlow<SyncState>

    /**
     * Check if sync is needed and start if necessary.
     * Called on app startup.
     */
    suspend fun checkAndStartSync()

    /**
     * Force start a full sync regardless of cache state.
     * Called on pull-to-refresh.
     */
    suspend fun forceSync()

    /**
     * Check if sync should run based on cache freshness.
     */
    suspend fun shouldSync(): Boolean

    // ==================== WorkManager Methods ====================

    /**
     * Enqueue background sync via WorkManager.
     * Performs immediate fetch of top 50 pages, then enqueues remaining pages.
     */
    suspend fun enqueueBackgroundSync()

    /**
     * Cancel any ongoing background sync.
     */
    suspend fun cancelBackgroundSync()

    /**
     * Get current sync progress.
     */
    suspend fun getSyncProgress(): SyncProgress?
}

/**
 * Sync progress data class.
 */
data class SyncProgress(
    val lastPageSynced: Int,
    val totalPages: Int,
    val tokensCached: Int,
    val isRunning: Boolean
)
package com.otistran.flash_trade.domain.model

/**
 * Represents the current state of token synchronization.
 */
sealed class SyncState {
    
    /**
     * No sync in progress, ready to start.
     */
    data object Idle : SyncState()
    
    /**
     * Sync is currently running.
     */
    data class Syncing(
        val currentPage: Int,
        val totalPages: Int?, // null if unknown yet
        val tokensFetched: Int
    ) : SyncState()
    
    /**
     * Sync completed successfully.
     */
    data class Completed(
        val totalTokensSynced: Int,
        val tokensDeleted: Int,
        val durationMs: Long
    ) : SyncState()
    
    /**
     * Sync failed with error.
     */
    data class Error(
        val message: String,
        val lastSuccessfulPage: Int,
        val canRetry: Boolean = true
    ) : SyncState()
}
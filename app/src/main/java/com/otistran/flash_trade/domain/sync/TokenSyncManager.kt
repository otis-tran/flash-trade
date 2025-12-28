package com.otistran.flash_trade.domain.sync

import com.otistran.flash_trade.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages background synchronization of tokens from API to local database.
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
}
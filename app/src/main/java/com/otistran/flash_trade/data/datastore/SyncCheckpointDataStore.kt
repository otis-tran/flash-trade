package com.otistran.flash_trade.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore for persisting token sync checkpoint progress.
 * Survives app termination and worker restarts.
 */
private val Context.syncCheckpointDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_checkpoint")

@Singleton
class SyncCheckpointDataStore @Inject constructor(
    private val context: Context
) {

    private object Keys {
        val LAST_PAGE_SYNCED = intPreferencesKey("last_page_synced")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val TOTAL_PAGES = intPreferencesKey("total_pages")
        val CURRENT_BATCH = intPreferencesKey("current_batch")
    }

    // ==================== Read ====================

    /**
     * Flow emitting the last page number that was successfully synced.
     */
    val lastPageSynced: Flow<Int> = context.syncCheckpointDataStore.data.map { it ->
        it[Keys.LAST_PAGE_SYNCED] ?: 0
    }

    /**
     * Flow emitting the total number of pages to sync.
     */
    val totalPages: Flow<Int> = context.syncCheckpointDataStore.data.map { it ->
        it[Keys.TOTAL_PAGES] ?: 3218
    }

    /**
     * Flow emitting the current batch being processed.
     */
    val currentBatch: Flow<Int> = context.syncCheckpointDataStore.data.map { it ->
        it[Keys.CURRENT_BATCH] ?: 0
    }

    /**
     * Suspend function to get the last page synced.
     */
    suspend fun getLastPageSynced(): Int = lastPageSynced.first()

    /**
     * Suspend function to get the total pages.
     */
    suspend fun getTotalPages(): Int = totalPages.first()

    /**
     * Suspend function to get the current batch.
     */
    suspend fun getCurrentBatch(): Int = currentBatch.first()

    /**
     * Suspend function to get the last sync timestamp.
     */
    suspend fun getLastSyncTimestamp(): Long {
        return context.syncCheckpointDataStore.data.map { it ->
            it[Keys.LAST_SYNC_TIMESTAMP] ?: 0L
        }.first()
    }

    // ==================== Write ====================

    /**
     * Save sync progress after a batch completes.
     * @param page The last page number that was successfully synced
     * @param total Total number of pages to sync (default 3218)
     */
    suspend fun saveProgress(page: Int, total: Int = 3218) {
        context.syncCheckpointDataStore.edit { prefs ->
            prefs[Keys.LAST_PAGE_SYNCED] = page
            prefs[Keys.TOTAL_PAGES] = total
            prefs[Keys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * Update the current batch being processed.
     */
    suspend fun saveCurrentBatch(batch: Int) {
        context.syncCheckpointDataStore.edit { prefs ->
            prefs[Keys.CURRENT_BATCH] = batch
        }
    }

    /**
     * Reset all checkpoint data.
     */
    suspend fun reset() {
        context.syncCheckpointDataStore.edit { it.clear() }
    }
}

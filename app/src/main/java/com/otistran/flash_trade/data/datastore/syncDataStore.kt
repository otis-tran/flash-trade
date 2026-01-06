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

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_preferences")

@Singleton
class SyncPreferences @Inject constructor(
    private val context: Context
) {
    
    private object Keys {
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val CURRENT_GENERATION = intPreferencesKey("current_generation")
        val TOTAL_TOKENS_CACHED = intPreferencesKey("total_tokens_cached")
    }
    
    // ==================== Read ====================
    
    val lastSyncTime: Flow<Long> = context.syncDataStore.data.map { prefs ->
        prefs[Keys.LAST_SYNC_TIME] ?: 0L
    }
    
    val currentGeneration: Flow<Int> = context.syncDataStore.data.map { prefs ->
        prefs[Keys.CURRENT_GENERATION] ?: 0
    }
    
    val totalTokensCached: Flow<Int> = context.syncDataStore.data.map { prefs ->
        prefs[Keys.TOTAL_TOKENS_CACHED] ?: 0
    }
    
    suspend fun getLastSyncTime(): Long = lastSyncTime.first()
    
    suspend fun getCurrentGeneration(): Int = currentGeneration.first()
    
    // ==================== Write ====================
    
    suspend fun updateLastSyncTime(time: Long = System.currentTimeMillis()) {
        context.syncDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIME] = time
        }
    }
    
    suspend fun incrementGeneration(): Int {
        var newGeneration = 0
        context.syncDataStore.edit { prefs ->
            val current = prefs[Keys.CURRENT_GENERATION] ?: 0
            newGeneration = current + 1
            prefs[Keys.CURRENT_GENERATION] = newGeneration
        }
        return newGeneration
    }
    
    suspend fun updateTotalTokensCached(count: Int) {
        context.syncDataStore.edit { prefs ->
            prefs[Keys.TOTAL_TOKENS_CACHED] = count
        }
    }
    
    suspend fun clearAll() {
        context.syncDataStore.edit { it.clear() }
    }
}
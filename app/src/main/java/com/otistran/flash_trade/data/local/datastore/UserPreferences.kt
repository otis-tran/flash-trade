package com.otistran.flash_trade.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SELECTED_CHAIN_ID = intPreferencesKey("selected_chain_id")
        val AUTO_SELL_ENABLED = booleanPreferencesKey("auto_sell_enabled")
        val USER_ID = stringPreferencesKey("user_id")
        val WALLET_ADDRESS = stringPreferencesKey("wallet_address")
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_ONBOARDED] ?: false }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }
    val selectedChainId: Flow<Int> = context.dataStore.data.map { it[Keys.SELECTED_CHAIN_ID] ?: 1 }
    val autoSellEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_SELL_ENABLED] ?: true }
    val userId: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val walletAddress: Flow<String?> = context.dataStore.data.map { it[Keys.WALLET_ADDRESS] }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_ONBOARDED] = value }
    }

    suspend fun setBiometricEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = value }
    }

    suspend fun setSelectedChainId(chainId: Int) {
        context.dataStore.edit { it[Keys.SELECTED_CHAIN_ID] = chainId }
    }

    suspend fun setAutoSellEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SELL_ENABLED] = value }
    }

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { it[Keys.USER_ID] = userId }
    }

    suspend fun setWalletAddress(address: String) {
        context.dataStore.edit { it[Keys.WALLET_ADDRESS] = address }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

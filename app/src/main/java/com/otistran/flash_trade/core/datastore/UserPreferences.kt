package com.otistran.flash_trade.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.domain.model.UserAuthState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NETWORK_MODE = stringPreferencesKey("network_mode")

        // Auth
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")

        // Portfolio balance cache (Ethereum only)
        val ETH_BALANCE_ETHEREUM = stringPreferencesKey("eth_balance_ethereum")
        val BALANCE_CACHE_TS_ETHEREUM = longPreferencesKey("balance_cache_ts_ethereum")

        // Swap settings
        val DEFAULT_SLIPPAGE = doublePreferencesKey("default_slippage")
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_ONBOARDED] ?: false }
    val biometricEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }
    val selectedChainId: Flow<Int> = context.dataStore.data.map { it[Keys.SELECTED_CHAIN_ID] ?: 1 }
    val autoSellEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_SELL_ENABLED] ?: true }
    val userId: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val walletAddress: Flow<String?> = context.dataStore.data.map { it[Keys.WALLET_ADDRESS] }
    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "DARK" }
    val networkMode: Flow<String> = context.dataStore.data.map { it[Keys.NETWORK_MODE] ?: "ETHEREUM" }

    // Auth
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false }
    val authToken: Flow<String?> = context.dataStore.data.map { it[Keys.AUTH_TOKEN] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[Keys.USER_EMAIL] }
    val displayName: Flow<String?> = context.dataStore.data.map { it[Keys.DISPLAY_NAME] }
    val loginTimestamp: Flow<Long> = context.dataStore.data.map { it[Keys.LOGIN_TIMESTAMP] ?: 0L }

    // Combined auth state
    val authStateFlow: Flow<UserAuthState> = context.dataStore.data.map { prefs ->
        UserAuthState(
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
            userId = prefs[Keys.USER_ID],
            token = prefs[Keys.AUTH_TOKEN],
            userEmail = prefs[Keys.USER_EMAIL],
            displayName = prefs[Keys.DISPLAY_NAME],
            walletAddress = prefs[Keys.WALLET_ADDRESS],
            loginTimestamp = prefs[Keys.LOGIN_TIMESTAMP] ?: 0L
        )
    }

    // Auth methods
    suspend fun saveLoginState(user: User, token: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.USER_EMAIL] = user.email ?: ""
            prefs[Keys.DISPLAY_NAME] = user.displayName ?: ""
            prefs[Keys.LOGIN_TIMESTAMP] = System.currentTimeMillis()

            token?.let { prefs[Keys.AUTH_TOKEN] = it }
            user.walletAddress?.let { prefs[Keys.WALLET_ADDRESS] = it }
        }
    }

    suspend fun clearLoginState() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.IS_LOGGED_IN)
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.DISPLAY_NAME)
            prefs.remove(Keys.LOGIN_TIMESTAMP)
            // Keep USER_ID and WALLET_ADDRESS for potential cache
        }
    }

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

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setNetworkMode(mode: String) {
        context.dataStore.edit { it[Keys.NETWORK_MODE] = mode }
    }

    suspend fun getDefaultSlippage(): Double {
        return context.dataStore.data.first()[Keys.DEFAULT_SLIPPAGE] ?: 0.5
    }

    suspend fun setDefaultSlippage(slippage: Double) {
        context.dataStore.edit { it[Keys.DEFAULT_SLIPPAGE] = slippage }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // ==================== Portfolio Balance Cache ====================

    /**
     * Cache ETH balance for specific network.
     */
    suspend fun cacheBalance(chainId: Long, balance: Double) {
        context.dataStore.edit { prefs ->
            val balanceKey = getBalanceKey(chainId)
            val timestampKey = getBalanceTimestampKey(chainId)

            prefs[balanceKey] = balance.toString()
            prefs[timestampKey] = System.currentTimeMillis()
        }
    }

    /**
     * Get cached balance for specific network.
     * Returns null if not cached.
     */
    suspend fun getCachedBalance(chainId: Long): CachedBalance? {
        val prefs = context.dataStore.data.first()
        val balanceKey = getBalanceKey(chainId)
        val timestampKey = getBalanceTimestampKey(chainId)

        val balance = prefs[balanceKey]?.toDoubleOrNull() ?: return null
        val timestamp = prefs[timestampKey] ?: return null

        return CachedBalance(balance, timestamp)
    }

    private fun getBalanceKey(chainId: Long): Preferences.Key<String> {
        if (chainId != 1L) {
            throw IllegalArgumentException("Only Ethereum (chainId=1) is supported")
        }
        return Keys.ETH_BALANCE_ETHEREUM
    }

    private fun getBalanceTimestampKey(chainId: Long): Preferences.Key<Long> {
        if (chainId != 1L) {
            throw IllegalArgumentException("Only Ethereum (chainId=1) is supported")
        }
        return Keys.BALANCE_CACHE_TS_ETHEREUM
    }
}

/**
 * Cached balance with timestamp for TTL validation.
 */
data class CachedBalance(val balance: Double, val timestamp: Long) {
    /**
     * Check if cache is still valid within TTL.
     */
    fun isValid(ttlMs: Long): Boolean {
        return System.currentTimeMillis() - timestamp < ttlMs
    }
}

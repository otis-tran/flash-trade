package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.core.datastore.UserPreferences
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.Settings
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository using DataStore.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val userPreferences: UserPreferences
) : SettingsRepository {

    override fun observeSettings(): Flow<Settings> {
        return combine(
            userPreferences.networkMode,
            userPreferences.themeMode,
            userPreferences.autoSellEnabled
        ) { network, theme, autoSell ->
            Settings(
                networkMode = NetworkMode.valueOf(network),
                themeMode = ThemeMode.valueOf(theme),
                isAutoSellEnabled = autoSell
            )
        }
    }

    override suspend fun setNetworkMode(mode: NetworkMode) {
        userPreferences.setNetworkMode(mode.name)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        userPreferences.setThemeMode(mode.name)
    }

    override suspend fun setAutoSellEnabled(enabled: Boolean) {
        userPreferences.setAutoSellEnabled(enabled)
    }

    override suspend fun clearSettings() {
        userPreferences.clear()
    }
}

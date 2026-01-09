package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.Settings
import com.otistran.flash_trade.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for settings data.
 * Follows clean architecture - domain layer contract.
 */
interface SettingsRepository {
    /**
     * Observes current settings. Emits on every change.
     */
    fun observeSettings(): Flow<Settings>

    /**
     * Updates network mode.
     * @param mode New network mode
     */
    suspend fun setNetworkMode(mode: NetworkMode)

    /**
     * Updates theme mode.
     * @param mode New theme mode
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Updates auto-sell preference.
     * @param enabled Whether auto-sell is enabled
     */
    suspend fun setAutoSellEnabled(enabled: Boolean)

    /**
     * Updates auto-sell duration.
     * @param minutes Duration in minutes before auto-sell triggers
     */
    suspend fun setAutoSellDurationMinutes(minutes: Int)

    /**
     * Gets whether auto-sell is enabled.
     */
    suspend fun isAutoSellEnabled(): Boolean

    /**
     * Gets auto-sell duration in minutes.
     */
    suspend fun getAutoSellDurationMinutes(): Int

    /**
     * Gets the default slippage tolerance.
     * @return Slippage percentage (e.g., 0.5 for 0.5%)
     */
    suspend fun getDefaultSlippage(): Double

    /**
     * Updates the default slippage tolerance.
     * @param slippage Slippage percentage (e.g., 0.5 for 0.5%)
     */
    suspend fun setDefaultSlippage(slippage: Double)

    /**
     * Clears all settings (for logout).
     */
    suspend fun clearSettings()
}


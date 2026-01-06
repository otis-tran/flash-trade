package com.otistran.flash_trade.di

import com.otistran.flash_trade.core.datastore.UserPreferences
import com.otistran.flash_trade.data.repository.SettingsRepositoryImpl
import com.otistran.flash_trade.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for settings dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    fun provideSettingsRepository(
        userPreferences: UserPreferences
    ): SettingsRepository = SettingsRepositoryImpl(userPreferences)
}
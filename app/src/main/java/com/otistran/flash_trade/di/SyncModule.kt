package com.otistran.flash_trade.di

import android.content.Context
import com.otistran.flash_trade.data.datastore.SyncCheckpointDataStore
import com.otistran.flash_trade.data.datastore.SyncPreferences
import com.otistran.flash_trade.data.sync.TokenSyncManagerImpl
import com.otistran.flash_trade.domain.sync.TokenSyncManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindTokenSyncManager(
        impl: TokenSyncManagerImpl
    ): TokenSyncManager

    companion object {
        @Provides
        @Singleton
        fun provideSyncPreferences(
            @ApplicationContext context: Context
        ): SyncPreferences {
            return SyncPreferences(context)
        }

        @Provides
        @Singleton
        fun provideSyncCheckpointDataStore(
            @ApplicationContext context: Context
        ): SyncCheckpointDataStore {
            return SyncCheckpointDataStore(context)
        }
    }
}
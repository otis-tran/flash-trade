package com.otistran.flash_trade.di

import android.content.Context
import androidx.room.Room
import com.otistran.flash_trade.data.local.dao.TradeDao
import com.otistran.flash_trade.data.local.dao.WalletDao
import com.otistran.flash_trade.data.local.database.FlashTradeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FlashTradeDatabase = Room.databaseBuilder(
        context,
        FlashTradeDatabase::class.java,
        FlashTradeDatabase.DATABASE_NAME
    ).build()

    @Provides
    fun provideTradeDao(database: FlashTradeDatabase): TradeDao =
        database.tradeDao()

    @Provides
    fun provideWalletDao(database: FlashTradeDatabase): WalletDao =
        database.walletDao()
}

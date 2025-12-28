package com.otistran.flash_trade.di

import android.content.Context
import androidx.room.Room
import com.otistran.flash_trade.data.local.database.FlashTradeDatabase
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.local.database.dao.TransactionDao
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
    fun provideDatabase(@ApplicationContext context: Context): FlashTradeDatabase {
        return Room.databaseBuilder(
            context,
            FlashTradeDatabase::class.java,
            "flash_trade.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTokenDao(database: FlashTradeDatabase): TokenDao {
        return database.tokenDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: FlashTradeDatabase): TransactionDao {
        return database.transactionDao()
    }
}
package com.otistran.flash_trade.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.local.database.dao.TransactionDao
import com.otistran.flash_trade.data.local.entity.TokenEntity
import com.otistran.flash_trade.data.local.entity.TokenRemoteKeysEntity
import com.otistran.flash_trade.data.local.entity.TransactionEntity

/**
 * Room database for Flash Trade app.
 * Caches transaction and token data for offline-first experience.
 */
@Database(
    entities = [
        TokenEntity::class,
        TokenRemoteKeysEntity::class,
        TransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FlashTradeDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
    abstract fun transactionDao(): TransactionDao
}

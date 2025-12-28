package com.otistran.flash_trade.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.otistran.flash_trade.data.local.database.dao.TransactionDao
import com.otistran.flash_trade.data.local.entity.TransactionEntity

/**
 * Room database for Flash Trade app.
 * Caches transaction data for offline-first portfolio experience.
 */
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlashTradeDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}

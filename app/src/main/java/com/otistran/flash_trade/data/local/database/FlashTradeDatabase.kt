package com.otistran.flash_trade.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.otistran.flash_trade.data.local.database.converter.PurchaseStatusConverter
import com.otistran.flash_trade.data.local.database.dao.PurchaseDao
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.local.entity.PurchaseEntity
import com.otistran.flash_trade.data.local.entity.TokenEntity

/**
 * Room database for Flash Trade app.
 * Caches transaction and token data for offline-first experience.
 */
@Database(
    entities = [
        TokenEntity::class,
        PurchaseEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(PurchaseStatusConverter::class)
abstract class FlashTradeDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
    abstract fun purchaseDao(): PurchaseDao
}

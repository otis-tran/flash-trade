package com.otistran.flash_trade.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.otistran.flash_trade.data.local.dao.TradeDao
import com.otistran.flash_trade.data.local.dao.WalletDao
import com.otistran.flash_trade.data.local.entity.TradeEntity
import com.otistran.flash_trade.data.local.entity.WalletEntity

@Database(
    entities = [TradeEntity::class, WalletEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlashTradeDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun walletDao(): WalletDao

    companion object {
        const val DATABASE_NAME = "flash_trade_db"
    }
}

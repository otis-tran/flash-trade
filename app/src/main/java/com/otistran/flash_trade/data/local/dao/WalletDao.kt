package com.otistran.flash_trade.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.otistran.flash_trade.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: WalletEntity)

    @Query("SELECT * FROM wallets LIMIT 1")
    suspend fun getWallet(): WalletEntity?

    @Query("SELECT * FROM wallets LIMIT 1")
    fun observeWallet(): Flow<WalletEntity?>

    @Query("UPDATE wallets SET balance = :balance WHERE address = :address")
    suspend fun updateBalance(address: String, balance: Double)

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()
}

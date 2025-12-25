package com.otistran.flash_trade.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.otistran.flash_trade.data.local.entity.TransactionEntity

/**
 * DAO for transaction cache operations.
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE chainId = :chainId ORDER BY timeStamp DESC LIMIT :limit")
    suspend fun getTransactions(chainId: Long, limit: Int = 100): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE chainId = :chainId AND cachedAt > :minTimestamp ORDER BY timeStamp DESC")
    suspend fun getRecentTransactions(chainId: Long, minTimestamp: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE chainId = :chainId AND cachedAt < :expiredTimestamp")
    suspend fun deleteExpired(chainId: Long, expiredTimestamp: Long)

    @Query("DELETE FROM transactions WHERE chainId = :chainId")
    suspend fun deleteAllForChain(chainId: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE chainId = :chainId")
    suspend fun getTransactionCount(chainId: Long): Int
}

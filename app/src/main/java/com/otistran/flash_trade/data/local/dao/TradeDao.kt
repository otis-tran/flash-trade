package com.otistran.flash_trade.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.otistran.flash_trade.data.local.entity.TradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trade: TradeEntity)

    @Update
    suspend fun update(trade: TradeEntity)

    @Query("SELECT * FROM trades WHERE id = :id")
    suspend fun getById(id: String): TradeEntity?

    @Query("SELECT * FROM trades ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE status = 'PENDING' OR status = 'COMPLETED'")
    fun getPending(): Flow<List<TradeEntity>>

    @Query("UPDATE trades SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM trades WHERE id = :id")
    suspend fun deleteById(id: String)
}

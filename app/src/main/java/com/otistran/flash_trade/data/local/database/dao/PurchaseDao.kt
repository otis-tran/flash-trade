package com.otistran.flash_trade.data.local.database.dao

import androidx.room.*
import com.otistran.flash_trade.data.local.entity.PurchaseEntity
import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: PurchaseEntity)

    @Update
    suspend fun update(purchase: PurchaseEntity)

    @Query("SELECT * FROM purchases WHERE txHash = :txHash")
    suspend fun getByTxHash(txHash: String): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE status = :status ORDER BY purchaseTime DESC")
    fun observeByStatus(status: PurchaseStatus): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE status IN (:statuses) ORDER BY purchaseTime DESC")
    fun observeByStatuses(statuses: List<PurchaseStatus>): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY purchaseTime DESC")
    fun observeAll(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE status = 'HELD' AND autoSellTime <= :currentTime")
    suspend fun getPendingAutoSells(currentTime: Long): List<PurchaseEntity>

    @Query("UPDATE purchases SET status = :status WHERE txHash = :txHash")
    suspend fun updateStatus(txHash: String, status: PurchaseStatus)

    @Query("UPDATE purchases SET status = :status, sellTxHash = :sellTxHash WHERE txHash = :txHash")
    suspend fun updateSold(txHash: String, status: PurchaseStatus, sellTxHash: String)

    @Query("UPDATE purchases SET workerId = :workerId WHERE txHash = :txHash")
    suspend fun updateWorkerId(txHash: String, workerId: String)

    @Query("SELECT COUNT(*) FROM purchases WHERE status = 'HELD'")
    fun observeHeldCount(): Flow<Int>
}

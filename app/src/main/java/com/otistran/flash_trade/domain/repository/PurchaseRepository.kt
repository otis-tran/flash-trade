package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import com.otistran.flash_trade.domain.model.Purchase
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {
    suspend fun insertPurchase(purchase: Purchase)
    suspend fun updatePurchase(purchase: Purchase)
    suspend fun getPurchaseByTxHash(txHash: String): Purchase?
    fun observePurchasesByStatus(status: PurchaseStatus): Flow<List<Purchase>>
    fun observePurchasesByStatuses(statuses: List<PurchaseStatus>): Flow<List<Purchase>>
    fun observeAllPurchases(): Flow<List<Purchase>>
    /** Observe all purchases for a specific wallet address */
    fun observeAllPurchasesByWallet(walletAddress: String): Flow<List<Purchase>>
    /** Observe active purchases (HELD, SELLING, RETRYING) for countdown displays */
    fun observeActivePurchases(): Flow<List<Purchase>>
    suspend fun getPendingAutoSells(currentTime: Long): List<Purchase>
    suspend fun updatePurchaseStatus(txHash: String, status: PurchaseStatus)
    suspend fun updatePurchaseSold(txHash: String, status: PurchaseStatus, sellTxHash: String)
    suspend fun updateWorkerId(txHash: String, workerId: String)
    fun observeHeldCount(): Flow<Int>
}


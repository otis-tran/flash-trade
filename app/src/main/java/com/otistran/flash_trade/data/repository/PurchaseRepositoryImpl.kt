package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.local.database.dao.PurchaseDao
import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import com.otistran.flash_trade.data.local.mapper.toDomain
import com.otistran.flash_trade.data.local.mapper.toEntity
import com.otistran.flash_trade.domain.model.Purchase
import com.otistran.flash_trade.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PurchaseRepositoryImpl @Inject constructor(
    private val purchaseDao: PurchaseDao
) : PurchaseRepository {

    override suspend fun insertPurchase(purchase: Purchase) {
        purchaseDao.insert(purchase.toEntity())
    }

    override suspend fun updatePurchase(purchase: Purchase) {
        purchaseDao.update(purchase.toEntity())
    }

    override suspend fun getPurchaseByTxHash(txHash: String): Purchase? {
        return purchaseDao.getByTxHash(txHash)?.toDomain()
    }

    override fun observePurchasesByStatus(status: PurchaseStatus): Flow<List<Purchase>> {
        return purchaseDao.observeByStatus(status).map { list -> list.map { it.toDomain() } }
    }

    override fun observePurchasesByStatuses(statuses: List<PurchaseStatus>): Flow<List<Purchase>> {
        return purchaseDao.observeByStatuses(statuses).map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllPurchases(): Flow<List<Purchase>> {
        return purchaseDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllPurchasesByWallet(walletAddress: String): Flow<List<Purchase>> {
        return purchaseDao.observeAllByWallet(walletAddress).map { list -> list.map { it.toDomain() } }
    }

    override fun observeActivePurchases(): Flow<List<Purchase>> {
        return observePurchasesByStatuses(
            listOf(PurchaseStatus.HELD, PurchaseStatus.SELLING, PurchaseStatus.RETRYING)
        )
    }

    override suspend fun getPendingAutoSells(currentTime: Long): List<Purchase> {
        return purchaseDao.getPendingAutoSells(currentTime).map { it.toDomain() }
    }

    override suspend fun updatePurchaseStatus(txHash: String, status: PurchaseStatus) {
        purchaseDao.updateStatus(txHash, status)
    }

    override suspend fun updatePurchaseSold(txHash: String, status: PurchaseStatus, sellTxHash: String) {
        purchaseDao.updateSold(txHash, status, sellTxHash)
    }

    override suspend fun updateWorkerId(txHash: String, workerId: String) {
        purchaseDao.updateWorkerId(txHash, workerId)
    }

    override fun observeHeldCount(): Flow<Int> {
        return purchaseDao.observeHeldCount()
    }
}

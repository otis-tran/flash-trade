package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import com.otistran.flash_trade.domain.model.Purchase
import com.otistran.flash_trade.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Get all purchases as a Flow.
 */
class GetPurchasesUseCase @Inject constructor(
    private val purchaseRepository: PurchaseRepository
) {

    operator fun invoke(): Flow<List<Purchase>> {
        return purchaseRepository.observeAllPurchases()
    }

    fun heldOnly(): Flow<List<Purchase>> {
        return purchaseRepository.observePurchasesByStatus(PurchaseStatus.HELD)
    }

    fun heldCount(): Flow<Int> {
        return purchaseRepository.observeHeldCount()
    }
}

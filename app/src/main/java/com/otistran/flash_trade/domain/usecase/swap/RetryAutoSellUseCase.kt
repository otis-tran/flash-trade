package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import com.otistran.flash_trade.domain.repository.PurchaseRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Retry a failed auto-sell by resetting status and rescheduling.
 */
class RetryAutoSellUseCase @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val scheduleAutoSellUseCase: ScheduleAutoSellUseCase
) {
    suspend operator fun invoke(txHash: String) {
        Timber.i("Retrying auto-sell for $txHash")
        
        // Reset status to HELD
        purchaseRepository.updatePurchaseStatus(txHash, PurchaseStatus.HELD)
        
        // Schedule immediately (0 delay)
        scheduleAutoSellUseCase(txHash, delayMinutes = 0)
    }
}

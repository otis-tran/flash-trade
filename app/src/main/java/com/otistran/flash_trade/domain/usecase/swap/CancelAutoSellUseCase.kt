package com.otistran.flash_trade.domain.usecase.swap

import androidx.work.WorkManager
import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import com.otistran.flash_trade.domain.repository.PurchaseRepository
import com.otistran.flash_trade.util.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * Cancel pending auto-sell for a purchase.
 */
class CancelAutoSellUseCase @Inject constructor(
    private val workManager: WorkManager,
    private val purchaseRepository: PurchaseRepository
) {

    suspend operator fun invoke(txHash: String): Result<Unit> {
        return try {
            val purchase = purchaseRepository.getPurchaseByTxHash(txHash)
                ?: return Result.Error("Purchase not found")

            if (!purchase.canCancel) {
                return Result.Error("Cannot cancel: status is ${purchase.status}")
            }

            // Cancel WorkManager job
            workManager.cancelUniqueWork("auto_sell_$txHash")

            // Update status to CANCELLED
            purchaseRepository.updatePurchaseStatus(txHash, PurchaseStatus.CANCELLED)

            Timber.i("Cancelled auto-sell for $txHash")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e("Failed to cancel auto-sell", e)
            Result.Error(e.message ?: "Failed to cancel")
        }
    }
}

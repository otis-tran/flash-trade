package com.otistran.flash_trade.domain.usecase.swap

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.otistran.flash_trade.data.work.AutoSellWorker
import com.otistran.flash_trade.domain.repository.PurchaseRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val AUTO_SELL_DELAY_HOURS = 24L

/**
 * Schedule auto-sell worker for a purchase.
 */
class ScheduleAutoSellUseCase @Inject constructor(
    private val workManager: WorkManager,
    private val purchaseRepository: PurchaseRepository
) {

    suspend operator fun invoke(txHash: String): String {
        val workRequest = OneTimeWorkRequestBuilder<AutoSellWorker>()
            .setInitialDelay(AUTO_SELL_DELAY_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15, TimeUnit.MINUTES
            )
            .setInputData(
                workDataOf(AutoSellWorker.KEY_TX_HASH to txHash)
            )
            .addTag("auto_sell")
            .addTag("purchase:$txHash")
            .build()

        workManager.enqueueUniqueWork(
            "auto_sell_$txHash",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        val workerId = workRequest.id.toString()

        // Update purchase with worker ID
        purchaseRepository.updateWorkerId(txHash, workerId)

        Timber.i("Scheduled auto-sell for $txHash, workerId=$workerId")
        return workerId
    }
}

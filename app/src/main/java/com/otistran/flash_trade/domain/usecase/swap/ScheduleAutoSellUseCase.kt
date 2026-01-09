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
import com.otistran.flash_trade.domain.repository.SettingsRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Schedule auto-sell worker for a purchase.
 * Reads duration from user settings.
 */
class ScheduleAutoSellUseCase @Inject constructor(
    private val workManager: WorkManager,
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository
) {

    /**
     * Schedule auto-sell with duration from settings.
     * @param txHash Purchase transaction hash
     * @param delayMinutes Optional override for delay (0 = immediate for retry)
     */
    suspend operator fun invoke(txHash: String, delayMinutes: Int? = null): String {
        val duration = delayMinutes ?: settingsRepository.getAutoSellDurationMinutes()
        
        val workRequest = OneTimeWorkRequestBuilder<AutoSellWorker>()
            .setInitialDelay(duration.toLong(), TimeUnit.MINUTES)
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

        Timber.i("Scheduled auto-sell for $txHash in ${duration}min, workerId=$workerId")
        return workerId
    }
}

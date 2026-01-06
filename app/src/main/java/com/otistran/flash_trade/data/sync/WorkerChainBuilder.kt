package com.otistran.flash_trade.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.otistran.flash_trade.data.work.TokenSyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Builds chained WorkManager work requests for token sync.
 * Extracted from TokenSyncManagerImpl to improve modularity.
 */
class WorkerChainBuilder @Inject constructor() {

    companion object {
        private const val SYNC_WORK_NAME = "token_sync_work"
    }

    /**
     * Enqueue a chain of batch workers for syncing pages.
     */
    fun enqueueBatchChain(
        workManager: WorkManager,
        startPage: Int,
        endPage: Int,
        pagesPerBatch: Int,
        generation: Int
    ) {
        // Start a unique work chain with placeholder
        var continuation = workManager.beginUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            createPlaceholderWorker()
        )

        // Split into batches
        var currentStart = startPage
        while (currentStart <= endPage) {
            val currentEnd = minOf(currentStart + pagesPerBatch - 1, endPage)

            continuation = continuation.then(
                createBatchWorker(currentStart, currentEnd, generation)
            )

            currentStart = currentEnd + 1
        }

        continuation.enqueue()
    }

    /**
     * Create a placeholder worker that marks immediate fetch as complete.
     */
    private fun createPlaceholderWorker(): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<TokenSyncWorker>()
            .setInputData(
                workDataOf(
                    TokenSyncWorker.START_PAGE_KEY to 0,
                    TokenSyncWorker.END_PAGE_KEY to 0,
                    TokenSyncWorker.GENERATION_KEY to 0
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()
    }

    /**
     * Create a batch worker for a range of pages.
     */
    private fun createBatchWorker(
        startPage: Int,
        endPage: Int,
        generation: Int
    ): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<TokenSyncWorker>()
            .setInputData(
                workDataOf(
                    TokenSyncWorker.START_PAGE_KEY to startPage,
                    TokenSyncWorker.END_PAGE_KEY to endPage,
                    TokenSyncWorker.GENERATION_KEY to generation
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresCharging(false)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()
    }
}

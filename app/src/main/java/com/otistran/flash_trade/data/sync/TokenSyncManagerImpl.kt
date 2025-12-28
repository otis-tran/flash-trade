package com.otistran.flash_trade.data.sync

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.otistran.flash_trade.data.datastore.SyncCheckpointDataStore
import com.otistran.flash_trade.data.datastore.SyncPreferences
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.data.work.TokenSyncWorker
import com.otistran.flash_trade.domain.model.SyncState
import com.otistran.flash_trade.domain.sync.SyncProgress
import com.otistran.flash_trade.domain.sync.TokenSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TokenSyncManager using WorkManager for background sync.
 *
 * Architecture:
 * - Phase 1: Immediate fetch of top 50 pages via coroutines (< 2s launch)
 * - Phase 2: WorkManager handles remaining pages (51-3218) via batched workers
 * - Survives app backgrounding (Privy login) and app termination
 */
@Singleton
class TokenSyncManagerImpl @Inject constructor(
    private val kyberApi: KyberApiService,
    private val tokenDao: TokenDao,
    private val syncPreferences: SyncPreferences,
    private val workManager: WorkManager,
    private val syncCheckpoint: SyncCheckpointDataStore
) : TokenSyncManager {

    companion object {
        private const val TAG = "TokenSyncManager"

        // API configuration
        private const val MIN_TVL_THRESHOLD = 10_000.0
        private const val PAGE_SIZE = 100
        private const val CACHE_TTL_MS = 60 * 60 * 1000L  // 1 hour

        // Progressive sync configuration
        private const val IMMEDIATE_PAGE_COUNT = 50  // Top 50 pages = 5K tokens
        private const val PAGES_PER_BATCH = 100       // Pages per worker batch
        private const val IMMEDIATE_FETCH_TIMEOUT_MS = 5000L  // Max wait for immediate fetch

        // Work identifiers
        private const val SYNC_WORK_NAME = "token_sync_work"
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Scope for immediate fetch operations
    private val immediateFetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Note: We don't observe WorkManager state in init since we don't have LifecycleOwner.
    // Sync state is updated when sync is enqueued and can be polled via getSyncProgress().

    // ==================== Public Interface ====================

    override suspend fun shouldSync(): Boolean {
        val lastSync = syncPreferences.getLastSyncTime()
        val tokenCount = tokenDao.getTokenCount()

        return when {
            tokenCount == 0 -> {
                Log.d(TAG, "shouldSync: true (DB empty)")
                true
            }
            System.currentTimeMillis() - lastSync > CACHE_TTL_MS -> {
                Log.d(TAG, "shouldSync: true (cache stale)")
                true
            }
            else -> {
                Log.d(TAG, "shouldSync: false (cache fresh)")
                false
            }
        }
    }

    override suspend fun checkAndStartSync() {
        if (shouldSync()) {
            enqueueBackgroundSync()
        }
    }

    override suspend fun forceSync() {
        Log.d(TAG, "Force sync requested")

        // Cancel existing work
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        syncCheckpoint.reset()

        // Start fresh sync
        enqueueBackgroundSync()
    }

    // ==================== WorkManager Methods ====================

    override suspend fun enqueueBackgroundSync() {
        Log.d(TAG, "Enqueueing background sync")

        // Increment generation for this sync session
        val generation = syncPreferences.incrementGeneration()
        Log.d(TAG, "Sync generation: $generation")

        // Phase 1: Immediate fetch of top 50 pages (parallel)
        _syncState.value = SyncState.Syncing(
            currentPage = 1,
            totalPages = 3218,
            tokensFetched = 0
        )

        try {
            fetchImmediatePages(generation)
            Log.d(TAG, "Immediate fetch complete: ${tokenDao.getTokenCount()} tokens cached")
        } catch (e: Exception) {
            Log.w(TAG, "Immediate fetch failed, continuing with WorkManager", e)
        }

        // Phase 2: Enqueue WorkManager for remaining pages (51-3218)
        enqueueBatchWorkers(startPage = 51, endPage = 3218, generation = generation)
    }

    override suspend fun cancelBackgroundSync() {
        Log.d(TAG, "Canceling background sync")
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        _syncState.value = SyncState.Idle
    }

    override suspend fun getSyncProgress(): SyncProgress? {
        val workInfos = workManager.getWorkInfosForUniqueWork(SYNC_WORK_NAME).get()
        val workInfo = workInfos.firstOrNull()

        return SyncProgress(
            lastPageSynced = syncCheckpoint.getLastPageSynced(),
            totalPages = syncCheckpoint.getTotalPages(),
            tokensCached = tokenDao.getTokenCount(),
            isRunning = workInfo?.state == WorkInfo.State.RUNNING
        )
    }

    // ==================== Private Methods ====================

    /**
     * Phase 1: Immediate fetch of top 50 pages using parallel coroutines.
     * Completes within ~2 seconds to provide fast app launch experience.
     */
    private suspend fun fetchImmediatePages(generation: Int) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching immediate pages (1-$IMMEDIATE_PAGE_COUNT) in parallel")

        // Fetch all 50 pages in parallel
        val fetchJobs = (1..IMMEDIATE_PAGE_COUNT).map { page ->
            async {
                try {
                    val response = kyberApi.getTokens(
                        minTvl = MIN_TVL_THRESHOLD,
                        sort = "tvl_desc",
                        page = page,
                        limit = PAGE_SIZE
                    )

                    // Convert and insert immediately
                    val entities = response.data
                        .filter { isValidToken(it) }
                        .map { dto ->
                            // Using the extension function defined in TokenMapper
                            with(com.otistran.flash_trade.data.mapper.TokenMapper) {
                                dto.toEntity(generation)
                            }
                        }

                    if (entities.isNotEmpty()) {
                        tokenDao.upsertTokens(entities)
                    }

                    entities.size
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch page $page: ${e.message}")
                    0
                }
            }
        }

        // Wait for all pages to complete (with timeout)
        try {
            withTimeout(IMMEDIATE_FETCH_TIMEOUT_MS) {
                val results = fetchJobs.awaitAll()
                val totalFetched = results.sum()
                Log.d(TAG, "Immediate fetch complete: $totalFetched tokens fetched")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Immediate fetch timeout, continuing with available data")
        }
    }

    /**
     * Phase 2: Enqueue WorkManager batch workers for remaining pages.
     * Creates a chain of workers, each processing 100 pages.
     */
    private fun enqueueBatchWorkers(startPage: Int, endPage: Int, generation: Int) {
        Log.d(TAG, "Enqueuing batch workers: pages $startPage-$endPage")

        // Start a unique work chain
        var continuation = workManager.beginUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            createImmediateWorkerPlaceholder() // Placeholder for completed immediate fetch
        )

        // Split into batches of 100 pages each
        var currentStart = startPage
        while (currentStart <= endPage) {
            val currentEnd = minOf(currentStart + PAGES_PER_BATCH - 1, endPage)

            continuation = continuation.then(
                createBatchWorker(currentStart, currentEnd, generation)
            )

            currentStart = currentEnd + 1
        }

        continuation.enqueue()
        Log.d(TAG, "WorkManager enqueue complete")
    }

    /**
     * Create a placeholder worker that marks immediate fetch as complete.
     * This ensures the work chain starts properly.
     */
    private fun createImmediateWorkerPlaceholder(): androidx.work.OneTimeWorkRequest {
        // Use TokenSyncWorker with no-op parameters
        return OneTimeWorkRequestBuilder<TokenSyncWorker>()
            .setInputData(
                androidx.work.workDataOf(
                    TokenSyncWorker.START_PAGE_KEY to 0,  // Special value: no-op
                    TokenSyncWorker.END_PAGE_KEY to 0,
                    TokenSyncWorker.GENERATION_KEY to 0
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)  // Allow even if battery low
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
    ): androidx.work.OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<TokenSyncWorker>()
            .setInputData(
                androidx.work.workDataOf(
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
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()
    }

    /**
     * Validate token data before processing.
     */
    private fun isValidToken(dto: com.otistran.flash_trade.data.remote.dto.kyber.TokenDto): Boolean {
        return !dto.name.isNullOrBlank() && !dto.symbol.isNullOrBlank()
    }
}

package com.otistran.flash_trade.data.sync

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.otistran.flash_trade.data.datastore.SyncCheckpointDataStore
import com.otistran.flash_trade.data.datastore.SyncPreferences
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.local.entity.TokenEntity
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.domain.model.SyncState
import com.otistran.flash_trade.domain.sync.SyncProgress
import com.otistran.flash_trade.domain.sync.TokenSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TokenSyncManager using WorkManager for background sync.
 *
 * Architecture:
 * - Phase 1: Immediate fetch of top 10 pages via coroutines (< 2s launch)
 * - Phase 2: WorkManager handles remaining pages (11-20) via batched workers
 * - Survives app backgrounding (Privy login) and app termination
 */
@Singleton
class TokenSyncManagerImpl @Inject constructor(
    private val kyberApi: KyberApiService,
    private val tokenDao: TokenDao,
    private val syncPreferences: SyncPreferences,
    private val workManager: WorkManager,
    private val syncCheckpoint: SyncCheckpointDataStore,
    private val workerChainBuilder: WorkerChainBuilder
) : TokenSyncManager {

    companion object {
        // API configuration
        private const val MIN_TVL_THRESHOLD = 10_000.0
        private const val PAGE_SIZE = 100
        private const val CACHE_TTL_MS = 60 * 60 * 1000L  // 1 hour

        // Progressive sync configuration
        private const val IMMEDIATE_PAGE_COUNT = 10   // Top 10 pages = 1K tokens
        private const val MAX_PREFETCH_PAGES = 20     // Maximum pages to cache total
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
                Timber.d("shouldSync: true (DB empty)")
                true
            }
            System.currentTimeMillis() - lastSync > CACHE_TTL_MS -> {
                Timber.d("shouldSync: true (cache stale)")
                true
            }
            else -> {
                Timber.d("shouldSync: false (cache fresh)")
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
        Timber.d("Force sync requested")

        // Cancel existing work
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        syncCheckpoint.reset()

        // Start fresh sync
        enqueueBackgroundSync()
    }

    // ==================== WorkManager Methods ====================

    override suspend fun enqueueBackgroundSync() {
        Timber.d("Enqueueing background sync")

        // Increment generation for this sync session
        val generation = syncPreferences.incrementGeneration()
        Timber.d("Sync generation: $generation")

        // Phase 1: Immediate fetch of top 10 pages (parallel)
        _syncState.value = SyncState.Syncing(
            currentPage = 1,
            totalPages = IMMEDIATE_PAGE_COUNT,
            tokensFetched = 0
        )

        try {
            fetchImmediatePages(generation)
            Timber.d("Immediate fetch complete: ${tokenDao.getTokenCount()} tokens cached")
        } catch (e: Exception) {
            Timber.w("Immediate fetch failed, continuing with WorkManager", e)
        }

        // Phase 2: Enqueue WorkManager for remaining pages (11-20)
        enqueueBatchWorkers(
            startPage = IMMEDIATE_PAGE_COUNT + 1,
            endPage = MAX_PREFETCH_PAGES,
            generation = generation
        )
    }

    override suspend fun cancelBackgroundSync() {
        Timber.d("Canceling background sync")
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
     * Phase 1: Immediate fetch of top 10 pages using sequential coroutines.
     * Completes within ~2 seconds to provide fast app launch experience.
     */
    private suspend fun fetchImmediatePages(generation: Int) = withContext(Dispatchers.IO) {
        Timber.d("Fetching immediate pages (1-$IMMEDIATE_PAGE_COUNT) sequentially")

        val allTokens = mutableListOf<TokenEntity>()

        // Sequential fetch to avoid overwhelming the API
        for (page in 1..IMMEDIATE_PAGE_COUNT) {
            try {
                val response = kyberApi.getTokens(
                    minTvl = MIN_TVL_THRESHOLD,
                    sort = "tvl_desc",
                    page = page,
                    limit = PAGE_SIZE
                )

                val entities = response.data
                    .filter { isValidToken(it) }
                    .map { dto ->
                        // Using the extension function defined in TokenMapper
                        with(com.otistran.flash_trade.data.mapper.TokenMapper) {
                            dto.toEntity(generation)
                        }
                    }

                if (entities.isEmpty()) {
                    Timber.d("Page $page is empty, stopping fetch")
                    break
                }

                allTokens.addAll(entities)
                Timber.v("Page $page: ${entities.size} tokens")

                // Small delay between requests to avoid rate limiting
                if (page < IMMEDIATE_PAGE_COUNT) {
                    delay(100)
                }

            } catch (e: Exception) {
                Timber.w("Failed to fetch page $page: ${e.message}")
                // Continue with next page instead of failing completely
            }
        }

        // Single bulk insert at the end
        if (allTokens.isNotEmpty()) {
            tokenDao.upsertTokens(allTokens)
            syncPreferences.updateLastSyncTime()
            Timber.d("Immediate fetch complete: ${allTokens.size} tokens cached")
        }
    }

    /**
     * Phase 2: Enqueue WorkManager batch workers for remaining pages.
     */
    private fun enqueueBatchWorkers(startPage: Int, endPage: Int, generation: Int) {
        Timber.d("Enqueuing batch workers: pages $startPage-$endPage")
        workerChainBuilder.enqueueBatchChain(
            workManager = workManager,
            startPage = startPage,
            endPage = endPage,
            pagesPerBatch = PAGES_PER_BATCH,
            generation = generation
        )
        Timber.d("WorkManager enqueue complete")
    }

    /**
     * Validate token data before processing.
     */
    private fun isValidToken(dto: com.otistran.flash_trade.data.remote.dto.kyber.TokenDto): Boolean {
        return !dto.name.isNullOrBlank() && !dto.symbol.isNullOrBlank()
    }
}

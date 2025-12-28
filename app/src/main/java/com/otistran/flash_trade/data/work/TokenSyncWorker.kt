package com.otistran.flash_trade.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.otistran.flash_trade.data.datastore.SyncCheckpointDataStore
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.remote.api.KyberApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for syncing token data in batches.
 *
 * Each worker processes a batch of pages (default 100 pages) to stay within
 * the 10-minute execution limit while maximizing throughput.
 *
 * Survives app backgrounding (e.g., during Privy login) and app termination.
 */
@HiltWorker
class TokenSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val kyberApi: KyberApiService,
    private val tokenDao: TokenDao,
    private val syncCheckpoint: SyncCheckpointDataStore
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TokenSyncWorker"

        const val START_PAGE_KEY = "start_page"
        const val END_PAGE_KEY = "end_page"
        const val GENERATION_KEY = "generation"

        // API configuration
        private const val MIN_TVL_THRESHOLD = 10_000.0
        private const val PAGE_SIZE = 100

        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L

        // Rate limiting
        private const val REQUEST_DELAY_MS = 100L
    }

    override suspend fun doWork(): Result {
        val startPage = inputData.getInt(START_PAGE_KEY, 1)
        val endPage = inputData.getInt(END_PAGE_KEY, startPage + 100)
        val generation = inputData.getInt(GENERATION_KEY, 1)

        Log.d(TAG, "Starting batch sync: pages $startPage-$endPage, generation $generation")

        return try {
            syncBatch(startPage, endPage, generation)

            // Save checkpoint after successful batch
            syncCheckpoint.saveProgress(endPage)
            Log.d(TAG, "Batch complete: saved checkpoint at page $endPage")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Batch failed: pages $startPage-$endPage", e)

            // Retry on transient failures
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Sync a batch of pages from the API.
     * Collects all tokens and performs a single bulk insert for efficiency.
     */
    private suspend fun syncBatch(startPage: Int, endPage: Int, generation: Int) {
        val allTokens = mutableListOf<com.otistran.flash_trade.data.local.entity.TokenEntity>()

        for (page in startPage..endPage) {
            val response = fetchPageWithRetry(page)

            if (response == null) {
                Log.w(TAG, "Skipping page $page after max retries")
                continue
            }

            if (response.data.isEmpty()) {
                Log.d(TAG, "Empty response at page $page - end of data")
                break
            }

            // Filter valid tokens and convert to entities with generation tracking
            val entities = response.data
                .filter { isValidToken(it) }
                .map { dto ->
                    // Using the extension function defined in TokenMapper
                    with(com.otistran.flash_trade.data.mapper.TokenMapper) {
                        dto.toEntity(generation)
                    }
                }

            allTokens.addAll(entities)
            Log.v(TAG, "Page $page: ${entities.size} tokens")

            // Rate limiting - delay between requests
            if (page < endPage) {
                kotlinx.coroutines.delay(REQUEST_DELAY_MS)
            }
        }

        // Single bulk insert for the entire batch
        if (allTokens.isNotEmpty()) {
            tokenDao.upsertTokens(allTokens)
            Log.i(TAG, "Batch complete: ${allTokens.size} tokens inserted")
        } else {
            Log.w(TAG, "Batch complete: no tokens to insert")
        }
    }

    /**
     * Fetch a page with retry logic.
     * Returns null if all retries are exhausted.
     */
    private suspend fun fetchPageWithRetry(page: Int): com.otistran.flash_trade.data.remote.dto.kyber.TokenListResponse? {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                return kyberApi.getTokens(
                    minTvl = MIN_TVL_THRESHOLD,
                    sort = "tvl_desc",
                    page = page,
                    limit = PAGE_SIZE
                )
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Fetch page $page failed (attempt ${attempt + 1}/$MAX_RETRIES): ${e.message}")

                if (attempt < MAX_RETRIES - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }

        Log.e(TAG, "Failed to fetch page $page after $MAX_RETRIES retries", lastException)
        return null
    }

    /**
     * Validate token data before insertion.
     */
    private fun isValidToken(dto: com.otistran.flash_trade.data.remote.dto.kyber.TokenDto): Boolean {
        return !dto.name.isNullOrBlank() && !dto.symbol.isNullOrBlank()
    }
}

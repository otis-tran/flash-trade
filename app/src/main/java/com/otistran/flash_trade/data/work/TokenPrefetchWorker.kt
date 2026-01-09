package com.otistran.flash_trade.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.mapper.TokenMapper.toEntityList
import com.otistran.flash_trade.data.remote.api.KyberApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * WorkManager worker for background token prefetch.
 * Fetches 20 pages (2,000 tokens) with rate limiting.
 * Uses Semaphore to limit concurrent requests and avoid 504 errors.
 */
@HiltWorker
class TokenPrefetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tokenDao: TokenDao,
    private val kyberApi: KyberApiService
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "token_prefetch_work"
        private const val PAGE_SIZE = 100
        private const val PREFETCH_PAGES = 20  // 2,000 tokens
        private const val MAX_CONCURRENT_REQUESTS = 4  // Limit concurrent API calls
        private const val DELAY_BETWEEN_REQUESTS_MS = 100L  // Small delay to avoid burst
        private const val MAX_RETRIES = 2
    }

    override suspend fun doWork(): Result {
        Timber.i("[TokenPrefetch] Starting prefetch, attempt ${runAttemptCount + 1}")
        
        // Check if we already have enough tokens
        val currentCount = tokenDao.getTokenCount()
        if (currentCount >= PREFETCH_PAGES * PAGE_SIZE) {
            Timber.i("[TokenPrefetch] Already have $currentCount tokens, skipping")
            return Result.success()
        }

        return try {
            val startTime = System.currentTimeMillis()
            val totalTokens = AtomicInteger(0)
            val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

            withContext(Dispatchers.IO) {
                coroutineScope {
                    // Fetch all pages with controlled concurrency
                    (1..PREFETCH_PAGES).map { page ->
                        launch {
                            semaphore.withPermit {
                                delay(DELAY_BETWEEN_REQUESTS_MS) // Small delay to avoid burst
                                fetchPageWithRetry(page, totalTokens)
                            }
                        }
                    }
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            Timber.i("[TokenPrefetch] Complete: ${totalTokens.get()} tokens in ${elapsed}ms")
            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "[TokenPrefetch] Failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Fetch a single page with retry and exponential backoff
     */
    private suspend fun fetchPageWithRetry(page: Int, totalTokens: AtomicInteger) {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                val response = kyberApi.getTokens(page = page, limit = PAGE_SIZE)
                val tokens = response.data.toEntityList()
                if (tokens.isNotEmpty()) {
                    tokenDao.upsertTokens(tokens)
                    totalTokens.addAndGet(tokens.size)
                    Timber.d("[TokenPrefetch] Page $page: +${tokens.size} tokens")
                }
                return // Success, exit retry loop
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES) {
                    // Exponential backoff: 1s, 2s
                    val backoffMs = (1000L * (attempt + 1))
                    Timber.w("[TokenPrefetch] Page $page failed (attempt ${attempt + 1}), retrying in ${backoffMs}ms")
                    delay(backoffMs)
                }
            }
        }
        
        // All retries exhausted
        Timber.w(lastException, "[TokenPrefetch] Failed page $page after ${MAX_RETRIES + 1} attempts")
    }
}

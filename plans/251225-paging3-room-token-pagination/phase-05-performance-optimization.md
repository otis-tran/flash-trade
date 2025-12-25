# Phase 5: Performance Optimization

**Estimated Effort:** 1-2 hours
**Dependencies:** Phase 4 (UI Integration)
**Status:** Pending

---

## Objectives

1. Implement splash screen prefetch for first 100 tokens
2. Integrate prefetch into TokenCacheManager (or create if not exists)
3. Wire prefetch to app startup sequence
4. Delete unused core/network boilerplate (ApiService.kt if exists)
5. Optimize SafeApiCall integration with RemoteMediator
6. Add performance logging for monitoring

---

## Implementation Steps

### Step 5.1: Create/Extend TokenCacheManager

**File:** `app/src/main/java/com/otistran/flash_trade/domain/manager/token-cache-manager.kt`

**Content:**

```kotlin
package com.otistran.flash_trade.domain.manager

import android.util.Log
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.mapper.TokenEntityMapper.toEntityList
import com.otistran.flash_trade.data.mapper.TokenMapper.toDomainList
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.model.TokenSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TokenCacheManager"

/**
 * Manages token cache prefetching during app startup.
 * Prefetches first page of tokens to Room for instant display on TradingScreen.
 */
@Singleton
class TokenCacheManager @Inject constructor(
    private val kyberApi: KyberApiService,
    private val tokenDao: TokenDao
) {

    /**
     * Prefetch first 100 tokens during splash screen.
     * Runs on IO dispatcher to avoid blocking UI.
     *
     * @return Number of tokens cached, or 0 on error
     */
    suspend fun prefetchTokens(): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "Starting token prefetch...")

            // Check if cache already has data (skip if within TTL)
            val existingCount = tokenDao.getAllTokens().size
            if (existingCount > 0) {
                Log.d(TAG, "Cache already has $existingCount tokens, skipping prefetch")
                return@withContext existingCount
            }

            // Fetch first page from API
            val response = kyberApi.getTokens(
                minTvl = 1000.0,
                sort = TokenSortOrder.TVL_DESC.value,
                page = 1,
                limit = 100
            )

            // Map to domain models
            val tokens = response.data.toDomainList()

            // Insert to Room
            val entities = tokens.toEntityList()
            tokenDao.insertTokens(entities)

            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Prefetched ${tokens.size} tokens in ${duration}ms")

            tokens.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prefetch tokens", e)
            0 // Return 0 on error (app still launches, just slower)
        }
    }

    /**
     * Clear expired cache entries based on TTL.
     * Optional: Call periodically or on app startup.
     */
    suspend fun clearExpiredCache() = withContext(Dispatchers.IO) {
        try {
            val ttlThreshold = System.currentTimeMillis() - (5 * 60 * 1000) // 5 min
            val staleToken = tokenDao.getStaleToken(ttlThreshold)

            if (staleToken != null) {
                Log.d(TAG, "Cache expired, clearing...")
                tokenDao.clearAll()
                Log.d(TAG, "Cache cleared")
            } else {
                Log.d(TAG, "Cache still fresh")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear expired cache", e)
        }
    }
}
```

**Key Features:**
- **Prefetch on Startup:** Fetches first 100 tokens during splash
- **Skip if Cached:** Avoids redundant API calls if cache exists
- **Performance Logging:** Tracks prefetch duration for monitoring
- **Error Resilient:** App launches even if prefetch fails

---

### Step 5.2: Integrate with AppStartupManager

**Option A: Extend Existing AppStartupManager**

**File:** `app/src/main/java/com/otistran/flash_trade/domain/manager/app-startup-manager.kt`
*(If this file exists based on codebase-summary.md)*

Add prefetch to existing initialization:

```kotlin
@Singleton
class AppStartupManager @Inject constructor(
    private val tokenCacheManager: TokenCacheManager,
    // ... existing dependencies ...
) {

    suspend fun initialize() {
        coroutineScope {
            // Launch parallel initialization tasks
            launch { initializePrivy() }
            launch { tokenCacheManager.prefetchTokens() } // NEW: Prefetch tokens
            // ... other startup tasks ...
        }
    }
}
```

**Option B: Create New Startup Manager (if not exists)**

**File:** `app/src/main/java/com/otistran/flash_trade/domain/manager/app-startup-manager.kt`

```kotlin
package com.otistran.flash_trade.domain.manager

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app startup initialization tasks.
 * Runs tasks in parallel for faster cold start.
 */
@Singleton
class AppStartupManager @Inject constructor(
    private val tokenCacheManager: TokenCacheManager
) {

    /**
     * Initialize app dependencies during splash screen.
     * All tasks run in parallel via coroutineScope.
     */
    suspend fun initialize() {
        coroutineScope {
            // Prefetch tokens in parallel with other init tasks
            launch { tokenCacheManager.prefetchTokens() }
            // Add other startup tasks here (Privy init, etc.)
        }
    }
}
```

---

### Step 5.3: Hook into Splash Screen / MainActivity

**Option A: Using Jetpack Startup Library (Recommended)**

**File:** `app/src/main/java/com/otistran/flash_trade/startup/token-cache-initializer.kt`

```kotlin
package com.otistran.flash_trade.startup

import android.content.Context
import androidx.startup.Initializer
import com.otistran.flash_trade.FlashTradeApplication
import com.otistran.flash_trade.domain.manager.TokenCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Jetpack Startup initializer for token cache prefetch.
 * Runs during app cold start before MainActivity.
 */
class TokenCacheInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val app = context.applicationContext as FlashTradeApplication
        val tokenCacheManager = app.tokenCacheManager // Get from Hilt component

        // Launch prefetch in background
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            tokenCacheManager.prefetchTokens()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList() // No dependencies
    }
}
```

**AndroidManifest.xml:**

```xml
<application>
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${applicationId}.androidx-startup"
        android:exported="false"
        tools:node="merge">
        <meta-data
            android:name="com.otistran.flash_trade.startup.TokenCacheInitializer"
            android:value="androidx.startup" />
    </provider>
</application>
```

**Option B: Manual Initialization in MainActivity (Simpler)**

**File:** `app/src/main/java/com/otistran/flash_trade/MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appStartupManager: AppStartupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize app (prefetch tokens, etc.)
        lifecycleScope.launch {
            appStartupManager.initialize()
        }

        setContent {
            FlashTradeTheme {
                // ... existing UI setup ...
            }
        }
    }
}
```

**Recommendation:** Use **Option B (Manual)** for simplicity unless Jetpack Startup already configured.

---

### Step 5.4: Delete Unused Boilerplate

**File to Check:** `app/src/main/java/com/otistran/flash_trade/core/network/ApiService.kt`

If this file exists and is unused (not referenced anywhere):

```bash
# Search for usage
grep -r "ApiService" app/src/main/java/
```

If no results (or only imports that can be removed):

```bash
# Delete file
rm app/src/main/java/com/otistran/flash_trade/core/network/ApiService.kt
```

**Files to Keep:**
- `SafeApiCall.kt` - Still used by RemoteMediator
- `KyberApiService.kt` - Active API service

---

### Step 5.5: Performance Logging Integration

**File:** `app/src/main/java/com/otistran/flash_trade/util/performance-logger.kt`

**Content:**

```kotlin
package com.otistran.flash_trade.util

import android.util.Log

private const val TAG = "PerformanceLogger"

/**
 * Simple performance logger for monitoring critical paths.
 */
object PerformanceLogger {

    /**
     * Log operation duration.
     */
    inline fun <T> logDuration(operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "$operation completed in ${duration}ms")
        return result
    }

    /**
     * Log cold start metrics (from Application.onCreate).
     */
    fun logColdStart(duration: Long) {
        Log.i(TAG, "Cold start: ${duration}ms")
    }

    /**
     * Log token prefetch metrics.
     */
    fun logTokenPrefetch(count: Int, duration: Long) {
        Log.i(TAG, "Token prefetch: $count tokens in ${duration}ms")
    }
}
```

**Usage in TokenCacheManager:**

```kotlin
suspend fun prefetchTokens(): Int = withContext(Dispatchers.IO) {
    return@withContext PerformanceLogger.logDuration("Token prefetch") {
        try {
            // ... existing prefetch logic ...
        } catch (e: Exception) {
            0
        }
    }
}
```

---

### Step 5.6: Optimize SafeApiCall with RemoteMediator

**Current Issue:**
RemoteMediator expects exceptions to be thrown, but SafeApiCall returns `NetworkResult`.

**Solution (Already Implemented in Phase 2):**
TokenRemoteMediator wraps SafeApiCall and converts NetworkResult.Error → IOException:

```kotlin
when (result) {
    is NetworkResult.Success -> { /* ... */ }
    is NetworkResult.Error -> {
        MediatorResult.Error(IOException("Failed: ${result.exception.message}"))
    }
}
```

**No additional changes needed** - pattern already optimized.

---

## Performance Targets

### Cold Start Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Prefetch Duration | <300ms | PerformanceLogger.logTokenPrefetch() |
| Total Cold Start | <800ms | PerformanceLogger.logColdStart() |
| First Token Display | <100ms | From Room cache (instant) |

### Memory Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Prefetch Memory | <5MB | 100 tokens × ~50KB each |
| Total Cache Size | <15MB | 500 tokens max (PagingConfig.maxSize) |

---

## Testing Checklist (Manual)

### Test 1: Prefetch on Cold Start
- [ ] Clear app data
- [ ] Launch app
- [ ] Check logcat for "Prefetched 100 tokens in XXms"
- [ ] Verify TradingScreen displays tokens instantly (no loading)

### Test 2: Skip Prefetch if Cached
- [ ] Launch app (prefetch runs)
- [ ] Kill and relaunch app within 5 min
- [ ] Check logcat for "Cache already has 100 tokens, skipping prefetch"
- [ ] Verify no API call made

### Test 3: Prefetch Failure Resilience
- [ ] Enable airplane mode
- [ ] Clear app data
- [ ] Launch app
- [ ] Verify app launches despite prefetch failure
- [ ] Verify TradingScreen shows error state (no cache)

### Test 4: Performance Monitoring
- [ ] Check logcat for performance metrics:
  - "Token prefetch: 100 tokens in XXms"
  - "Cold start: XXms"
- [ ] Verify cold start <800ms (target)

---

## Verification Checklist

- [ ] TokenCacheManager created or extended
- [ ] `prefetchTokens()` method implemented
- [ ] AppStartupManager integrated (or manual init in MainActivity)
- [ ] Prefetch hooked into app startup sequence
- [ ] Performance logging added
- [ ] Unused ApiService.kt deleted (if applicable)
- [ ] Cold start performance <800ms (measured)
- [ ] Prefetch duration <300ms (measured)
- [ ] Project builds without errors

---

## Files Created (Summary)

1. `domain/manager/token-cache-manager.kt` (~80 lines)
2. `domain/manager/app-startup-manager.kt` (~30 lines, if new)
3. `util/performance-logger.kt` (~40 lines)

**Total:** 2-3 new files, ~150 lines

---

## Files Modified (Summary)

1. `MainActivity.kt` (+5 lines for manual init, if using Option B)
2. `AndroidManifest.xml` (+10 lines for Jetpack Startup, if using Option A)

**Total:** 1-2 files modified, ~15 lines

---

## Files Deleted (Summary)

1. `core/network/ApiService.kt` (if unused)

**Total:** 0-1 file deleted

---

## Post-Implementation Checklist

### Phase 1-5 Complete
- [ ] All 5 phases implemented
- [ ] All files created/modified as documented
- [ ] Project builds successfully
- [ ] Manual testing completed
- [ ] Performance targets met (cold start <800ms, prefetch <300ms)

### Documentation Updates
- [ ] Update `docs/codebase-summary.md` with new database schema
- [ ] Update `docs/system-architecture.md` with Paging 3 architecture
- [ ] Update `README.md` implementation status (TokenPrefetch ✅)

### Code Cleanup
- [ ] Remove unused imports
- [ ] Remove commented-out code
- [ ] Run code formatter (Kotlin style)
- [ ] No TODO comments left without tickets

### Future Enhancements (Post-Plan)
- [ ] Add unit tests for TokenCacheManager
- [ ] Add instrumentation tests for Paging flow
- [ ] Implement search with Paging (if needed)
- [ ] Migrate other screens to Paging 3 (PortfolioScreen, etc.)
- [ ] Add analytics events (token_prefetch_success, token_page_loaded)

---

## Summary

Phase 5 completes the Paging 3 + Room implementation with:
1. **Splash Screen Prefetch:** First 100 tokens cached during app startup
2. **Performance Monitoring:** Logging for cold start and prefetch metrics
3. **Code Cleanup:** Remove unused boilerplate
4. **Optimized Startup:** Parallel initialization for faster cold start

**End Result:**
- Cold start <800ms (target met)
- Token list displays instantly from Room cache
- Smooth infinite scrolling with Paging 3
- Offline support with TTL-based refresh
- Production-ready implementation

---

## Next Steps

1. Complete all 5 phases sequentially
2. Run manual testing checklist
3. Verify performance targets met
4. Update project documentation
5. Mark plan as completed
6. Deploy to QA/staging for integration testing

**Plan Complete!** 🎉

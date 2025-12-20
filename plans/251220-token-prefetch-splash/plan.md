# Token Prefetch During Splash Screen

**Status:** Ready for Implementation
**Priority:** High
**Complexity:** Low-Medium
**Date:** 2024-12-20

## Overview

Preload popular tokens and swap quotes during app startup (splash screen) for instant display when user reaches TradingScreen. This eliminates the loading spinner on TradingScreen by fetching data in parallel with auth check.

## Current Behavior

1. **MainActivity.checkAuthDuringSplash()** - Checks auth status, sets `isAppReady = true`
2. **TradingViewModel.init{}** - Calls `loadTokens()` on viewmodel creation
3. **User sees loading spinner** on TradingScreen while tokens fetch

## Target Behavior

1. **MainActivity** - Parallel prefetch: auth check + token fetch + quote prefetch
2. **TradingViewModel** - Checks cache first, skips API call if data exists
3. **User sees instant token list** when TradingScreen loads

## Architecture Decisions

### Option A: PrefetchManager (Recommended)
- New singleton class handles all prefetch logic
- Clean separation, testable, reusable
- MainActivity just calls `prefetchManager.prefetch()`

### Option B: Extend TokenRepository
- Add `prefetchPopularTokens()` method to existing repo
- Less overhead, but mixes concerns

**Decision:** Option A - PrefetchManager for clean architecture

## Implementation Phases

### Phase 1: Create PrefetchManager

**File:** `domain/manager/PrefetchManager.kt`

```kotlin
@Singleton
class PrefetchManager @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val quoteCacheManager: QuoteCacheManager,
    private val swapRepository: SwapRepository
) {
    private val _prefetchState = MutableStateFlow<PrefetchState>(PrefetchState.Idle)
    val prefetchState: StateFlow<PrefetchState> = _prefetchState.asStateFlow()

    suspend fun prefetch() {
        if (_prefetchState.value is PrefetchState.Loading) return

        _prefetchState.value = PrefetchState.Loading

        coroutineScope {
            // Parallel prefetch
            val tokenJob = async { prefetchTokens() }
            val quoteJob = async { prefetchQuotes() }

            tokenJob.await()
            quoteJob.await()
        }

        _prefetchState.value = PrefetchState.Completed
    }

    private suspend fun prefetchTokens() {
        tokenRepository.getTokens(TokenFilter(minTvl = 10000.0, limit = 50))
    }

    private suspend fun prefetchQuotes() {
        QuoteCacheManager.getPopularPairs().forEach { (tokenIn, tokenOut) ->
            try {
                swapRepository.getQuote(
                    chain = "base",
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = QuoteCacheManager.DEFAULT_PRELOAD_AMOUNT,
                    userAddress = null // No user address during splash
                )
            } catch (e: Exception) {
                // Silent fail - quotes are optional
            }
        }
    }
}

sealed class PrefetchState {
    object Idle : PrefetchState()
    object Loading : PrefetchState()
    object Completed : PrefetchState()
}
```

### Phase 2: Update MainActivity

**File:** `MainActivity.kt`

```kotlin
@Inject
lateinit var prefetchManager: PrefetchManager

private fun checkAuthDuringSplash() {
    lifecycleScope.launch {
        // Parallel execution: auth check + prefetch
        val authDeferred = async { checkLoginStatusUseCase() }
        val prefetchDeferred = async { prefetchManager.prefetch() }

        // Wait for both
        val authResult = authDeferred.await()
        prefetchDeferred.await() // Don't block on failure

        startDestination = when (authResult) {
            is Result.Success -> {
                if (authResult.data.isLoggedIn && authResult.data.isSessionValid) {
                    TradingGraph
                } else Login
            }
            else -> Login
        }
        isAppReady = true
    }
}
```

### Phase 3: Update TradingViewModel

**File:** `presentation/feature/trading/TradingViewModel.kt`

```kotlin
@HiltViewModel
class TradingViewModel @Inject constructor(
    private val getTokensUseCase: GetTokensUseCase,
    private val tokenRepository: TokenRepository // Add for cache check
) : BaseViewModel<...> {

    init {
        loadTokensIfNeeded()
    }

    private fun loadTokensIfNeeded() {
        viewModelScope.launch {
            // Check cache first via observeTokens()
            val cachedTokens = tokenRepository.observeTokens().first()

            if (cachedTokens.isNotEmpty()) {
                // Use cached data immediately
                setState {
                    copy(
                        isLoading = false,
                        tokens = cachedTokens,
                        hasMore = cachedTokens.size >= 50
                    )
                }
            } else {
                // No cache, fetch from API
                loadTokens()
            }
        }
    }
}
```

## Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `domain/manager/PrefetchManager.kt` | Create | Prefetch orchestrator |
| `MainActivity.kt` | Modify | Add parallel prefetch call |
| `TradingViewModel.kt` | Modify | Check cache before API |
| `di/AppModule.kt` | Modify | Provide PrefetchManager (if needed) |

## Testing Considerations

1. **Happy path:** Prefetch completes before auth → instant token display
2. **Slow prefetch:** Auth completes first → splash ends, tokens load normally
3. **Prefetch failure:** Silent fail → fallback to normal loading
4. **No network:** Cached tokens from previous session (if any)

## Edge Cases

- **First launch:** No cache, prefetch runs, TradingScreen shows loading briefly
- **App killed during prefetch:** No issue, cache not persisted to disk
- **Quote prefetch slow:** Tokens show immediately, quotes cache in background

## Success Metrics

- TradingScreen load time: ~2s → 0ms (cached)
- No visual regressions
- No splash screen duration increase (parallel execution)

## Dependencies

- Existing: `TokenRepository`, `QuoteCacheManager`, `SwapRepository`
- No new libraries needed

## Risks

| Risk | Mitigation |
|------|------------|
| Splash duration increase | Prefetch doesn't block auth; use `async` |
| Memory pressure | 50 tokens + 3 quotes minimal footprint |
| API rate limiting | Already throttled by Kyber SDK |

## Implementation Order

1. Create `PrefetchManager.kt` with token prefetch only
2. Update `MainActivity.kt` to call prefetch
3. Update `TradingViewModel.kt` to use cache
4. Test token prefetch flow
5. Add quote prefetch to `PrefetchManager`
6. Test complete flow
7. Update README status

## Notes

- Quote prefetch optional - can be added in Phase 2
- Consider persisting token cache to DataStore for offline support (future enhancement)
- Analytics: Track prefetch duration for monitoring

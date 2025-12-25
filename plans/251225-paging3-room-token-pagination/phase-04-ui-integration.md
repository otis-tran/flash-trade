# Phase 4: UI Integration

**Estimated Effort:** 1-2 hours
**Dependencies:** Phase 3 (Repository Integration)
**Status:** ✅ COMPLETE (2025-12-26)
**Review:** reports/251226-from-code-reviewer-to-orchestrator-phase-04-ui-integration-review.md

---

## Objectives

1. Create GetPagedTokensUseCase for domain layer encapsulation
2. Update TradingViewModel to expose `Flow<PagingData<Token>>`
3. Modify TradingScreen to use `collectAsLazyPagingItems()`
4. Implement LoadState handling for loading/error UI
5. Maintain existing UI patterns (MVI, Compose)

---

## Implementation Steps

### Step 4.1: Create GetPagedTokensUseCase

**File:** `app/src/main/java/com/otistran/flash_trade/domain/usecase/token/get-paged-tokens-use-case.kt`

**Content:**

```kotlin
package com.otistran.flash_trade.domain.usecase.token

import androidx.paging.PagingData
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting paginated token stream.
 * Encapsulates repository call for UI layer.
 */
class GetPagedTokensUseCase @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    /**
     * Execute use case to get paginated token stream.
     *
     * @param filter Token filter criteria (minTvl, sort, etc.)
     * @return Flow of PagingData for Compose UI
     */
    operator fun invoke(filter: TokenFilter = TokenFilter()): Flow<PagingData<Token>> {
        return tokenRepository.getPagedTokens(filter)
    }
}
```

**Rationale:**
- Follows existing project pattern (use cases in domain layer)
- `operator fun invoke()` enables clean syntax: `useCase(filter)`
- Pure delegation to repository (minimal logic)

---

### Step 4.2: Update TradingViewModel

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/trading-view-model.kt`
*(Exact path may vary - check actual location)*

**Imports to Add:**

```kotlin
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.otistran.flash_trade.domain.usecase.token.GetPagedTokensUseCase
import kotlinx.coroutines.flow.Flow
```

**ViewModel Changes:**

```kotlin
@HiltViewModel
class TradingViewModel @Inject constructor(
    private val getPagedTokensUseCase: GetPagedTokensUseCase, // NEW: Inject use case
    // ... existing use cases ...
) : ViewModel() { // Or MviContainer if using MVI base

    // ==================== NEW: Paging Flow ====================

    /**
     * Paginated token stream using Paging 3.
     * cachedIn(viewModelScope) ensures pagination survives config changes.
     */
    val pagingTokens: Flow<PagingData<Token>> = getPagedTokensUseCase(
        filter = TokenFilter(
            minTvl = 1000.0,
            sort = TokenSortOrder.TVL_DESC,
            limit = 100
        )
    ).cachedIn(viewModelScope) // CRITICAL: Cache across config changes

    // ==================== Existing State/Methods ====================

    // Keep existing state management (MVI intents, states, etc.)
    // This new flow coexists with existing logic

    // ... rest of ViewModel unchanged ...
}
```

**Key Points:**
- **`cachedIn(viewModelScope)`:** Caches PagingData across config changes (rotation, etc.)
  - **Without this:** Pagination state resets on rotation
  - **With this:** Scroll position and loaded pages preserved
- **Filter Configuration:** Adjust `TokenFilter` parameters as needed for UI
- **Backward Compatibility:** Existing ViewModel logic remains intact

---

### Step 4.3: Update TradingScreen Composable

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/trading-screen.kt`
*(Exact path may vary - check actual location)*

**Imports to Add:**

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
```

**Composable Changes:**

```kotlin
@Composable
fun TradingScreen(
    viewModel: TradingViewModel = hiltViewModel(),
    onNavigateToDetails: (Token) -> Unit
) {
    // Collect PagingData as LazyPagingItems
    val tokens: LazyPagingItems<Token> = viewModel.pagingTokens.collectAsLazyPagingItems()

    // Pull-to-refresh state
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = tokens.loadState.refresh is LoadState.Loading,
        onRefresh = { tokens.refresh() },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            // Initial loading state
            tokens.loadState.refresh is LoadState.Loading && tokens.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error state (initial load failed)
            tokens.loadState.refresh is LoadState.Error && tokens.itemCount == 0 -> {
                val error = (tokens.loadState.refresh as LoadState.Error).error
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${error.localizedMessage}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Success state (show list)
            else -> {
                TokenList(
                    tokens = tokens,
                    onTokenClick = onNavigateToDetails
                )
            }
        }
    }
}

@Composable
private fun TokenList(
    tokens: LazyPagingItems<Token>,
    onTokenClick: (Token) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = tokens.itemCount,
            key = tokens.itemKey { it.address } // Stable key for animations
        ) { index ->
            val token = tokens[index]
            if (token != null) {
                TokenCard(
                    token = token,
                    onClick = { onTokenClick(token) }
                )
            }
        }

        // Append loading indicator
        item {
            when {
                tokens.loadState.append is LoadState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                tokens.loadState.append is LoadState.Error -> {
                    val error = (tokens.loadState.append as LoadState.Error).error
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error loading more: ${error.localizedMessage}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenCard(
    token: Token,
    onClick: () -> Unit
) {
    // Existing TokenCard implementation (adjust as needed)
    // Use existing design patterns from project

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token logo (if using Coil)
            AsyncImage(
                model = token.logoUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = token.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "TVL: ${token.formattedTvl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Verified badge (if applicable)
            if (token.isVerified) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
```

**Key Implementation Details:**

1. **`collectAsLazyPagingItems()`:**
   - Converts `Flow<PagingData<Token>>` → `LazyPagingItems<Token>`
   - Manages lifecycle-aware collection

2. **LoadState Handling:**
   - `loadState.refresh`: Initial/refresh loading state
   - `loadState.append`: Loading next page state
   - `loadState.prepend`: Not used (forward-only pagination)

3. **Pull-to-Refresh:**
   - `tokens.refresh()` triggers RemoteMediator REFRESH
   - Clears cache and reloads from page 1

4. **Stable Keys:**
   - `itemKey { it.address }` provides stable keys for item animations
   - Improves performance and prevents recomposition issues

5. **Error Handling:**
   - Displays error message from `LoadState.Error`
   - Shows retry option (pull-to-refresh)

---

### Step 4.4: LoadState UI Patterns

**Loading States:**

```kotlin
// Initial load
if (tokens.loadState.refresh is LoadState.Loading && tokens.itemCount == 0) {
    // Show full-screen loading indicator
}

// Loading more (append)
if (tokens.loadState.append is LoadState.Loading) {
    // Show bottom loading indicator
}
```

**Error States:**

```kotlin
// Initial load error
if (tokens.loadState.refresh is LoadState.Error && tokens.itemCount == 0) {
    val error = (tokens.loadState.refresh as LoadState.Error).error
    // Show full-screen error with retry button
}

// Append error
if (tokens.loadState.append is LoadState.Error) {
    val error = (tokens.loadState.append as LoadState.Error).error
    // Show inline error at bottom
}
```

---

## Alternative: Using Existing Token List UI

If project already has a TokenList composable, adapt it:

```kotlin
@Composable
fun TradingScreen(viewModel: TradingViewModel = hiltViewModel()) {
    val tokens = viewModel.pagingTokens.collectAsLazyPagingItems()

    // Reuse existing TokenList composable with LazyPagingItems
    ExistingTokenList(
        tokens = tokens, // Pass LazyPagingItems instead of List<Token>
        onTokenClick = { /* ... */ }
    )
}
```

Modify `ExistingTokenList` to accept `LazyPagingItems` instead of `List`.

---

## Testing Checklist (Manual)

### Test 1: Initial Load
- [ ] Launch app
- [ ] Verify loading indicator shows
- [ ] Verify first 200 tokens load
- [ ] Verify list displays correctly

### Test 2: Scroll Pagination
- [ ] Scroll to bottom
- [ ] Verify loading indicator appears at bottom
- [ ] Verify next 100 tokens load
- [ ] Continue scrolling → verify continuous loading

### Test 3: Pull-to-Refresh
- [ ] Pull down on list
- [ ] Verify refresh triggered
- [ ] Verify list updates (if API data changed)

### Test 4: Error Handling
- [ ] Enable airplane mode with empty cache
- [ ] Launch app
- [ ] Verify error message displays
- [ ] Re-enable network
- [ ] Pull to refresh
- [ ] Verify tokens load

### Test 5: Config Change (Rotation)
- [ ] Load tokens
- [ ] Scroll to middle of list
- [ ] Rotate device
- [ ] Verify scroll position maintained
- [ ] Verify no re-fetch (cached in ViewModel)

### Test 6: Offline Support
- [ ] Load tokens online
- [ ] Enable airplane mode
- [ ] Kill and reopen app
- [ ] Verify cached tokens display
- [ ] Scroll → verify all cached pages accessible

---

## Verification Checklist

- [ ] GetPagedTokensUseCase created
- [ ] TradingViewModel injects GetPagedTokensUseCase
- [ ] ViewModel exposes `pagingTokens: Flow<PagingData<Token>>`
- [ ] `cachedIn(viewModelScope)` applied to flow
- [ ] TradingScreen collects with `collectAsLazyPagingItems()`
- [ ] LazyColumn uses `items(count, key)` pattern
- [ ] LoadState handling for refresh/append states
- [ ] Pull-to-refresh integrated
- [ ] Error states display user-friendly messages
- [ ] Project builds without errors
- [ ] UI renders correctly in preview/emulator

---

## Performance Optimizations

### LazyColumn Key Strategy
```kotlin
items(
    count = tokens.itemCount,
    key = tokens.itemKey { it.address } // Stable key prevents recomposition
) { ... }
```

### Content Type (Optional)
For mixed item types (e.g., headers, ads):
```kotlin
items(
    count = tokens.itemCount,
    key = tokens.itemKey { it.address },
    contentType = { "token" } // Helps LazyColumn reuse compositions
) { ... }
```

---

## Files Created (Summary)

1. `domain/usecase/token/get-paged-tokens-use-case.kt` (~25 lines)

**Total:** 1 new file, ~25 lines

---

## Files Modified (Summary)

1. `presentation/feature/trading/trading-view-model.kt` (+15 lines)
2. `presentation/feature/trading/trading-screen.kt` (+100 lines for LoadState handling)

**Total:** 2 files modified, ~115 lines added

---

## Next Phase

Proceed to **Phase 5: Performance Optimization** for splash screen prefetch and cleanup.

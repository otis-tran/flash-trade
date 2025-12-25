# Brainstorm: High-Performance Pagination Architecture

**Date:** 2025-12-25
**Topic:** Scalable data-handling solution for 3,000+ pages of token data
**Status:** Complete

---

## Problem Statement

API returns 321,613 tokens across 3,217 pages (100 items/page). Current implementation uses simple in-memory cache with basic pagination. Need a fast, smooth, scalable solution for excellent UX even with massive datasets.

### Current State Analysis

**Existing `core/network/` module (UNUSED):**
- `ApiService.kt` - Empty template, all endpoints commented out
- `SafeApiCall.kt` - Solid error handling, retry logic, parallel calls
- `NetworkResult.kt` - Good sealed class with map/flatMap transformers
- `AppException.kt` - Comprehensive exception hierarchy

**Active implementation:**
- `KyberApiService.kt` - Direct Retrofit calls without SafeApiCall wrapper
- `TokenRepositoryImpl.kt` - Basic in-memory `MutableStateFlow` cache
- No Paging library integration
- No Room database for persistence

---

## Key Constraints

| Constraint | Impact |
|------------|--------|
| 321K tokens, 3,217 pages | Can't load all at once |
| Flash Trade = "one-click" speed | UX must feel instant |
| DeFi context: volatile data | Need balance between fresh & cached |
| Meme tokens: newly_liquid priority | Must support filter `category=newly_liquid` |
| Mobile memory limits | Can't keep 300K objects in RAM |

---

## Evaluated Approaches

### Approach 1: Paging 3 + Room (RECOMMENDED)

**Architecture:**
```
UI (LazyColumn) → PagingData → RemoteMediator → Room DB → API
                                      ↓
                              TokenRemoteKeys (track page state)
```

**Components:**
1. `TokenEntity` + `TokenRemoteKeysEntity` in Room
2. `TokenRemoteMediator` - handles network+db sync
3. `TokenPagingSource` - Room as single source of truth
4. `GetPaginatedTokensUseCase` - returns `Flow<PagingData<Token>>`

**Pros:**
- Jetpack official, battle-tested
- Automatic scroll position restoration
- Built-in placeholders, retry, separator support
- Room as offline cache = instant cold starts
- Memory-efficient (loads 3 pages at a time)

**Cons:**
- Boilerplate: ~5 new classes
- RemoteMediator complexity for "newly_liquid" category
- Need careful invalidation strategy for volatile data

**Complexity:** Medium
**Production-readiness:** High

---

### Approach 2: Manual Infinite Scroll + LRU Cache

**Architecture:**
```
UI → ViewModel → TokenRepository → Memory LRU Cache (500 tokens)
                         ↓
                  KyberApiService (fetch on-demand)
```

**Implementation:**
- `LruCache<Int, List<Token>>` keyed by page number
- ViewModel tracks `lastVisiblePage`, prefetches ±1 page
- No Room, pure in-memory

**Pros:**
- Simpler, fewer classes
- Direct control over caching logic
- Lower learning curve

**Cons:**
- No persistence = cold start always hits network
- Manual scroll restoration
- Memory pressure with fast scrolling
- No built-in retry/placeholder support

**Complexity:** Low
**Production-readiness:** Medium

---

### Approach 3: Hybrid Tiered Caching

**Architecture:**
```
L1: Memory (Top 100 hot tokens)
L2: Room (last 1000 viewed)
L3: API (everything else)
```

**Pros:**
- Optimal for DeFi where top tokens dominate usage
- Minimal DB footprint
- Smart eviction based on access frequency

**Cons:**
- Complex eviction logic
- Hard to maintain consistency across tiers
- Over-engineered for current needs

**Complexity:** High
**Production-readiness:** Medium

---

## Final Recommendation: Approach 1 (Paging 3 + Room)

### Rationale

1. **Flash Trade's "Cache-First" already needs Room** - README says "Pre-fetch tokens during splash for instant display". Room enables this naturally.

2. **3,217 pages is NOT a real-world user flow** - Users typically:
   - View top 50-100 tokens (sorted by TVL/volume)
   - Search for specific token
   - Filter by `newly_liquid` (< 100 results usually)

   Paging handles all these efficiently.

3. **The unused `core/network/` shows intent** - Team already prepared SafeApiCall patterns. Paging fits this architecture.

4. **Jetpack Compose + Paging 3 = native integration** - `collectAsLazyPagingItems()` just works.

---

## Implementation Checklist

### Phase 1: Database Layer
- [ ] Add Room dependency if missing
- [ ] Create `TokenEntity` with indices on `address`, `totalTvl`
- [ ] Create `TokenRemoteKeysEntity` for page tracking
- [ ] Create `TokenDao` with `@Query` for pagination

### Phase 2: Paging Infrastructure
- [ ] Create `TokenRemoteMediator` (handles API→DB sync)
- [ ] Implement `getRefreshKey()` for scroll restoration
- [ ] Add invalidation logic for `newly_liquid` filters

### Phase 3: Repository Integration
- [ ] Update `TokenRepository` interface with `Flow<PagingData<Token>>`
- [ ] Implement `TokenRepositoryImpl` using `Pager`
- [ ] Migrate existing `getSafeTokens()` to use paging

### Phase 4: UI Integration
- [ ] Update ViewModel to expose `PagingData` flow
- [ ] Use `LazyPagingItems` in Compose
- [ ] Add loading/error state placeholders

### Phase 5: Performance Optimization
- [ ] Prefetch during splash screen
- [ ] Implement search with local DB + remote fallback
- [ ] Add TTL-based cache invalidation (5-10min for volatile data)

---

## Review: `core/network/` Module

**Current Status:** Created but completely unused.

**Assessment:**
- `SafeApiCall.kt` - **Keep and use**. Excellent patterns for error handling.
- `NetworkResult.kt` - **Keep**. Good for non-paging endpoints.
- `ApiService.kt` - **Delete or rename**. It's empty boilerplate.
- `AppException.kt` - **Keep**. Comprehensive exception handling.

**Recommendation:**
Wire `SafeApiCall` into `TokenRemoteMediator` for consistent error handling across paginated requests. Current `KyberApiService` calls bypass this entirely.

---

## Performance Targets

| Metric | Target | Implementation |
|--------|--------|----------------|
| Cold start token display | <100ms | Room + prefetch on splash |
| Scroll frame rate | 60fps | Paging3 automatic chunking |
| Page load latency | <500ms | Parallel fetch with prefetch |
| Memory footprint | <50MB for tokens | Room + limited memory cache |
| Search latency | <200ms | Room FTS + API fallback |

---

## Unresolved Questions

1. **Cache invalidation strategy for `newly_liquid`?**
   - These tokens are time-sensitive
   - Options: TTL (5min), manual refresh, WebSocket for real-time

2. **Should Room persist all 321K tokens?**
   - Alternative: Only cache pages user actually viewed
   - DB size could grow to ~100MB if all cached

3. **How to handle API rate limits?**
   - Current: No rate limit handling
   - Recommended: Implement in RemoteMediator with exponential backoff

---

## Next Steps

1. Confirm Paging 3 approach with team
2. Create implementation plan for Phase 1-5
3. Prioritize based on current feature branch (`feature/portfolio-screen`)

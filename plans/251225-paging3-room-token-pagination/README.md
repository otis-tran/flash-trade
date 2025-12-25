# Paging 3 + Room Token Pagination - Implementation Plan

**Plan ID:** 251225-paging3-room-token-pagination
**Created:** 2025-12-25
**Status:** Ready for Implementation
**Estimated Effort:** 8-12 hours

---

## Quick Links

- **[Master Plan](./plan.md)** - Complete overview, architecture, success criteria
- **[Phase 1: Database Layer](./phase-01-database-layer.md)** - Entities, DAO, schema
- **[Phase 2: Paging Infrastructure](./phase-02-paging-infrastructure.md)** - RemoteMediator
- **[Phase 3: Repository Integration](./phase-03-repository-integration.md)** - Pager config
- **[Phase 4: UI Integration](./phase-04-ui-integration.md)** - ViewModel, Composable
- **[Phase 5: Performance Optimization](./phase-05-performance-optimization.md)** - Prefetch
- **[Summary Report](../reports/251225-paging3-plan-summary.md)** - Executive summary

---

## What This Plan Implements

Migration from in-memory token caching to Paging 3 + Room architecture for:
- **High-performance pagination** (321,613 tokens across 3,217 pages)
- **Offline-first support** (Room as single source of truth)
- **Automatic network sync** (RemoteMediator pattern)
- **TTL-based cache** (5-minute invalidation)
- **Splash screen prefetch** (first 100 tokens cached on startup)

---

## Architecture Target

```
UI (LazyColumn) → PagingData → RemoteMediator → Room DB → Kyber API
                                      ↓
                              TokenRemoteKeys (pagination state)
```

**Key Components:**
- TokenEntity (Room entity with indices)
- TokenRemoteKeysEntity (pagination state tracker)
- TokenRemoteMediator (network-to-database sync)
- TokenDao (PagingSource provider)
- GetPagedTokensUseCase (domain layer)

---

## Implementation Phases

| Phase | Description | Effort | Files Created | Files Modified |
|-------|-------------|--------|---------------|----------------|
| **Phase 1** | Database Layer | 2-3h | 4 (~235 lines) | 2 |
| **Phase 2** | Paging Infrastructure | 3-4h | 1 (~195 lines) | 0 |
| **Phase 3** | Repository Integration | 1-2h | 0 | 2-3 |
| **Phase 4** | UI Integration | 1-2h | 1 (~25 lines) | 2 |
| **Phase 5** | Performance Optimization | 1-2h | 2-3 (~150 lines) | 1-2 |
| **Total** | | **8-12h** | **10-11 files** | **6-8 files** |

---

## How to Use This Plan

### Step 1: Review Master Plan
Read `plan.md` for complete context, architecture decisions, and success criteria.

### Step 2: Execute Phases Sequentially
1. Start with Phase 1 (Database Layer)
2. Complete implementation per phase documentation
3. Test manually after each phase
4. Proceed to next phase only after verification

### Step 3: Follow Phase Documentation
Each phase document contains:
- **Objectives** - What this phase accomplishes
- **Implementation Steps** - Code snippets, file paths
- **Verification Checklist** - Testing requirements
- **Files Summary** - Created/modified files

### Step 4: Manual Testing
Use testing checklists in each phase:
- Phase 1: Database schema verification
- Phase 2: RemoteMediator load types
- Phase 3: Repository method integration
- Phase 4: UI pagination flow
- Phase 5: Prefetch performance

### Step 5: Documentation Updates
After Phase 5 completion:
- Update `docs/codebase-summary.md`
- Update `docs/system-architecture.md`
- Update `README.md` implementation status

---

## Key Features

### Paging 3 Benefits
- ✅ Efficient memory usage (only visible + prefetch window)
- ✅ Automatic pagination handling
- ✅ Scroll position restoration
- ✅ LoadState management
- ✅ Pull-to-refresh support

### Room Integration
- ✅ Offline-first architecture
- ✅ Single source of truth pattern
- ✅ Indexed queries (fast lookups)
- ✅ Transaction safety

### Performance Optimizations
- ✅ Splash screen prefetch (first 100 tokens)
- ✅ TTL-based cache invalidation (5 min)
- ✅ Parallel initialization (with Privy, etc.)
- ✅ Memory bounded (maxSize: 500 items)

---

## Technical Specifications

### Database Schema

**tokens table:**
```sql
CREATE TABLE tokens (
    address TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    symbol TEXT NOT NULL,
    decimals INTEGER NOT NULL,
    logo_url TEXT,
    is_verified INTEGER NOT NULL,
    is_whitelisted INTEGER NOT NULL,
    is_stable INTEGER NOT NULL,
    is_honeypot INTEGER NOT NULL,
    total_tvl REAL NOT NULL,
    pool_count INTEGER NOT NULL,
    cgk_rank INTEGER,
    cmc_rank INTEGER,
    cached_at INTEGER NOT NULL
);

CREATE UNIQUE INDEX index_tokens_address ON tokens(address);
CREATE INDEX index_tokens_total_tvl ON tokens(total_tvl);
```

**token_remote_keys table:**
```sql
CREATE TABLE token_remote_keys (
    token_address TEXT PRIMARY KEY,
    prev_page INTEGER,
    next_page INTEGER,
    created_at INTEGER NOT NULL
);
```

### PagingConfig
```kotlin
PagingConfig(
    pageSize = 100,
    prefetchDistance = 20,
    enablePlaceholders = false,
    initialLoadSize = 200,
    maxSize = 500
)
```

### API Integration
- Endpoint: `GET /ethereum/api/v1/tokens`
- Page Size: 100 tokens/page
- Total: 321,613 tokens across 3,217 pages
- Filters: minTvl, maxTvl, sort, page, limit

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| Cold Start | <800ms | PerformanceLogger.logColdStart() |
| Prefetch Duration | <300ms | PerformanceLogger.logTokenPrefetch() |
| First Token Display | <100ms | From Room cache (instant) |
| Memory Footprint | ~10MB (1000 tokens) | Android Profiler |

---

## Backward Compatibility

**Existing Methods Preserved:**
- `getTokens(filter)` - Manual pagination (in-memory cache)
- `getTokenByAddress(address)` - Single token lookup
- `searchTokens(query)` - Search functionality
- `getSafeTokens()` - Filtered safe tokens
- `observeTokens()` - Flow of cached tokens

**New Method Added:**
- `getPagedTokens(filter)` - Paging 3 flow

**Migration Path:**
1. Implement new method (Phase 3)
2. Migrate TradingScreen to use new method (Phase 4)
3. Gradually migrate other screens
4. Mark old methods `@Deprecated` in future
5. Remove in later PR after full migration

---

## Success Criteria

### Functional Requirements ✅
- [ ] All 5 phases implemented
- [ ] Paging 3 dependencies added
- [ ] Database schema created
- [ ] RemoteMediator syncs network → Room
- [ ] Repository exposes Flow<PagingData<Token>>
- [ ] UI uses LazyPagingItems
- [ ] Prefetch during splash
- [ ] Backward compatibility maintained

### Performance Requirements 🎯
- [ ] Cold start <800ms
- [ ] Prefetch <300ms
- [ ] Memory <15MB for 1000 tokens
- [ ] Offline support functional

### Code Quality 📐
- [ ] All files <200 lines
- [ ] Follows kebab-case naming
- [ ] Hilt DI throughout
- [ ] SafeApiCall pattern maintained
- [ ] No unit tests (per requirements)

---

## Common Issues & Solutions

### Issue 1: Room Migration Fails
**Solution:** Use `.fallbackToDestructiveMigration()` in DatabaseModule (dev only)

### Issue 2: PagingData Not Updating
**Solution:** Ensure `cachedIn(viewModelScope)` applied to Flow

### Issue 3: Remote Keys Out of Sync
**Solution:** Clear all keys on REFRESH loadType (implemented in Phase 2)

### Issue 4: Prefetch Slows Cold Start
**Solution:** Run prefetch in parallel with other initialization (Phase 5)

---

## Plan Checklist

### Before Starting
- [x] Plan reviewed and approved
- [x] Dependencies identified (Paging 3, Room)
- [x] Architecture validated
- [ ] Development environment ready

### During Implementation
- [ ] Phase 1 complete (Database Layer)
- [ ] Phase 2 complete (Paging Infrastructure)
- [ ] Phase 3 complete (Repository Integration)
- [ ] Phase 4 complete (UI Integration)
- [ ] Phase 5 complete (Performance Optimization)

### After Completion
- [ ] All manual tests passed
- [ ] Performance targets met
- [ ] Documentation updated
- [ ] Plan marked as completed
- [ ] Cleanup unused code

---

## Resources

### Paging 3 Documentation
- [Official Guide](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
- [RemoteMediator Sample](https://github.com/android/architecture-components-samples/tree/main/PagingWithNetworkSample)
- [Compose Integration](https://developer.android.com/topic/libraries/architecture/paging/v3-paged-data#compose)

### Room Documentation
- [Room Paging](https://developer.android.com/training/data-storage/room/paging)
- [Room Migration](https://developer.android.com/training/data-storage/room/migrating-db-versions)

### Project Documentation
- `docs/code-standards.md`
- `docs/system-architecture.md`
- `docs/project-overview-pdr.md`

---

## Support

**Questions or Issues?**
- Check phase documentation for implementation details
- Review master plan for architectural context
- Refer to project code standards for style guide

---

## Implementation Notes

Add implementation notes here as you progress through phases:

### Phase 1 Notes
-

### Phase 2 Notes
-

### Phase 3 Notes
-

### Phase 4 Notes
-

### Phase 5 Notes
-

---

**Last Updated:** 2025-12-25
**Plan Status:** Ready for Implementation
**Active Plan:** `export CK_ACTIVE_PLAN=plans/251225-paging3-room-token-pagination`

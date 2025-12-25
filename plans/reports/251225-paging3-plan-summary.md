# Implementation Plan Summary: Paging 3 + Room Token Pagination

**Plan ID:** 251225-paging3-room-token-pagination
**Date:** 2025-12-25
**Planner:** Claude (Planner Subagent)
**Status:** Ready for Implementation
**Estimated Total Effort:** 8-12 hours

---

## Executive Summary

Created comprehensive 5-phase implementation plan to migrate Flash Trade token list from in-memory MutableStateFlow cache to Paging 3 + Room architecture. Plan enables high-performance pagination for 321,613 tokens across 3,217 pages while maintaining offline-first capability and backward compatibility.

---

## Architecture Overview

**Target Pattern:** UI → PagingData → RemoteMediator → Room DB → Kyber API

**Key Components:**
- **Database:** TokenEntity (indexed), TokenRemoteKeysEntity (pagination state)
- **Paging:** TokenRemoteMediator (network-to-database sync)
- **Repository:** getPagedTokens() returns Flow<PagingData<Token>>
- **UI:** LazyPagingItems with LoadState handling
- **Optimization:** Splash screen prefetch (first 100 tokens)

---

## Implementation Phases

### Phase 1: Database Layer (2-3 hours)
**Files Created:** 4 (~235 lines)
- `token-entity.kt` - Room entity with indices
- `token-remote-keys-entity.kt` - Pagination state tracker
- `token-dao.kt` - DAO with PagingSource
- `token-entity-mapper.kt` - Entity ↔ Domain mapping

**Files Modified:** 2
- `build.gradle.kts` - Add Paging dependencies
- `FlashTradeDatabase.kt` - Add new entities (version bump to 2)

**Deliverables:**
- Room entities with proper indices (address, total_tvl)
- DAO providing PagingSource for Paging library
- Database version 2 with destructive migration (dev only)

---

### Phase 2: Paging Infrastructure (3-4 hours)
**Files Created:** 1 (~195 lines)
- `token-remote-mediator.kt` - RemoteMediator implementation

**Key Features:**
- LoadType handling (REFRESH, APPEND, PREPEND)
- TTL-based cache invalidation (5 min)
- SafeApiCall integration (NetworkResult → MediatorResult)
- Atomic database transactions
- Remote key management for pagination state

---

### Phase 3: Repository Integration (1-2 hours)
**Files Modified:** 2-3
- `TokenRepository.kt` - Add getPagedTokens() interface
- `TokenRepositoryImpl.kt` - Implement with Pager configuration
- `database-module.kt` - Ensure DB injection (if needed)

**Configuration:**
- pageSize: 100 (matches API)
- prefetchDistance: 20
- initialLoadSize: 200 (2 pages)
- maxSize: 500 (memory bound)
- enablePlaceholders: false (simpler UI)

**Backward Compatibility:** Existing methods unchanged

---

### Phase 4: UI Integration (1-2 hours)
**Files Created:** 1 (~25 lines)
- `get-paged-tokens-use-case.kt` - Domain layer use case

**Files Modified:** 2 (~115 lines)
- `trading-view-model.kt` - Expose Flow<PagingData<Token>>
- `trading-screen.kt` - Use collectAsLazyPagingItems()

**UI Features:**
- LoadState handling (refresh/append)
- Pull-to-refresh support
- Error state display
- Scroll position restoration (cachedIn)

---

### Phase 5: Performance Optimization (1-2 hours)
**Files Created:** 2-3 (~150 lines)
- `token-cache-manager.kt` - Prefetch logic
- `app-startup-manager.kt` - Parallel initialization (if new)
- `performance-logger.kt` - Monitoring utilities

**Files Modified:** 1-2
- `MainActivity.kt` or `AndroidManifest.xml` - Hook prefetch

**Files Deleted:** 0-1
- `ApiService.kt` - If unused boilerplate exists

**Optimizations:**
- Splash screen prefetch (first 100 tokens)
- Parallel initialization with Privy/other tasks
- Performance logging for cold start monitoring
- SafeApiCall pattern maintained

---

## Technical Specifications

### Database Schema

**TokenEntity:**
- Primary Key: address
- Indices: address (unique), total_tvl
- Fields: name, symbol, decimals, logoUrl, isVerified, isWhitelisted, isStable, isHoneypot, totalTvl, poolCount, cgkRank, cmcRank, cachedAt

**TokenRemoteKeysEntity:**
- Primary Key: tokenAddress
- Fields: prevPage, nextPage, createdAt

### Performance Targets

| Metric | Target | Current |
|--------|--------|---------|
| Cold Start | <800ms | ~1.2s |
| Prefetch Duration | <300ms | TBD |
| First Token Display | <100ms | TBD |
| Memory Footprint | ~10MB (1000 tokens) | ~50MB (in-memory) |

### TTL Configuration

- Cache TTL: 5 minutes (300 seconds)
- Invalidation: Automatic via RemoteMediator.initialize()
- Manual Refresh: Pull-to-refresh in UI

---

## Project Integration

### Follows Project Standards
- **File Naming:** kebab-case (token-entity.kt)
- **Code Style:** Kotlin official, <200 lines/file
- **Architecture:** MVI + Clean Architecture
- **DI:** Hilt injection throughout
- **Error Handling:** SafeApiCall pattern maintained

### Backward Compatibility
- Existing getTokens(), searchTokens(), getSafeTokens() methods unchanged
- In-memory cache remains for non-paged features
- Gradual migration path (TradingScreen first)

---

## Risk Mitigation

### Identified Risks
1. **Remote Keys Desync:** TTL + REFRESH loadType rebuild state
2. **SafeApiCall Integration:** NetworkResult mapped to MediatorResult
3. **Migration Complexity:** New method coexists with old, phased rollout

### Mitigation Strategies
- Atomic database transactions prevent partial state
- Destructive migration for dev (no prod data yet)
- Comprehensive error handling in RemoteMediator
- Performance logging for monitoring

---

## Testing Strategy

**Manual Testing Checklist (NO UNIT TESTS per requirements):**

1. **Pagination Flow**
   - First load (200 tokens)
   - Scroll to trigger append
   - Continuous scrolling

2. **Offline Support**
   - Load tokens online
   - Enable airplane mode
   - Verify cached data accessible

3. **TTL Cache**
   - Fresh cache skips API
   - Expired cache refreshes

4. **Error Handling**
   - Network errors display gracefully
   - Pull-to-refresh recovers

5. **Config Changes**
   - Rotation preserves scroll position
   - No unnecessary re-fetch

---

## Files Summary

### Total Files Created: 10-11
1. token-entity.kt
2. token-remote-keys-entity.kt
3. token-dao.kt
4. token-entity-mapper.kt
5. token-remote-mediator.kt
6. get-paged-tokens-use-case.kt
7. token-cache-manager.kt
8. app-startup-manager.kt (if new)
9. performance-logger.kt

### Total Files Modified: 6-8
1. build.gradle.kts
2. FlashTradeDatabase.kt
3. TokenRepository.kt
4. TokenRepositoryImpl.kt
5. trading-view-model.kt
6. trading-screen.kt
7. MainActivity.kt or AndroidManifest.xml
8. database-module.kt (if needed)

### Total Files Deleted: 0-1
1. ApiService.kt (if unused)

### Total Lines Added: ~750-850 lines
- Phase 1: ~241 lines
- Phase 2: ~195 lines
- Phase 3: ~60 lines
- Phase 4: ~140 lines
- Phase 5: ~165 lines

---

## Documentation Updates Required

Post-implementation documentation updates:

1. **docs/codebase-summary.md**
   - Add Paging 3 dependencies
   - Add TokenEntity, TokenRemoteKeysEntity schema
   - Update TokenRepository methods

2. **docs/system-architecture.md**
   - Add Paging 3 architecture diagram
   - Document RemoteMediator pattern
   - Update data flow diagrams

3. **README.md**
   - Update "Token Prefetch (Splash)" status to ✅
   - Update performance metrics table

---

## Success Criteria

### Functional Requirements
- [x] Plan created with 5 detailed phases
- [ ] Paging 3 dependencies added
- [ ] Database layer complete (entities, DAO)
- [ ] RemoteMediator handles pagination
- [ ] Repository exposes Flow<PagingData<Token>>
- [ ] UI uses LazyPagingItems
- [ ] Prefetch during splash screen
- [ ] Backward compatibility maintained

### Non-Functional Requirements
- [ ] Cold start <800ms (measured)
- [ ] Prefetch <300ms (measured)
- [ ] Memory usage <15MB for 1000 tokens
- [ ] Offline support functional
- [ ] All files <200 lines
- [ ] Follows project code standards

---

## Implementation Approach

### Sequential Execution
1. Phase 1 (Database) → Phase 2 (RemoteMediator) → Phase 3 (Repository)
2. Phase 4 (UI) → Phase 5 (Optimization)
3. Each phase depends on previous completion
4. Test after each phase before proceeding

### Estimated Timeline
- **Day 1:** Phases 1-2 (5-7 hours)
- **Day 2:** Phases 3-5 (3-5 hours)
- **Total:** 1.5-2 days for single developer

---

## Next Actions

1. **Review plan** with stakeholders/team lead
2. **Begin Phase 1** - Database Layer implementation
3. **Iterative development** - Complete phases sequentially
4. **Manual testing** - Use checklist after each phase
5. **Documentation update** - After Phase 5 complete
6. **Plan closure** - Mark completed and archive

---

## Unresolved Questions

None. All architectural decisions documented in plan.md.

---

## Plan Location

**Path:** `D:/projects/flash-trade/plans/251225-paging3-room-token-pagination/`

**Files:**
- `plan.md` - Master plan overview
- `phase-01-database-layer.md` - Database implementation details
- `phase-02-paging-infrastructure.md` - RemoteMediator implementation
- `phase-03-repository-integration.md` - Repository updates
- `phase-04-ui-integration.md` - ViewModel and UI changes
- `phase-05-performance-optimization.md` - Prefetch and cleanup

---

## Contact

**Planner:** Claude (Planner Subagent ab24439)
**Plan Date:** 2025-12-25
**Status:** Ready for Implementation

---

**End of Summary Report**

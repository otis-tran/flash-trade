# Portfolio Feature Implementation Plan

**Date**: 2025-12-25
**Feature**: High-Performance Crypto Portfolio with Etherscan V2 API
**Status**: Planning Complete
**Branch**: `feature/portfolio-screen`

---

## Overview

Transform Flash Trade's portfolio screen from mock data to production-ready real-time crypto balance tracking. Implement dual-network support (Ethereum + Linea) with Etherscan V2 API integration, focusing on maximum performance via parallel data fetching, TTL-based caching, and Compose optimization.

### Core Requirements

1. **Real-Time Balance Tracking**: Fetch ETH balances + ERC-20 tokens via Etherscan V2 API
2. **Dual Network Support**: Ethereum (chainId=1) + Linea (chainId=59144) using existing NetworkMode enum
3. **Transaction History**: Merge normal (txlist) + token transfers (tokentx), display chronologically
4. **USD Valuation**: Calculate total portfolio value (future: integrate price oracle)
5. **QR Features**: Generate receive QR, scan for send (ZXing library)
6. **Performance**: <500ms initial render (cached data), parallel API calls, LazyColumn optimization

### Architecture Context

- **Existing**: MVI architecture with PortfolioViewModel, State, Event, Effect
- **Data Layer**: Add EtherscanApiService (Retrofit) + PortfolioRepository
- **Caching**: DataStore (balances, 30s TTL) + Room (transactions, 1h TTL)
- **DI**: Hilt modules (NetworkModule, PortfolioModule)

---

## Implementation Phases

### Phase 01: Data Layer - Etherscan API Integration
**File**: `phase-01-etherscan-api-integration.md`
**Status**: DONE (2025-12-25)
**Priority**: High
**Description**: Create Retrofit service for Etherscan V2 API with DTOs, mappers, and base networking infrastructure.

### Phase 02: Domain Layer - Portfolio Repository
**File**: `phase-02-portfolio-repository.md`
**Status**: DONE (2025-12-25)
**Priority**: High
**Dependencies**: Phase 01
**Description**: Implement repository pattern with use cases for parallel data fetching (supervisorScope).

### Phase 03: Data Layer - Caching Strategy
**File**: `phase-03-caching-strategy.md`
**Status**: DONE (2025-12-25)
**Priority**: High
**Dependencies**: Phase 02
**Description**: Add Room database for transactions, DataStore for balances, TTL-based invalidation logic.

### Phase 04: Presentation Layer - ViewModel Integration
**File**: `phase-04-viewmodel-integration.md`
**Status**: DONE (2025-12-25)
**Priority**: High
**Dependencies**: Phase 03
**Description**: Replace mock data in PortfolioViewModel with real API calls, implement stale-while-revalidate pattern.

### Phase 05: QR Code Feature
**File**: `phase-05-qr-code-feature.md`
**Status**: DONE (2025-12-25)
**Priority**: Medium
**Dependencies**: Phase 04
**Description**: Add QR generation (receive) and scanner (send) using ZXing library with bottom sheet UI.

---

## Success Criteria

- [ ] ETH balance updates every 30s (cached display)
- [ ] Transaction history loads with pagination (100 items per page)
- [ ] Network switch (Ethereum ↔ Linea) triggers immediate refresh
- [ ] Portfolio renders in <500ms from cache on cold start
- [ ] LazyColumn scroll performance: 60fps with 100+ transactions
- [ ] QR scanner successfully reads wallet addresses
- [ ] Error states handled gracefully (network failure → show cached data)

---

## Performance Targets

| Metric | Target | Implementation |
|--------|--------|----------------|
| Initial Render | <500ms | Cache-first with DataStore/Room |
| Balance Refresh | <2s | Parallel fetch via supervisorScope |
| Transaction Load | <3s | Paginated API calls + Room cache |
| Scroll Performance | 60fps | LazyColumn keys + contentType |
| Cache Hit Rate | >80% | 30s (balance) + 1h (tx) TTL |

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Etherscan API rate limits (5 calls/sec) | High | Implement exponential backoff, cache aggressively |
| Large transaction lists (1000+ items) | Medium | Mandatory pagination, consider Paging 3 if >500 tx |
| Network failures during portfolio load | Medium | Stale-while-revalidate, show cached data with error toast |
| Decimal handling errors (Wei ↔ ETH) | Medium | Use BigDecimal + Ethers-kt validation, unit tests |
| Memory leaks from coroutine scopes | Low | Use viewModelScope, structured concurrency |

---

## Key Architectural Decisions

1. **supervisorScope over coroutineScope**: One failed data source (prices) shouldn't cancel others (balances, transactions)
2. **Room for transactions, DataStore for balances**: Complex queries + pagination vs. simple key-value
3. **No Paging 3 initially**: Implement manual pagination first, add Paging 3 if user feedback demands it
4. **Ethers-kt for Wei conversion**: Avoid BigDecimal errors with library validation
5. **ZXing over MLKit**: Smaller APK size, no Google Play Services dependency

---

## References

- Research: `research/researcher-01-etherscan-api.md` (Etherscan V2 API patterns)
- Research: `research/researcher-02-android-performance.md` (Performance optimization)
- Scout: `scout/scout-01-portfolio-files.md` (Existing codebase structure)
- Existing Code: `PortfolioViewModel.kt`, `PortfolioState.kt`, `UserPreferences.kt`
- API Docs: [Etherscan V2 API](https://docs.etherscan.io/v2-migration)

---

## Next Steps

1. Review and approve this plan
2. Start Phase 01: Etherscan API integration
3. Implement phases sequentially (each depends on previous)
4. Test performance at each phase completion
5. Update plan.md with status as phases complete

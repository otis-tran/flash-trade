# Portfolio Feature - Implementation Plan

**Date**: 2025-12-25
**Status**: Planning Complete
**Planner**: planner (subagent a662029)
**Plan Directory**: `D:/projects/flash-trade/plans/251225-portfolio-feature/`

---

## Summary

Created comprehensive implementation plan for Flash Trade's high-performance crypto portfolio feature. Plan transforms existing mock data implementation into production-ready real-time balance tracking with Etherscan V2 API integration across Ethereum + Linea networks.

### Plan Structure

- **Main Plan**: `plan.md` (overview, success criteria, risk assessment)
- **5 Phase Files**: Detailed step-by-step implementation guides
- **Total Lines**: ~650 lines across 6 files

---

## Key Deliverables

### 1. Main Plan (plan.md)
- Feature overview with performance targets (<500ms cache render, 60fps scroll)
- 5 implementation phases with dependencies
- Success criteria checklist (7 items)
- Performance targets table (5 metrics)
- Risk assessment with mitigations
- Architectural decisions (supervisorScope, Room vs DataStore, etc.)

### 2. Phase 01: Etherscan API Integration
**Lines**: ~145
**Priority**: High
**Key Components**:
- Retrofit service for 3 Etherscan V2 endpoints (balance, tokentx, txlist)
- DTOs with Moshi JSON parsing (BalanceResponseDto, TokenTxDto, TxDto)
- Mappers for DTO → Domain conversion
- NetworkModule DI configuration
- API key setup in BuildConfig

**Critical Patterns**:
- Unified V2 endpoint with chainid parameter (Ethereum=1, Linea=59144)
- Wei → ETH conversion using BigDecimal for precision
- Status="1" for success, "0" for error handling

### 3. Phase 02: Portfolio Repository
**Lines**: ~150
**Priority**: High
**Dependencies**: Phase 01
**Key Components**:
- PortfolioRepository interface with 4 methods
- PortfolioRepositoryImpl with parallel fetching (supervisorScope)
- 3 use cases (GetBalanceUseCase, GetTokensUseCase, GetTransactionsUseCase)
- Token balance aggregation from transfer history
- Graceful degradation on partial failures

**Critical Patterns**:
- supervisorScope for independent failure handling
- Parallel API calls with async/await
- Transaction merging (normal + token transfers) sorted by timestamp

### 4. Phase 03: Caching Strategy
**Lines**: ~140
**Priority**: High
**Dependencies**: Phase 02
**Key Components**:
- Room database with TransactionEntity + TransactionDao
- UserPreferences DataStore extension for balance cache
- TTL-based invalidation (30s balance, 1h transactions)
- Stale-while-revalidate pattern

**Critical Patterns**:
- Tiered caching: Room (complex queries) vs DataStore (key-value)
- Cache-first with background refresh
- Network-specific cache keys (Ethereum vs Linea)
- Automatic expired cache cleanup

### 5. Phase 04: ViewModel Integration
**Lines**: ~130
**Priority**: High
**Dependencies**: Phase 03
**Key Components**:
- Replace mock data in PortfolioViewModel
- Cache-first loading with background refresh
- Network switch handling with data reload
- LazyColumn optimization (keys + contentType)
- Pull-to-refresh support

**Critical Patterns**:
- Show cached data immediately (<500ms), refresh in background
- LazyColumn keys for 40% scroll performance improvement
- derivedStateOf for computed values (totalBalanceUsd)
- Error state with cached data fallback

### 6. Phase 05: QR Code Feature
**Lines**: ~135
**Priority**: Medium
**Dependencies**: Phase 04
**Key Components**:
- QR code generation for wallet address (ZXing)
- QR scanner with CameraX + MLKit barcode scanning
- Bottom sheet UI for receive/send actions
- Camera permission handling with rationale
- EIP-681 format support (ethereum:0x...@chainId)

**Critical Patterns**:
- ZXing for generation (lightweight), MLKit for scanning (Compose-friendly)
- Address validation with checksum verification
- Runtime permission request with fallback to manual input

---

## Research Insights Applied

### From researcher-01-etherscan-api.md
- Etherscan V2 unified endpoint with chainid parameter
- Rate limit: 5 calls/sec (requires caching + backoff)
- tokentx endpoint for token balance aggregation (sum incoming - outgoing)
- Response format: `{status: "1", message: "OK", result: "..."}`

### From researcher-02-android-performance.md
- supervisorScope prevents cascade failures (one failed source doesn't cancel others)
- LazyColumn keys improve scroll performance by 40%
- Tiered caching: DataStore (30s TTL balance) + Room (1h TTL transactions)
- Stale-while-revalidate: show cache immediately, refresh background

### From scout-01-portfolio-files.md
- Existing MVI architecture with PortfolioViewModel + State + Event + Effect
- NetworkMode enum already has chainId (Ethereum=1, Linea=59144)
- UserPreferences DataStore ready for extension
- BaseViewModel provides viewModelScope for structured concurrency

---

## Performance Targets

| Metric | Target | Implementation Strategy |
|--------|--------|-------------------------|
| Initial Render | <500ms | DataStore cache-first loading |
| Balance Refresh | <2s | Parallel fetch via supervisorScope |
| Transaction Load | <3s | Paginated API + Room cache |
| Scroll Performance | 60fps | LazyColumn keys + contentType |
| Cache Hit Rate | >80% | 30s (balance) + 1h (tx) TTL |

---

## Risk Mitigation Summary

**High Priority Risks**:
1. **Etherscan rate limits (5 calls/sec)**: Exponential backoff + aggressive caching (30s-1h TTL)
2. **Large transaction lists (1000+)**: Mandatory pagination (100 items/page), Room cache with trimming

**Medium Priority Risks**:
3. **Network failures during load**: Stale-while-revalidate pattern, show cached data with error toast
4. **Decimal handling errors (Wei ↔ ETH)**: BigDecimal validation + Ethers-kt library

**Low Priority Risks**:
5. **Memory leaks from coroutines**: viewModelScope auto-cancellation + structured concurrency

---

## Architectural Decisions

1. **supervisorScope over coroutineScope**: One failed data source doesn't cancel others
2. **Room for transactions, DataStore for balances**: Complex queries + pagination vs simple key-value
3. **No Paging 3 initially**: Manual pagination first, add Paging 3 if user feedback demands
4. **Ethers-kt for Wei conversion**: Avoid BigDecimal errors (future enhancement)
5. **ZXing + MLKit for QR**: Lightweight generation + Compose-friendly scanning

---

## Implementation Order

1. **Phase 01** (Etherscan API): Foundation for all data fetching
2. **Phase 02** (Repository): Business logic + parallel fetching patterns
3. **Phase 03** (Caching): Performance optimization via TTL-based cache
4. **Phase 04** (ViewModel): Replace mock data, integrate real API + cache
5. **Phase 05** (QR Code): User-facing receive/send feature

Each phase depends on previous, allowing incremental testing and validation.

---

## Success Criteria Checklist

- [ ] ETH balance updates every 30s (cached display)
- [ ] Transaction history loads with pagination (100 items/page)
- [ ] Network switch (Ethereum ↔ Linea) triggers immediate refresh
- [ ] Portfolio renders <500ms from cache on cold start
- [ ] LazyColumn scroll: 60fps with 100+ transactions
- [ ] QR scanner reads wallet addresses successfully
- [ ] Error states show cached data gracefully

---

## Files Created

1. `plan.md` - Main overview (80 lines)
2. `phase-01-etherscan-api-integration.md` - API setup (145 lines)
3. `phase-02-portfolio-repository.md` - Domain layer (150 lines)
4. `phase-03-caching-strategy.md` - Room + DataStore (140 lines)
5. `phase-04-viewmodel-integration.md` - Presentation layer (130 lines)
6. `phase-05-qr-code-feature.md` - QR generation/scanning (135 lines)

**Total**: ~780 lines of actionable implementation guidance

---

## Unresolved Questions

1. Should we use free Etherscan API tier (5 calls/sec) or require user-provided key?
2. Token price oracle integration - use CoinGecko, CryptoCompare, or defer to v2?
3. Offline-first sync for transactions vs always-online assumption?
4. Paging 3 threshold - how many transactions before mandatory pagination library?
5. QR format - simple address or EIP-681 (ethereum:0x...@chainId) as default?

---

## Next Actions

1. Review plan with stakeholders
2. Create feature branch: `feature/portfolio-etherscan-api`
3. Start Phase 01: Etherscan API integration
4. Test each phase completion against success criteria
5. Update plan.md phase status as work progresses

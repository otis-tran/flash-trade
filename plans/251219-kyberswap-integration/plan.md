# KyberSwap Aggregator V1 Integration - Implementation Plan

**Date:** 2025-12-19
**Target:** Flash Trade Android App
**Scope:** Swap execution via KyberSwap Aggregator V1 API with Privy wallet integration
**Performance Goal:** Sub-15s app open → successful token swap

---

## Executive Summary

Integrate KyberSwap Aggregator V1 API for token swapping with MEV protection, combined with Privy TEE wallet for transaction signing. Implementation follows MVI + Clean Architecture with 6 parallel-executable phases.

**Key Features:**
- Quote fetching from KyberSwap V1 routes endpoint
- Transaction encoding via build endpoint
- Privy wallet transaction signing
- Real-time quote refresh (5s interval)
- ERC20 approval handling
- Base chain (8453) primary, multi-chain ready

---

## Architecture Overview

```
[SwapScreen] → [SwapViewModel] → [ExecuteSwapUseCase] → [SwapRepository]
                                                              ↓
                                    ┌─────────────────────────┴─────────────────┐
                                    ↓                                           ↓
                          [KyberSwapApiService]                      [PrivyAuthService]
                                    ↓                                           ↓
                            GET /routes                                wallet.provider
                            POST /build                              .request(ethSendTx)
```

---

## Phase Breakdown

### Phase 1: API Layer (30min)
- Create KyberSwapApiService with V1 swap endpoints
- Define DTOs: RouteResponseDto, BuildRouteRequestDto, BuildRouteResponseDto
- Add x-client-id header interceptor
- **Files:** 4 new (service, 3 DTOs)

### Phase 2: Domain Layer (25min)
- Create domain models: Quote, EncodedSwap, SwapResult
- Create SwapRepository interface
- Create GetSwapQuoteUseCase, ExecuteSwapUseCase
- **Files:** 6 new (3 models, 1 interface, 2 use cases)

### Phase 3: Data Layer (45min)
- Create SwapRepositoryImpl with quote caching (5s TTL)
- Add token approval check/execution
- Integrate Privy wallet transaction signing
- Create SwapMapper for DTO → domain conversion
- **Files:** 3 new (repository impl, mapper, cache manager)

### Phase 4: Presentation Layer (60min)
- Create SwapScreen with token input, amount, quote display
- Create SwapViewModel with MVI pattern
- Create SwapState, SwapEvent, SwapEffect
- Add quote auto-refresh (5s interval)
- **Files:** 4 new (screen, view model, state, event)

### Phase 5: Navigation & Integration (15min)
- Add SwapScreen to navigation graph
- Connect TradingScreen token click → SwapScreen
- Add SwapModule for Hilt DI
- **Files:** 2 modified, 1 new

### Phase 6: Optimization (30min)
- Quote prefetching on token selection
- Parallel initialization
- Transaction status tracking
- **Files:** 2 modified, 1 new

**Total Estimated Time:** 3h 25min
**Total New Files:** 20
**Modified Files:** 5

---

## Critical Dependencies

1. **Existing:**
   - Retrofit instance with Moshi (@Named("kyber"))
   - Privy SDK initialized via PrivyProvider
   - TradingScreen with token list
   - MVI base classes (BaseViewModel, UiState, UiEvent, UiEffect)
   - Result wrapper for error handling

2. **External APIs:**
   - KyberSwap Aggregator V1: https://aggregator-api.kyberswap.com/base/api/v1/
   - Base chain RPC for gas estimation (optional)

3. **Required Env:**
   - CLIENT_ID for KyberSwap (x-client-id header)
   - Privy app ID (already configured)

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Quote expires before execution | High | 5s TTL, force refresh on swap |
| Token approval fails | High | Pre-check allowance, retry logic |
| User rejects transaction | Medium | Clear error state, retry option |
| Gas estimation failure | Medium | Fallback to 300k gas limit |
| Rate limiting (no client-id) | Low | Always include x-client-id header |

---

## Success Criteria

- [ ] Quote fetched in <2s for popular pairs
- [ ] Swap executed in <5s (approval + swap)
- [ ] Error states handled gracefully
- [ ] Quote auto-refresh without flicker
- [ ] Transaction hash returned and trackable
- [ ] All files <200 lines

---

## Next Steps

1. Read phase-01-api-layer.md for API implementation
2. Read phase-02-domain-layer.md for business logic
3. Read phase-03-data-layer.md for repository implementation
4. Read phase-04-presentation-layer.md for UI implementation
5. Read phase-05-navigation-integration.md for navigation setup
6. Read phase-06-optimization.md for performance enhancements

---

## Related Documents

- Research: `research/researcher-01-kyberswap-api.md`
- Research: `research/researcher-02-privy-wallet.md`
- Scout: `scout/scout-01-codebase-files.md`
- Code Standards: `docs/code-standards.md`
- Architecture: `docs/system-architecture.md`

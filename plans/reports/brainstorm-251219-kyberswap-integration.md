# KyberSwap Integration Strategy - Brainstorming Report

**Date**: 2024-12-19
**Branch**: feature/portfolio-screen
**Target**: Sub-15s app open → successful token buy

---

## 1. Executive Summary

Flash Trade integrates KyberSwap Aggregator for DEX swaps. Critical path analysis shows **15s target is aggressive but achievable** with proper optimization. Key strategy: **parallel prefetching, cached quotes, optimized transaction flow**.

---

## 2. Problem Statement

| Requirement | Challenge |
|-------------|-----------|
| App open → token buy < 15s | Cold start ~600ms + API latency + tx confirmation |
| Use KyberSwap Aggregator | 2-step API (GET route → POST encode) |
| Buy AND sell support | Same flow, different direction |
| User owns tokens (no debt) | Native wallet, not borrowed funds |
| Speed + reliability primary | Optimize for both, not just one |

---

## 3. KyberSwap API Architecture Analysis

### 3.1 V1 API Flow (Recommended)

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        KyberSwap V1 API Flow                              │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────┐     ┌───────────────────────────┐                       │
│  │ GET /routes │────▶│ Response: routeSummary    │                       │
│  │             │     │ - tokenIn/Out, amounts    │                       │
│  │ ~200-500ms  │     │ - gas estimates           │                       │
│  └─────────────┘     │ - route[], routerAddress  │                       │
│                      └──────────────┬────────────┘                       │
│                                     │                                     │
│                                     ▼                                     │
│  ┌─────────────────────┐     ┌───────────────────────────┐               │
│  │ POST /route/build   │────▶│ Response: encoded data    │               │
│  │                     │     │ - data (calldata hex)     │               │
│  │ ~100-300ms          │     │ - routerAddress           │               │
│  └─────────────────────┘     │ - transactionValue        │               │
│                              └──────────────┬────────────┘               │
│                                             │                             │
│                                             ▼                             │
│  ┌─────────────────────┐     ┌───────────────────────────┐               │
│  │ Send Transaction    │────▶│ Blockchain Confirmation   │               │
│  │ (Privy wallet sign) │     │ ~3-15s (network dependent)│               │
│  └─────────────────────┘     └───────────────────────────┘               │
│                                                                           │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.2 API Endpoints

| Endpoint | Method | Purpose | Latency |
|----------|--------|---------|---------|
| `/{chain}/api/v1/routes` | GET | Get optimal swap route + quote | 200-500ms |
| `/{chain}/api/v1/route/build` | POST | Encode route for execution | 100-300ms |

### 3.3 Key Parameters

**GET /routes Required:**
- `tokenIn` - Input token address (ETH: `0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE`)
- `tokenOut` - Output token address
- `amountIn` - Amount in wei
- `x-client-id` header - **CRITICAL for rate limiting**

**POST /route/build Required:**
- `routeSummary` - Exact object from GET response
- `sender` - User wallet address
- `recipient` - User wallet address (same for self-swap)
- `slippageTolerance` - BPS (10 = 0.1%)

---

## 4. Critical Path Analysis

### 4.1 Current Timeline Breakdown

| Phase | Action | Est. Time |
|-------|--------|-----------|
| 1 | App cold start | ~600ms |
| 2 | Auth check + wallet ready | ~200ms |
| 3 | Token list display | ~0ms (cached) |
| 4 | User selects token | Variable (user) |
| 5 | GET route quote | 200-500ms |
| 6 | User confirms swap | Variable (user) |
| 7 | POST encode route | 100-300ms |
| 8 | Token approval (if needed) | 3-10s |
| 9 | Sign + broadcast tx | 100-300ms |
| 10 | Tx confirmation | 3-15s |

**Total (w/o user interaction)**: ~4-27s depending on approval + confirmation

### 4.2 Optimization Opportunities

| Optimization | Time Saved | Feasibility |
|--------------|------------|-------------|
| Pre-approve router for common tokens | 3-10s | HIGH |
| Background quote refresh | 200-500ms per swap | HIGH |
| Parallel API calls | 100-300ms | MEDIUM |
| Skip confirmation UI | 1-2s user delay | MEDIUM |
| Use faster L2 chains | 5-10s confirmation | HIGH |

---

## 5. Network Strategy

### 5.1 Recommended Chain: Base

| Factor | Ethereum | Base | Polygon |
|--------|----------|------|---------|
| Tx confirmation | 12-15s | 2-3s | 2-5s |
| Gas cost | $5-50+ | $0.01-0.10 | $0.01-0.50 |
| DEX liquidity | Highest | Good | Good |
| KyberSwap support | Yes | Yes | Yes |
| User familiarity | High | Medium | High |

**Recommendation**: **Base** as primary chain for demo
- Fast confirmation (~2s)
- Low gas ($0.01-0.10)
- Good liquidity for meme tokens
- Growing ecosystem

### 5.2 Fallback Strategy

```
Primary: Base (chainId: 8453)
    ↓ if route not found
Fallback 1: Arbitrum (chainId: 42161)
    ↓ if route not found
Fallback 2: Ethereum (chainId: 1)
```

### 5.3 Native Token Address

```kotlin
const val NATIVE_TOKEN = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"
// Works for ETH on all EVM chains
```

---

## 6. Architecture Recommendation

### 6.1 Component Design

```
┌────────────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                              │
├────────────────────────────────────────────────────────────────────────┤
│  SwapScreen                  SwapViewModel                              │
│  ├─ QuoteDisplay             ├─ SwapState                               │
│  ├─ SlippageSettings         ├─ SwapEvent (GetQuote, ExecuteSwap)       │
│  ├─ SwapButton               └─ SwapEffect (Success, Error, Pending)    │
│  └─ TxStatusDialog                                                      │
├────────────────────────────────────────────────────────────────────────┤
│                           Domain Layer                                  │
├────────────────────────────────────────────────────────────────────────┤
│  GetSwapQuoteUseCase         ExecuteSwapUseCase                         │
│  ├─ Input: tokenIn/Out,      ├─ Input: routeSummary, wallet             │
│  │   amountIn               ├─ Output: txHash, status                  │
│  └─ Output: Quote            └─ Side effects: sign + broadcast          │
│                                                                         │
│  SwapRepository (interface)                                             │
│  ├─ getQuote(...)                                                       │
│  ├─ buildRoute(...)                                                     │
│  └─ checkAllowance(...)                                                 │
├────────────────────────────────────────────────────────────────────────┤
│                            Data Layer                                   │
├────────────────────────────────────────────────────────────────────────┤
│  SwapRepositoryImpl           KyberSwapApiService                       │
│  ├─ Quote caching             ├─ GET /routes                            │
│  ├─ Route encoding            ├─ POST /route/build                      │
│  └─ Error handling            └─ Headers: x-client-id                   │
│                                                                         │
│  SwapDtos                     SwapMapper                                │
│  ├─ RouteResponseDto          ├─ RouteSummary → Quote                   │
│  ├─ BuildRouteRequestDto      └─ BuildResponse → EncodedSwap            │
│  └─ BuildRouteResponseDto                                               │
└────────────────────────────────────────────────────────────────────────┘
```

### 6.2 File Structure

```
app/src/main/java/com/otistran/flash_trade/
├── data/
│   ├── remote/
│   │   ├── kyberswap/
│   │   │   ├── KyberSwapApiService.kt        # Retrofit interface
│   │   │   ├── dto/
│   │   │   │   ├── RouteResponseDto.kt       # GET /routes response
│   │   │   │   ├── BuildRouteRequestDto.kt   # POST /route/build body
│   │   │   │   └── BuildRouteResponseDto.kt  # POST response
│   │   │   └── mapper/
│   │   │       └── SwapMapper.kt             # DTO → Domain
│   └── repository/
│       └── SwapRepositoryImpl.kt             # Repository implementation
├── domain/
│   ├── model/
│   │   ├── Quote.kt                          # Quote domain model
│   │   └── SwapResult.kt                     # Swap execution result
│   ├── repository/
│   │   └── SwapRepository.kt                 # Repository interface
│   └── usecase/
│       ├── GetSwapQuoteUseCase.kt            # Quote business logic
│       └── ExecuteSwapUseCase.kt             # Swap execution logic
├── presentation/
│   └── feature/
│       └── swap/
│           ├── SwapScreen.kt                 # Compose UI
│           ├── SwapViewModel.kt              # MVI ViewModel
│           ├── SwapState.kt                  # UI State
│           ├── SwapEvent.kt                  # User events
│           └── SwapEffect.kt                 # Side effects
└── di/
    └── SwapModule.kt                         # Hilt bindings
```

---

## 7. Swap Execution Flow

### 7.1 Happy Path Sequence

```
User taps token → SwapScreen shown
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ PHASE 1: Get Quote (background, immediate on screen open)               │
├──────────────────────────────────────────────────────────────────────────┤
│ 1. SwapViewModel.onEvent(GetQuote(tokenIn, tokenOut, amountIn))         │
│ 2. GetSwapQuoteUseCase.invoke()                                          │
│ 3. SwapRepository.getQuote() → GET /routes                               │
│ 4. Parse routeSummary → Quote domain model                               │
│ 5. Update SwapState.quote                                                │
│ 6. Start 5s auto-refresh timer                                           │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼ (User sees quote, taps "Swap")
       │
┌──────────────────────────────────────────────────────────────────────────┐
│ PHASE 2: Check Allowance                                                 │
├──────────────────────────────────────────────────────────────────────────┤
│ 1. If tokenIn != NATIVE_TOKEN                                            │
│ 2. Check ERC20.allowance(wallet, routerAddress)                          │
│ 3. If allowance < amountIn:                                              │
│    a. Show approval UI                                                   │
│    b. ERC20.approve(routerAddress, MAX_UINT256)                          │
│    c. Wait for approval tx confirmation                                  │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ PHASE 3: Build + Execute Swap                                            │
├──────────────────────────────────────────────────────────────────────────┤
│ 1. SwapRepository.buildRoute() → POST /route/build                       │
│    - routeSummary from quote                                             │
│    - sender = recipient = wallet address                                 │
│    - slippageTolerance = 50 (0.5%)                                       │
│ 2. Get encoded calldata + transactionValue                               │
│ 3. Privy wallet.sendTransaction({                                        │
│       to: routerAddress,                                                 │
│       data: encodedData,                                                 │
│       value: transactionValue (if swapping native)                       │
│    })                                                                    │
│ 4. Wait for tx confirmation                                              │
│ 5. Show success UI with tx hash                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Error Handling

| Error Code | Meaning | User Action |
|------------|---------|-------------|
| 4008 | Route not found | Show "No liquidity" message |
| 4009 | Amount too large | Reduce amount |
| 4011 | Token not found | Token not supported |
| Rate limit | Too many requests | Wait + retry |
| Tx reverted | Slippage exceeded | Increase slippage, retry |

---

## 8. Performance Optimizations

### 8.1 Prefetch Strategy

```kotlin
// In TradingViewModel, when user selects token
fun onTokenSelected(token: Token) {
    // Start quote fetch immediately (don't wait for SwapScreen)
    viewModelScope.launch {
        val defaultAmount = "1000000000000000000" // 1 token
        val quote = swapRepository.getQuote(
            tokenIn = NATIVE_TOKEN, // ETH/native
            tokenOut = token.address,
            amountIn = defaultAmount
        )
        // Cache for SwapScreen
        _prefetchedQuote.value = quote
    }
}
```

### 8.2 Quote Caching Rules

| Rule | Value | Rationale |
|------|-------|-----------|
| Cache TTL | 5 seconds | KyberSwap recommendation |
| Refresh strategy | Every 5s while SwapScreen visible | Keep quote fresh |
| Stale tolerance | 10 seconds | Allow slightly stale for faster UX |

### 8.3 OkHttp Optimization

```kotlin
// In NetworkModule.kt
@Provides
@Named("kyberSwapClient")
fun provideKyberSwapOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS) // Reduce from 30s
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("x-client-id", BuildConfig.KYBER_CLIENT_ID)
                    .header("Accept", "application/json")
                    .build()
            )
        }
        .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
        .build()
}
```

### 8.4 Parallel Initialization

```kotlin
// In AppStartupManager or SplashViewModel
suspend fun initializeForTrading() = coroutineScope {
    // Run in parallel
    val tokenListJob = async { tokenRepository.prefetchTokens() }
    val walletJob = async { privyService.ensureWallet() }
    val gasJob = async { fetchGasPrice() } // For quote accuracy

    // Wait for all
    awaitAll(tokenListJob, walletJob, gasJob)
}
```

---

## 9. Token Approval Strategy

### 9.1 Approval Options

| Strategy | Pros | Cons | Recommendation |
|----------|------|------|----------------|
| Approve exact amount | Most secure | Extra tx per swap | For large amounts |
| Approve MAX_UINT256 | One-time, fast swaps | Less secure | For demo speed |
| Permit signature | No approval tx | Not all tokens support | Future enhancement |

**Demo Recommendation**: MAX_UINT256 approval for speed, with clear user disclosure

### 9.2 Pre-Approval Flow

```kotlin
// Check on SwapScreen open
val allowance = erc20Contract.allowance(wallet, routerAddress)
if (allowance < amountIn) {
    // Show approval needed indicator
    state = state.copy(needsApproval = true)
}

// When user approves
suspend fun approveToken(token: String, routerAddress: String) {
    val tx = erc20Contract.approve(routerAddress, MAX_UINT256)
    waitForConfirmation(tx)
}
```

---

## 10. Risk Assessment

### 10.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Rate limiting | Medium | High | Use clientId, implement backoff |
| Quote expiry | Medium | Medium | 5s refresh, stale warning |
| Tx revert | Medium | High | Gas estimation, slippage buffer |
| Network congestion | Low | High | L2 chain, higher gas price |
| Privy SDK issues | Low | Critical | Fallback UI, error states |

### 10.2 Business Risks

| Risk | Mitigation |
|------|------------|
| User loses funds | Clear slippage warnings, tx simulation |
| Demo fails | Pre-test on Base/Arbitrum, backup plan |
| Poor UX | Loading states, optimistic UI |

---

## 11. Implementation Phases

### Phase 1: Core Integration (Day 1-2)
- [ ] Create KyberSwapApiService Retrofit interface
- [ ] Implement DTOs for route/build endpoints
- [ ] Create SwapRepository + SwapRepositoryImpl
- [ ] Add Hilt module bindings

### Phase 2: Quote Flow (Day 2-3)
- [ ] Create GetSwapQuoteUseCase
- [ ] Build SwapScreen UI with quote display
- [ ] Implement SwapViewModel with MVI
- [ ] Add quote auto-refresh

### Phase 3: Execution Flow (Day 3-4)
- [ ] Create ExecuteSwapUseCase
- [ ] Implement token approval check/flow
- [ ] Integrate Privy wallet signing
- [ ] Add transaction status tracking

### Phase 4: Optimization (Day 4-5)
- [ ] Implement quote prefetching
- [ ] Add parallel initialization
- [ ] Optimize network layer
- [ ] Add error retry logic

### Phase 5: Polish (Day 5-6)
- [ ] UI refinements
- [ ] Error handling improvements
- [ ] Transaction history
- [ ] Demo recording prep

---

## 12. Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| App open → quote display | < 2s | Timestamp logging |
| Quote → swap complete | < 5s (L2) | Timestamp logging |
| Total flow | < 15s | Video timestamp |
| Swap success rate | > 95% | Success/failure ratio |
| User taps to swap | ≤ 3 | UX audit |

---

## 13. Answers to Key Questions

### A. Pre-loading Strategy
**Yes, pre-fetch swap routes during token selection**. When user taps a token in TradingScreen, immediately fetch quote in background. Cache with 5-10s TTL.

### B. Caching Strategy
- Token list: Aggressive (persist across sessions)
- Quotes: Short-lived (5s TTL)
- Router address: Long-lived (rarely changes)
- Approval status: Check each swap

### C. Parallel Processing
- Token list + wallet init: Parallel on startup
- Quote fetch + gas price: Parallel
- Approval check + quote refresh: Parallel
- Build route + sign: Sequential (dependency)

### D. Real-time Price Updates
5-second polling while SwapScreen visible. Show "Quote refreshing" indicator. Warn if quote is stale (>10s).

### E. Slippage Settings
Default 0.5% (50 bps). Allow user override 0.1%-5%. For meme tokens, suggest 1-3% default.

### F. Wallet Strategy
**In-app Privy wallet** for fastest execution. No WalletConnect delay. Auto-created on signup.

---

## 14. Unresolved Questions

1. **Token Approval UX**: Show as separate step or inline with swap confirmation?
2. **Auto-Sell Implementation**: WorkManager cron or alarm-based?
3. **Portfolio Sync**: After swap, immediate API call or wait for indexer?
4. **Testnet Strategy**: Use Sepolia or skip to Base mainnet for demo?
5. **Error Recovery**: Auto-retry failed swaps or require user action?

---

## 15. Next Steps

1. **Validate API access** - Test KyberSwap endpoints with provided clientId
2. **Create technical spec** - Detailed API contracts and data models
3. **Scaffold implementation** - Create file structure and interfaces
4. **Implement Phase 1** - Core API integration
5. **Daily sync** - Review progress and blockers

---

*Report prepared for Flash Trade KyberSwap integration planning session.*

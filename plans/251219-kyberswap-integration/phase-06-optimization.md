# Phase 6: Optimization

**Duration:** 30 minutes
**Dependencies:** All previous phases
**Risk Level:** Low

---

## Context Links

- **Research:** `research/researcher-01-kyberswap-api.md`
- **Performance Goal:** Sub-15s app open → successful swap
- **Splash Screen:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/splash/`

---

## Overview

Optimize swap flow for sub-15s completion. Implement quote prefetching on token selection, parallel initialization, and transaction status tracking. Add analytics and performance monitoring.

---

## Key Insights

1. **Performance Bottlenecks:**
   - Quote API call (1-2s)
   - Transaction signing (user dependent)
   - Network confirmation (3-10s)
   - Total: ~5-15s for full swap

2. **Optimization Opportunities:**
   - Prefetch quote on token selection (before SwapScreen)
   - Cache popular token pairs
   - Parallel wallet + quote loading
   - Skip approval for native tokens

3. **Analytics Needs:**
   - Swap flow funnel (select → quote → confirm → success)
   - Error tracking (API failures, user rejections)
   - Performance metrics (time to swap)

---

## Requirements

### Functional
- Quote prefetching on token hover/click
- Transaction status polling
- Retry logic for failed swaps
- Popular pair caching

### Non-Functional
- Reduce perceived latency
- <2s quote fetch for cached pairs
- <5s total swap execution
- 99% success rate (excluding user rejection)

---

## Architecture

```
Optimization Layers:

1. Quote Prefetching:
   TradingScreen → token hover → prefetch quote → cache

2. Parallel Loading:
   App Start → [Privy Init || Token List || Popular Quotes]

3. Transaction Tracking:
   executeSwap() → poll tx status → update UI

4. Popular Pair Cache:
   Preload ETH/USDC, ETH/WBTC quotes on app start
```

---

## Related Code Files

**Existing (to modify):**
1. `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingScreen.kt`
   - Add quote prefetching on token hover

2. `app/src/main/java/com/otistran/flash_trade/domain/manager/QuoteCacheManager.kt`
   - Add popular pair preloading

3. `app/src/main/java/com/otistran/flash_trade/data/repository/SwapRepositoryImpl.kt`
   - Add transaction status polling

**New files:**
4. `app/src/main/java/com/otistran/flash_trade/domain/manager/SwapAnalyticsManager.kt`

---

## Implementation Steps

### Step 1: Add Quote Prefetching (10min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingScreen.kt`

Add hover/long-press handler:

```kotlin
@Composable
fun TokenItem(
    token: Token,
    onClick: (Token) -> Unit,
    onPrefetchQuote: (Token) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick(token) },
                onLongClick = { onPrefetchQuote(token) }
            )
    ) {
        // ... token UI
    }
}

// In TradingViewModel
fun prefetchQuote(token: Token) {
    viewModelScope.launch {
        // Prefetch quote for default amount (e.g., 0.1 ETH)
        getSwapQuoteUseCase(
            chain = "base",
            tokenIn = "0xEeee...EEEE", // Native token
            tokenOut = token.address,
            amountIn = BigInteger("100000000000000000"), // 0.1 ETH
            userAddress = currentUserAddress
        )
        // Result cached automatically
    }
}
```

### Step 2: Popular Pair Preloading (8min)

**File:** `app/src/main/java/com/otistran/flash_trade/domain/manager/QuoteCacheManager.kt`

Add preload method:

```kotlin
/**
 * Preload popular trading pairs.
 */
suspend fun preloadPopularPairs(
    getQuoteUseCase: GetSwapQuoteUseCase,
    userAddress: String?
) {
    val popularPairs = listOf(
        Pair("0xEeee...EEEE", "0xUSDC_ADDRESS"), // ETH → USDC
        Pair("0xEeee...EEEE", "0xWBTC_ADDRESS"), // ETH → WBTC
        // Add more popular pairs
    )

    popularPairs.forEach { (tokenIn, tokenOut) ->
        try {
            getQuoteUseCase(
                chain = "base",
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = BigInteger("100000000000000000"), // 0.1 ETH
                userAddress = userAddress
            )
        } catch (e: Exception) {
            // Ignore preload failures
        }
    }
}
```

Call in SplashScreen or AppStartupManager:

```kotlin
// In SplashViewModel or AppStartupManager
viewModelScope.launch {
    val user = privyAuthService.getUser()
    val address = user?.embeddedEthereumWallets?.firstOrNull()?.address
    quoteCacheManager.preloadPopularPairs(getSwapQuoteUseCase, address)
}
```

### Step 3: Transaction Status Tracking (10min)

**File:** `app/src/main/java/com/otistran/flash_trade/data/repository/SwapRepositoryImpl.kt`

Add polling method:

```kotlin
/**
 * Poll transaction status until confirmed or timeout.
 * @param txHash Transaction hash
 * @param maxAttempts Maximum polling attempts (default 20 = ~60s)
 * @param delayMs Delay between attempts (default 3000ms)
 */
suspend fun pollTransactionStatus(
    txHash: String,
    maxAttempts: Int = 20,
    delayMs: Long = 3000L
): Result<SwapStatus> {
    repeat(maxAttempts) { attempt ->
        try {
            val user = privyAuthService.getUser()
                ?: return Result.Error("User not authenticated")

            val wallet = privyAuthService.ensureEthereumWallet(user).getOrNull()
                ?: return Result.Error("Failed to get wallet")

            // Query transaction receipt
            val receiptRequest = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_getTransactionReceipt")
                put("params", JSONArray().put(txHash))
                put("id", 1)
            }.toString()

            val result = wallet.provider.request(
                request = EthereumRpcRequest(
                    method = "eth_getTransactionReceipt",
                    params = arrayOf(txHash)
                )
            )

            when {
                result.isSuccess -> {
                    val receipt = result.getOrNull()?.data
                    if (receipt != null && receipt != "null") {
                        // Parse receipt status
                        val receiptJson = JSONObject(receipt)
                        val status = receiptJson.optString("status", "0x0")
                        return if (status == "0x1") {
                            Result.Success(SwapStatus.CONFIRMED)
                        } else {
                            Result.Success(SwapStatus.FAILED)
                        }
                    }
                    // Still pending, continue polling
                }
                result.isFailure -> {
                    Log.w(TAG, "Receipt query failed, attempt ${attempt + 1}")
                }
            }

            delay(delayMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error polling tx status", e)
        }
    }

    return Result.Error("Transaction confirmation timeout")
}
```

Update executeSwap to poll status:

```kotlin
override suspend fun executeSwap(
    encodedSwap: EncodedSwap
): Result<SwapResult> {
    // ... existing code to submit transaction ...

    when {
        result.isSuccess -> {
            val txHash = result.getOrNull()?.data ?: ""
            Log.d(TAG, "Swap submitted: $txHash")

            // Start polling in background
            viewModelScope.launch {
                val status = pollTransactionStatus(txHash)
                // Update SwapResult status
            }

            Result.Success(
                SwapResult(
                    txHash = txHash,
                    status = SwapStatus.PENDING
                )
            )
        }
        // ... error handling ...
    }
}
```

### Step 4: Create Analytics Manager (Optional, 2min)

**File:** `app/src/main/java/com/otistran/flash_trade/domain/manager/SwapAnalyticsManager.kt`

```kotlin
package com.otistran.flash_trade.domain.manager

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwapAnalyticsManager @Inject constructor() {

    fun trackSwapStarted(tokenIn: String, tokenOut: String, amount: String) {
        // Log to Firebase Analytics or similar
    }

    fun trackQuoteFetched(durationMs: Long, success: Boolean) {
        // Track quote fetch performance
    }

    fun trackSwapCompleted(txHash: String, durationMs: Long) {
        // Track successful swap
    }

    fun trackSwapFailed(error: String) {
        // Track swap failure reasons
    }
}
```

---

## Todo List

- [ ] Add quote prefetching on token hover/long-press
- [ ] Implement popular pair preloading in QuoteCacheManager
- [ ] Add transaction status polling method
- [ ] Update executeSwap to poll tx status in background
- [ ] Create SwapAnalyticsManager (optional)
- [ ] Add analytics tracking to swap flow
- [ ] Test quote prefetching reduces perceived latency
- [ ] Verify popular pair cache hits
- [ ] Test tx status polling with real swaps
- [ ] Measure end-to-end swap time

---

## Success Criteria

- [ ] Quote prefetching works on token hover
- [ ] Popular pairs cached on app start
- [ ] Transaction status updates after confirmation
- [ ] Sub-15s total swap time (select → confirm)
- [ ] <2s quote fetch for cached pairs
- [ ] No performance regressions

---

## Risk Assessment

**Low Risk:**
- Prefetching is optional enhancement
- Polling can be disabled if issues arise

**Potential Issues:**
1. **Prefetch cost:** May increase API usage
2. **Polling timeout:** 60s may not be enough for slow networks
3. **Cache size:** Too many cached quotes may use memory
4. **Race conditions:** Quote may expire before user confirms

---

## Security Considerations

1. **Rate limiting:** Prefetching should respect API limits
2. **Cache poisoning:** Validate cached quotes before use
3. **Status polling:** Verify txHash format before polling

---

## Performance Metrics

**Before Optimization:**
- Token selection → quote display: ~2s
- Swap execution → confirmation: ~10s
- Total: ~12s

**After Optimization:**
- Token selection → quote display: <1s (cached)
- Swap execution → confirmation: ~8s (parallel)
- Total: <10s

**Target:**
- App open → first swap: <15s
- Quote fetch (cached): <500ms
- Transaction confirmation: <10s

---

## Next Steps

After completion:
1. Run end-to-end performance tests
2. Monitor analytics for bottlenecks
3. Consider additional optimizations:
   - WebSocket for real-time quotes
   - Service worker for background prefetching
   - Optimistic UI updates

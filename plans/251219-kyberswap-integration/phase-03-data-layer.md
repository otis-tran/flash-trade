# Phase 3: Data Layer Implementation

**Duration:** 45 minutes
**Dependencies:** Phase 1 (API), Phase 2 (Domain)
**Risk Level:** Medium

---

## Context Links

- **Research:** `research/researcher-01-kyberswap-api.md`, `research/researcher-02-privy-wallet.md`
- **Existing Repository:** `app/src/main/java/com/otistran/flash_trade/data/repository/TokenRepositoryImpl.kt`
- **Privy Service:** `app/src/main/java/com/otistran/flash_trade/data/service/PrivyAuthService.kt`
- **Network:** `app/src/main/java/com/otistran/flash_trade/core/network/SafeApiCall.kt`

---

## Overview

Implement SwapRepository with quote caching, ERC20 approval handling, and Privy wallet integration. Create mapper to convert API DTOs to domain models. Add quote cache manager for 5s TTL.

---

## Key Insights

1. **Existing Infrastructure:**
   - SafeApiCall wrapper for error handling
   - PrivyAuthService provides wallet access
   - Token approval via eth_sendTransaction
   - Result wrapper for success/error states

2. **Quote Caching:**
   - 5s TTL to reduce API calls
   - Cache by tokenIn+tokenOut+amountIn key
   - Invalidate on successful swap

3. **Privy Integration:**
   - Get wallet via ensureEthereumWallet()
   - Sign tx via wallet.provider.request(ethSendTransaction)
   - Return tx hash or error

4. **Approval Flow:**
   - Check current allowance via eth_call
   - If insufficient, request approval tx
   - Use MAX_UINT256 for unlimited approval
   - Wait for approval confirmation before swap

---

## Requirements

### Functional
- Cache quotes with 5s TTL
- Check/execute ERC20 approvals
- Build transaction with Privy wallet
- Sign and broadcast transaction
- Handle user rejection gracefully

### Non-Functional
- Thread-safe cache (synchronized map)
- Retry logic for transient failures (3 attempts)
- Timeout: 30s for API, 60s for tx confirmation
- Gas estimation with 20% safety margin

---

## Architecture

```
SwapRepositoryImpl
    ├── getQuote() → [QuoteCacheManager] → [KyberApiService]
    ├── buildSwap() → [KyberApiService] → [SwapMapper]
    └── executeSwap() → [PrivyAuthService] → wallet.provider.request()

QuoteCacheManager
    ├── get(key) → Quote?
    ├── put(key, quote, ttl)
    └── invalidate(key)

SwapMapper
    ├── toQuote(RouteResponseDto) → Quote
    └── toEncodedSwap(BuildRouteResponseDto) → EncodedSwap
```

---

## Related Code Files

**Existing (for reference):**
1. `app/src/main/java/com/otistran/flash_trade/data/repository/TokenRepositoryImpl.kt`
2. `app/src/main/java/com/otistran/flash_trade/data/service/PrivyAuthService.kt`
3. `app/src/main/java/com/otistran/flash_trade/core/network/SafeApiCall.kt`
4. `app/src/main/java/com/otistran/flash_trade/data/mapper/TokenMapper.kt`

**New files:**
5. `app/src/main/java/com/otistran/flash_trade/data/repository/SwapRepositoryImpl.kt`
6. `app/src/main/java/com/otistran/flash_trade/data/mapper/SwapMapper.kt`
7. `app/src/main/java/com/otistran/flash_trade/domain/manager/QuoteCacheManager.kt`

---

## Implementation Steps

### Step 1: Create QuoteCacheManager (10min)

**File:** `app/src/main/java/com/otistran/flash_trade/domain/manager/QuoteCacheManager.kt`

```kotlin
package com.otistran.flash_trade.domain.manager

import com.otistran.flash_trade.domain.model.Quote
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe cache for swap quotes with TTL.
 */
@Singleton
class QuoteCacheManager @Inject constructor() {

    private data class CachedQuote(
        val quote: Quote,
        val expiresAt: Long
    )

    private val cache = ConcurrentHashMap<String, CachedQuote>()

    /**
     * Get cached quote if not expired.
     */
    fun get(tokenIn: String, tokenOut: String, amountIn: String): Quote? {
        val key = createKey(tokenIn, tokenOut, amountIn)
        val cached = cache[key] ?: return null

        val now = System.currentTimeMillis() / 1000
        return if (now < cached.expiresAt) {
            cached.quote
        } else {
            cache.remove(key)
            null
        }
    }

    /**
     * Put quote in cache with 5s TTL.
     */
    fun put(quote: Quote) {
        val key = createKey(quote.tokenIn, quote.tokenOut, quote.amountIn.toString())
        val expiresAt = System.currentTimeMillis() / 1000 + 5 // 5s TTL
        cache[key] = CachedQuote(quote, expiresAt)
    }

    /**
     * Invalidate cached quote.
     */
    fun invalidate(tokenIn: String, tokenOut: String, amountIn: String) {
        val key = createKey(tokenIn, tokenOut, amountIn)
        cache.remove(key)
    }

    /**
     * Clear all cached quotes.
     */
    fun clear() {
        cache.clear()
    }

    private fun createKey(tokenIn: String, tokenOut: String, amountIn: String): String {
        return "${tokenIn}_${tokenOut}_$amountIn"
    }
}
```

### Step 2: Create SwapMapper (12min)

**File:** `app/src/main/java/com/otistran/flash_trade/data/mapper/SwapMapper.kt`

```kotlin
package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.data.remote.dto.kyber.BuildRouteResponseDto
import com.otistran.flash_trade.data.remote.dto.kyber.RouteResponseDto
import com.otistran.flash_trade.domain.model.EncodedSwap
import com.otistran.flash_trade.domain.model.Quote
import java.math.BigInteger
import javax.inject.Inject

/**
 * Mapper for swap DTOs to domain models.
 */
class SwapMapper @Inject constructor() {

    /**
     * Map RouteResponseDto to Quote domain model.
     */
    fun toQuote(dto: RouteResponseDto): Quote {
        val summary = dto.data.routeSummary
        return Quote(
            tokenIn = summary.tokenIn,
            tokenOut = summary.tokenOut,
            amountIn = summary.amountIn.toBigInteger(),
            amountOut = summary.amountOut.toBigInteger(),
            amountOutUsd = summary.amountOutUsd,
            gas = summary.gas.toBigInteger(),
            gasUsd = summary.gasUsd,
            routerAddress = dto.data.routerAddress,
            routeId = summary.routeID,
            timestamp = summary.timestamp.toLong()
        )
    }

    /**
     * Map BuildRouteResponseDto to EncodedSwap domain model.
     */
    fun toEncodedSwap(dto: BuildRouteResponseDto): EncodedSwap {
        val data = dto.data
        return EncodedSwap(
            calldata = data.data,
            routerAddress = data.routerAddress,
            value = data.transactionValue.toBigIntegerOrZero(),
            gas = data.gas.toBigInteger(),
            amountOut = data.amountOut.toBigInteger()
        )
    }

    private fun String.toBigIntegerOrZero(): BigInteger {
        return try {
            if (this.startsWith("0x")) {
                BigInteger(this.substring(2), 16)
            } else {
                BigInteger(this)
            }
        } catch (e: Exception) {
            BigInteger.ZERO
        }
    }
}
```

### Step 3: Create SwapRepositoryImpl (23min)

**File:** `app/src/main/java/com/otistran/flash_trade/data/repository/SwapRepositoryImpl.kt`

```kotlin
package com.otistran.flash_trade.data.repository

import android.util.Log
import com.otistran.flash_trade.data.mapper.SwapMapper
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.data.remote.dto.kyber.BuildRouteRequestDto
import com.otistran.flash_trade.data.remote.dto.kyber.RouteSummaryDto
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.manager.QuoteCacheManager
import com.otistran.flash_trade.domain.model.EncodedSwap
import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.model.SwapResult
import com.otistran.flash_trade.domain.model.SwapStatus
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.util.Result
import io.privy.wallet.ethereum.EthereumRpcRequest
import org.json.JSONObject
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "SwapRepositoryImpl"

class SwapRepositoryImpl @Inject constructor(
    @Named("kyber") private val kyberApi: KyberApiService,
    private val privyAuthService: PrivyAuthService,
    private val swapMapper: SwapMapper,
    private val quoteCacheManager: QuoteCacheManager
) : SwapRepository {

    override suspend fun getQuote(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger,
        slippageTolerance: Int,
        userAddress: String?
    ): Result<Quote> {
        return try {
            // Check cache first
            val cached = quoteCacheManager.get(tokenIn, tokenOut, amountIn.toString())
            if (cached != null) {
                Log.d(TAG, "Quote cache hit")
                return Result.Success(cached)
            }

            // Fetch from API
            val response = kyberApi.getSwapRoute(
                chain = chain,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn.toString(),
                slippageTolerance = slippageTolerance,
                origin = userAddress
            )

            if (response.code != 0) {
                return Result.Error("Failed to get quote: ${response.message}")
            }

            val quote = swapMapper.toQuote(response)
            quoteCacheManager.put(quote)

            Result.Success(quote)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting quote", e)
            Result.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun buildSwap(
        chain: String,
        quote: Quote,
        senderAddress: String,
        recipientAddress: String?
    ): Result<EncodedSwap> {
        return try {
            val request = BuildRouteRequestDto(
                routeSummary = RouteSummaryDto(
                    tokenIn = quote.tokenIn,
                    amountIn = quote.amountIn.toString(),
                    amountInUsd = "0", // Not needed for build
                    tokenOut = quote.tokenOut,
                    amountOut = quote.amountOut.toString(),
                    amountOutUsd = quote.amountOutUsd,
                    gas = quote.gas.toString(),
                    gasPrice = "0", // Not needed for build
                    gasUsd = quote.gasUsd,
                    routeID = quote.routeId,
                    checksum = "", // Not needed for build
                    timestamp = quote.timestamp.toString()
                ),
                sender = senderAddress,
                recipient = recipientAddress ?: senderAddress,
                slippageTolerance = 10,
                enableGasEstimation = true
            )

            val response = kyberApi.buildSwapRoute(chain = chain, request = request)

            if (response.code != 0) {
                return Result.Error("Failed to build swap: ${response.message}")
            }

            val encodedSwap = swapMapper.toEncodedSwap(response)
            Result.Success(encodedSwap)
        } catch (e: Exception) {
            Log.e(TAG, "Error building swap", e)
            Result.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun executeSwap(
        encodedSwap: EncodedSwap
    ): Result<SwapResult> {
        return try {
            // Get user wallet
            val user = privyAuthService.getUser()
                ?: return Result.Error("User not authenticated")

            val walletResult = privyAuthService.ensureEthereumWallet(user)
            if (walletResult.isFailure) {
                return Result.Error("Failed to get wallet: ${walletResult.exceptionOrNull()?.message}")
            }

            val wallet = walletResult.getOrNull()!!

            // Build transaction JSON
            val txRequest = JSONObject().apply {
                put("from", wallet.address)
                put("to", encodedSwap.routerAddress)
                put("data", encodedSwap.calldata)
                put("value", "0x${encodedSwap.value.toString(16)}")
                put("gas", "0x${encodedSwap.gas.toString(16)}")
            }.toString()

            // Send transaction via Privy
            val result = wallet.provider.request(
                request = EthereumRpcRequest.ethSendTransaction(txRequest)
            )

            when {
                result.isSuccess -> {
                    val txHash = result.getOrNull()?.data ?: ""
                    Log.d(TAG, "Swap submitted: $txHash")
                    Result.Success(
                        SwapResult(
                            txHash = txHash,
                            status = SwapStatus.PENDING
                        )
                    )
                }
                result.isFailure -> {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Swap failed", error)
                    Result.Error(error?.message ?: "Transaction rejected")
                }
                else -> Result.Error("Unknown error")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during swap", e)
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
```

---

## Todo List

- [ ] Create QuoteCacheManager with ConcurrentHashMap
- [ ] Implement cache get/put/invalidate methods
- [ ] Create SwapMapper with toQuote method
- [ ] Create SwapMapper with toEncodedSwap method
- [ ] Create SwapRepositoryImpl with getQuote implementation
- [ ] Implement quote caching in getQuote
- [ ] Implement buildSwap with RouteSummaryDto conversion
- [ ] Implement executeSwap with Privy wallet integration
- [ ] Add error handling for all API calls
- [ ] Test with real API responses

---

## Success Criteria

- [ ] Quote caching reduces API calls
- [ ] Cache TTL enforced correctly (5s)
- [ ] Mapper converts DTOs without data loss
- [ ] Privy wallet transaction signing works
- [ ] Error states handled gracefully
- [ ] User rejection returns clear error
- [ ] All files <200 lines

---

## Risk Assessment

**Medium Risk:**
- Privy wallet integration may have edge cases
- Quote cache thread safety must be verified
- BigInteger conversion from hex strings

**Potential Issues:**
1. **Cache key collision:** Use tokenIn+tokenOut+amountIn composite key
2. **Privy timeout:** User may take >60s to approve
3. **Gas estimation failure:** Fallback to manual 300k limit
4. **Hex conversion errors:** Handle 0x prefix correctly

---

## Security Considerations

1. **Private key handling:** Never log private keys (Privy handles this)
2. **Transaction validation:** Verify to/data/value before signing
3. **Approval limits:** Use MAX_UINT256 or exact amount based on user preference
4. **Gas price:** Use network-suggested gas, don't hardcode

---

## Next Steps

After completion:
1. Create SwapModule for Hilt DI
2. Proceed to Phase 4: Presentation Layer
3. Test repository with mock API responses

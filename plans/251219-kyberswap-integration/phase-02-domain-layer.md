# Phase 2: Domain Layer Implementation

**Duration:** 25 minutes
**Dependencies:** Phase 1 (DTOs for reference)
**Risk Level:** Low

---

## Context Links

- **Research:** `research/researcher-01-kyberswap-api.md`
- **Existing Domain:** `app/src/main/java/com/otistran/flash_trade/domain/`
- **Token Model:** `app/src/main/java/com/otistran/flash_trade/domain/model/Token.kt`
- **UseCase Base:** `app/src/main/java/com/otistran/flash_trade/domain/usecase/UseCase.kt`

---

## Overview

Create domain models and business logic for swap operations. Define repository interface as contract between domain and data layers. Create use cases for quote fetching and swap execution following single-responsibility principle.

---

## Key Insights

1. **Domain Independence:**
   - No Android/framework dependencies
   - Pure Kotlin data classes
   - Business rules encapsulated in use cases

2. **Existing Patterns:**
   - UseCase base class with invoke operator
   - Result wrapper for success/error handling
   - Repository interfaces define contracts

3. **Swap Flow:**
   - GetSwapQuoteUseCase: Fetch quote + validate
   - ExecuteSwapUseCase: Check approval + build + sign + broadcast

---

## Requirements

### Functional
- Quote model with price, gas, and route info
- EncodedSwap model with transaction data
- SwapResult model with tx hash and status
- Repository interface for data access
- Use cases for quote and execution

### Non-Functional
- Immutable domain models
- Type-safe amount handling (BigInteger for wei)
- Clear error types
- Quote expiration tracking (timestamp)

---

## Architecture

```
Domain Models:
    ├── Quote (price, gas, amountOut, expiration)
    ├── EncodedSwap (calldata, routerAddress, value)
    └── SwapResult (txHash, status, timestamp)

Repository Interface:
    └── SwapRepository
        ├── getQuote()
        ├── buildSwap()
        └── executeSwap()

Use Cases:
    ├── GetSwapQuoteUseCase (domain validation)
    └── ExecuteSwapUseCase (orchestrates approval + swap)
```

---

## Related Code Files

**Existing (for reference):**
1. `app/src/main/java/com/otistran/flash_trade/domain/model/Token.kt`
2. `app/src/main/java/com/otistran/flash_trade/domain/usecase/UseCase.kt`
3. `app/src/main/java/com/otistran/flash_trade/util/Result.kt`

**New files:**
4. `app/src/main/java/com/otistran/flash_trade/domain/model/Quote.kt`
5. `app/src/main/java/com/otistran/flash_trade/domain/model/EncodedSwap.kt`
6. `app/src/main/java/com/otistran/flash_trade/domain/model/SwapResult.kt`
7. `app/src/main/java/com/otistran/flash_trade/domain/repository/SwapRepository.kt`
8. `app/src/main/java/com/otistran/flash_trade/domain/usecase/swap/GetSwapQuoteUseCase.kt`
9. `app/src/main/java/com/otistran/flash_trade/domain/usecase/swap/ExecuteSwapUseCase.kt`

---

## Implementation Steps

### Step 1: Create Quote Model (5min)

**File:** `app/src/main/java/com/otistran/flash_trade/domain/model/Quote.kt`

```kotlin
package com.otistran.flash_trade.domain.model

import java.math.BigInteger

/**
 * Swap quote with pricing and routing information.
 * @param tokenIn Input token address
 * @param tokenOut Output token address
 * @param amountIn Input amount in wei
 * @param amountOut Expected output amount in wei
 * @param amountOutUsd Output value in USD
 * @param gas Estimated gas units
 * @param gasUsd Gas cost in USD
 * @param routerAddress KyberSwap router contract address
 * @param routeId Unique route identifier
 * @param timestamp Quote creation timestamp (Unix seconds)
 */
data class Quote(
    val tokenIn: String,
    val tokenOut: String,
    val amountIn: BigInteger,
    val amountOut: BigInteger,
    val amountOutUsd: String,
    val gas: BigInteger,
    val gasUsd: String,
    val routerAddress: String,
    val routeId: String,
    val timestamp: Long
) {
    /**
     * Check if quote is expired (5s TTL).
     */
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis() / 1000
        return (now - timestamp) > 5
    }

    /**
     * Get price impact as percentage.
     */
    fun getPriceImpact(): Double {
        // Placeholder - calculate from route data
        return 0.0
    }
}
```

### Step 2: Create EncodedSwap Model (4min)

**File:** `app/src/main/java/com/otistran/flash_trade/domain/model/EncodedSwap.kt`

```kotlin
package com.otistran.flash_trade.domain.model

import java.math.BigInteger

/**
 * Encoded swap transaction ready for signing.
 * @param calldata Hex-encoded contract call data
 * @param routerAddress Target contract address
 * @param value Native token value to send (wei)
 * @param gas Estimated gas limit
 * @param amountOut Expected output amount
 */
data class EncodedSwap(
    val calldata: String,
    val routerAddress: String,
    val value: BigInteger,
    val gas: BigInteger,
    val amountOut: BigInteger
)
```

### Step 3: Create SwapResult Model (4min)

**File:** `app/src/main/java/com/otistran/flash_trade/domain/model/SwapResult.kt`

```kotlin
package com.otistran.flash_trade.domain.model

/**
 * Result of swap execution.
 * @param txHash Transaction hash
 * @param status Transaction status
 * @param timestamp Submission timestamp
 */
data class SwapResult(
    val txHash: String,
    val status: SwapStatus,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SwapStatus {
    PENDING,    // Submitted to network
    CONFIRMED,  // Mined and confirmed
    FAILED,     // Reverted or rejected
    CANCELLED   // User cancelled
}
```

### Step 4: Create SwapRepository Interface (5min)

**File:** `app/src/main/java/com/otistran/flash_trade/domain/repository/SwapRepository.kt`

```kotlin
package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.EncodedSwap
import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.model.SwapResult
import com.otistran.flash_trade.util.Result
import java.math.BigInteger

/**
 * Repository for swap operations.
 */
interface SwapRepository {

    /**
     * Get swap quote from KyberSwap Aggregator.
     * @param chain Chain identifier (e.g., "base")
     * @param tokenIn Input token address
     * @param tokenOut Output token address
     * @param amountIn Amount in wei
     * @param slippageTolerance Slippage in bps (default 10 = 0.1%)
     * @param userAddress User wallet address for exclusive pools
     * @return Quote or error
     */
    suspend fun getQuote(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger,
        slippageTolerance: Int = 10,
        userAddress: String? = null
    ): Result<Quote>

    /**
     * Build encoded swap transaction.
     * @param chain Chain identifier
     * @param quote Quote to execute
     * @param senderAddress User wallet address
     * @param recipientAddress Recipient address (default = sender)
     * @return Encoded swap data or error
     */
    suspend fun buildSwap(
        chain: String,
        quote: Quote,
        senderAddress: String,
        recipientAddress: String? = null
    ): Result<EncodedSwap>

    /**
     * Execute swap transaction via Privy wallet.
     * @param encodedSwap Encoded transaction data
     * @return Transaction hash or error
     */
    suspend fun executeSwap(
        encodedSwap: EncodedSwap
    ): Result<SwapResult>
}
```

### Step 5: Create GetSwapQuoteUseCase (3min)

**File:** `app/src/main/java/com/otistran/flash_trade/domain/usecase/swap/GetSwapQuoteUseCase.kt`

```kotlin
package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.util.Result
import java.math.BigInteger
import javax.inject.Inject

/**
 * Get swap quote with validation.
 */
class GetSwapQuoteUseCase @Inject constructor(
    private val swapRepository: SwapRepository
) {
    suspend operator fun invoke(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger,
        slippageTolerance: Int = 10,
        userAddress: String? = null
    ): Result<Quote> {
        // Validate inputs
        if (amountIn <= BigInteger.ZERO) {
            return Result.Error("Amount must be greater than zero")
        }
        if (tokenIn.equals(tokenOut, ignoreCase = true)) {
            return Result.Error("Cannot swap same token")
        }

        return swapRepository.getQuote(
            chain = chain,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            slippageTolerance = slippageTolerance,
            userAddress = userAddress
        )
    }
}
```

### Step 6: Create ExecuteSwapUseCase (4min)

**File:** `app/src/main/java/com/otistran/flash_trade/domain/usecase/swap/ExecuteSwapUseCase.kt`

```kotlin
package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.model.SwapResult
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Execute swap with approval check.
 */
class ExecuteSwapUseCase @Inject constructor(
    private val swapRepository: SwapRepository
) {
    suspend operator fun invoke(
        chain: String,
        quote: Quote,
        senderAddress: String
    ): Result<SwapResult> {
        // Check quote expiration
        if (quote.isExpired()) {
            return Result.Error("Quote expired, please refresh")
        }

        // Build swap transaction
        val buildResult = swapRepository.buildSwap(
            chain = chain,
            quote = quote,
            senderAddress = senderAddress
        )

        if (buildResult is Result.Error) {
            return Result.Error(buildResult.message)
        }

        val encodedSwap = (buildResult as Result.Success).data

        // Execute via Privy wallet
        return swapRepository.executeSwap(encodedSwap)
    }
}
```

---

## Todo List

- [ ] Create Quote model with expiration check
- [ ] Create EncodedSwap model with transaction data
- [ ] Create SwapResult model with SwapStatus enum
- [ ] Create SwapRepository interface with 3 methods
- [ ] Create GetSwapQuoteUseCase with input validation
- [ ] Create ExecuteSwapUseCase with expiration check
- [ ] Verify BigInteger imports for wei amounts
- [ ] Ensure all files <200 lines

---

## Success Criteria

- [ ] Domain models are immutable (val properties)
- [ ] No Android/framework dependencies
- [ ] Repository interface defines clear contracts
- [ ] Use cases have single responsibility
- [ ] Quote expiration logic works correctly
- [ ] Input validation prevents invalid swaps

---

## Risk Assessment

**Low Risk:**
- Simple data classes and interfaces
- No complex business logic
- Follows existing patterns

**Potential Issues:**
1. **BigInteger handling:** Ensure conversion from/to String for JSON
2. **Quote expiration:** 5s TTL may be too short for slow networks
3. **Status tracking:** May need additional states for pending approvals

---

## Security Considerations

1. **Amount validation:** Prevent zero or negative amounts
2. **Token address validation:** Check for valid ERC20 addresses
3. **Quote expiration:** Force refresh to prevent stale pricing
4. **Same token check:** Prevent tokenIn == tokenOut

---

## Next Steps

After completion:
1. Proceed to Phase 3: Data Layer
2. Create SwapRepositoryImpl to implement interface
3. Create mappers to convert DTOs → domain models

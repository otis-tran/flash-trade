# Phase 02: Domain Layer

## Context
- **Parent Plan:** [plan.md](plan.md)
- **Dependencies:** Phase 01 (Result.kt)
- **Docs:** [system-architecture.md](../../docs/system-architecture.md)

## Overview
| Field | Value |
|-------|-------|
| Date | 2024-12-12 |
| Priority | High |
| Implementation Status | Complete |
| Review Status | Complete |

**Description:** Create domain layer with entities, repository interfaces, and base use case.

## Key Insights
- Domain layer = pure Kotlin (NO Android dependencies)
- Repository interfaces define contracts, not implementations
- Use cases encapsulate single business operations

## Requirements
1. Domain entities: Token, Trade, Wallet, User
2. Repository interfaces for data access
3. Base UseCase class for consistent pattern

## Architecture

```
domain/
├── model/
│   ├── Token.kt
│   ├── Trade.kt
│   ├── Wallet.kt
│   └── User.kt
├── repository/
│   ├── TradeRepository.kt
│   ├── WalletRepository.kt
│   └── UserRepository.kt
└── usecase/
    └── UseCase.kt
```

## Implementation Steps

### Step 1: Create Token.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/domain/model/Token.kt`

```kotlin
package com.otistran.flash_trade.domain.model

/**
 * Domain entity representing a tradeable token.
 */
data class Token(
    val address: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val chainId: Int,
    val logoUrl: String? = null,
    val priceUsd: Double = 0.0
)
```

### Step 2: Create Trade.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/domain/model/Trade.kt`

```kotlin
package com.otistran.flash_trade.domain.model

/**
 * Domain entity representing a trade transaction.
 */
data class Trade(
    val id: String,
    val tokenAddress: String,
    val tokenSymbol: String,
    val amount: Double,
    val buyPriceUsd: Double,
    val sellPriceUsd: Double? = null,
    val status: TradeStatus,
    val autoSellTimestamp: Long,
    val createdAt: Long,
    val walletAddress: String
)

enum class TradeStatus {
    PENDING,
    COMPLETED,
    SOLD,
    FAILED
}
```

### Step 3: Create Wallet.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/domain/model/Wallet.kt`

```kotlin
package com.otistran.flash_trade.domain.model

/**
 * Domain entity representing a user's wallet.
 */
data class Wallet(
    val address: String,
    val chainId: Int,
    val balance: Double = 0.0,
    val createdAt: Long
)
```

### Step 4: Create User.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/domain/model/User.kt`

```kotlin
package com.otistran.flash_trade.domain.model

/**
 * Domain entity representing the app user.
 */
data class User(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val walletAddress: String? = null,
    val isOnboarded: Boolean = false
)
```

### Step 5: Create TradeRepository.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/domain/repository/TradeRepository.kt`

```kotlin
package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.Trade
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for trade operations.
 * Implementation in data layer.
 */
interface TradeRepository {
    suspend fun executeTrade(token: Token, amount: Double): Result<Trade>
    suspend fun getTradeById(id: String): Result<Trade>
    fun getAllTrades(): Flow<List<Trade>>
    fun getPendingTrades(): Flow<List<Trade>>
    suspend fun updateTradeStatus(id: String, status: com.otistran.flash_trade.domain.model.TradeStatus): Result<Unit>
}
```

### Step 6: Create WalletRepository.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/domain/repository/WalletRepository.kt`

```kotlin
package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.Wallet
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for wallet operations.
 */
interface WalletRepository {
    suspend fun createWallet(): Result<Wallet>
    suspend fun getWallet(): Result<Wallet?>
    fun observeWallet(): Flow<Wallet?>
    suspend fun refreshBalance(): Result<Double>
}
```

### Step 7: Create UserRepository.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/domain/repository/UserRepository.kt`

```kotlin
package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user operations.
 */
interface UserRepository {
    suspend fun getCurrentUser(): Result<User?>
    fun observeUser(): Flow<User?>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun setOnboarded(onboarded: Boolean): Result<Unit>
}
```

### Step 8: Create UseCase.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/domain/usecase/UseCase.kt`

```kotlin
package com.otistran.flash_trade.domain.usecase

import com.otistran.flash_trade.util.Result

/**
 * Base use case interface for single business operations.
 *
 * Usage:
 * class GetUserUseCase @Inject constructor(
 *     private val repo: UserRepository
 * ) : UseCase<Unit, User> {
 *     override suspend fun invoke(params: Unit): Result<User> = repo.getCurrentUser()
 * }
 */
interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Result<R>
}

/**
 * Use case without parameters.
 */
interface NoParamsUseCase<out R> {
    suspend operator fun invoke(): Result<R>
}

/**
 * Use case that returns Flow.
 */
interface FlowUseCase<in P, out R> {
    operator fun invoke(params: P): kotlinx.coroutines.flow.Flow<R>
}
```

## Todo List
- [x] Create domain/model/ directory
- [x] Create Token.kt
- [x] Create Trade.kt
- [x] Create Wallet.kt
- [x] Create User.kt
- [x] Create domain/repository/ directory
- [x] Create TradeRepository.kt
- [x] Create WalletRepository.kt
- [x] Create UserRepository.kt
- [x] Create domain/usecase/ directory
- [x] Create UseCase.kt
- [x] Verify compilation

## Success Criteria
- [x] All 8 files created
- [x] Each file <200 lines
- [x] No Android imports in domain layer
- [x] Repository interfaces use Result and Flow

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Entity fields may need adjustment | Low | Easy to add fields later |

## Security Considerations
- No sensitive data in domain entities (private keys stored separately)

## Next Steps
→ Phase 03: Data Layer - Local (implements repositories)

# Phase 02: Domain Layer - Portfolio Repository

**Parent Plan**: plan.md
**Dependencies**: Phase 01 (Etherscan API)
**Date**: 2025-12-25
**Priority**: High
**Status**: DONE (2025-12-25)

---

## Overview

Implement repository pattern for portfolio data with parallel fetching via structured concurrency. Create use cases for balance, token holdings, and transaction history that leverage `supervisorScope` for graceful degradation.

---

## Key Insights (from researcher-02-android-performance.md)

- **supervisorScope**: One failed API call (e.g., price oracle) doesn't cancel others (balance, transactions)
- **Structured Concurrency**: Use `viewModelScope` for auto-cancellation when screen closes
- **Parallel Fetching**: Fetch balance + tokens + transactions concurrently with `async/await`
- **Error Handling**: Wrap individual async blocks in try-catch, return default values on failure

---

## Requirements

1. Create `PortfolioRepository` interface with methods for balance, tokens, transactions
2. Implement `PortfolioRepositoryImpl` with EtherscanApiService injection
3. Create 3 use cases: `GetBalanceUseCase`, `GetTokensUseCase`, `GetTransactionsUseCase`
4. Implement parallel data fetching with `supervisorScope`
5. Handle errors gracefully (return cached/default data on failure)

---

## Architecture

```
domain/repository/
└── PortfolioRepository.kt          # Interface

data/repository/
└── PortfolioRepositoryImpl.kt      # Implementation

domain/usecase/
├── GetBalanceUseCase.kt
├── GetTokensUseCase.kt
└── GetTransactionsUseCase.kt

di/
└── PortfolioModule.kt              # DI bindings
```

---

## Related Code Files

- `data/repository/TokenRepositoryImpl.kt` - Reference for repository pattern
- `domain/usecase/UseCase.kt` - Base class for use cases
- `data/remote/api/EtherscanApiService.kt` - API client (Phase 01)
- `core/base/BaseViewModel.kt` - viewModelScope usage

---

## Implementation Steps

### 1. Create PortfolioRepository Interface

```kotlin
// domain/repository/PortfolioRepository.kt
interface PortfolioRepository {
    /**
     * Fetch native ETH balance for given address and network.
     * @return Balance in ETH (not Wei)
     */
    suspend fun getBalance(
        walletAddress: String,
        chainId: Long
    ): Result<Double>

    /**
     * Fetch ERC-20 token holdings from token transfer history.
     * Aggregates tokentx to calculate current balances.
     */
    suspend fun getTokenHoldings(
        walletAddress: String,
        chainId: Long
    ): Result<List<TokenHolding>>

    /**
     * Fetch transaction history (normal + token transfers merged).
     * @param page Pagination (1-indexed)
     * @param pageSize Number of results per page
     */
    suspend fun getTransactions(
        walletAddress: String,
        chainId: Long,
        page: Int = 1,
        pageSize: Int = 100
    ): Result<List<Transaction>>

    /**
     * Fetch all portfolio data in parallel.
     * Uses supervisorScope - partial failures return default values.
     */
    suspend fun getPortfolioData(
        walletAddress: String,
        chainId: Long
    ): PortfolioData
}

data class PortfolioData(
    val balance: Double,
    val tokens: List<TokenHolding>,
    val transactions: List<Transaction>,
    val hasErrors: Boolean = false,
    val errorMessage: String? = null
)
```

### 2. Implement PortfolioRepositoryImpl

```kotlin
// data/repository/PortfolioRepositoryImpl.kt
class PortfolioRepositoryImpl @Inject constructor(
    private val etherscanApi: EtherscanApiService,
    @Named("etherscanApiKey") private val apiKey: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PortfolioRepository {

    override suspend fun getBalance(
        walletAddress: String,
        chainId: Long
    ): Result<Double> = withContext(ioDispatcher) {
        try {
            val response = etherscanApi.getBalance(
                chainId = chainId,
                address = walletAddress,
                apiKey = apiKey
            )

            if (response.status == "1" && response.result != null) {
                val ethBalance = EtherscanMapper.weiToEth(response.result)
                Result.success(ethBalance)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTokenHoldings(
        walletAddress: String,
        chainId: Long
    ): Result<List<TokenHolding>> = withContext(ioDispatcher) {
        try {
            val response = etherscanApi.getTokenTx(
                chainId = chainId,
                address = walletAddress,
                apiKey = apiKey,
                page = 1,
                offset = 1000  // Get recent transfers to calculate balances
            )

            if (response.status == "1" && response.result != null) {
                val holdings = aggregateTokenBalances(response.result, walletAddress)
                Result.success(holdings)
            } else {
                Result.success(emptyList())  // No tokens is not an error
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTransactions(
        walletAddress: String,
        chainId: Long,
        page: Int,
        pageSize: Int
    ): Result<List<Transaction>> = withContext(ioDispatcher) {
        try {
            // Fetch both normal and token transactions in parallel
            val normalTxDeferred = async {
                etherscanApi.getTxList(
                    chainId = chainId,
                    address = walletAddress,
                    page = page,
                    offset = pageSize,
                    apiKey = apiKey
                )
            }

            val tokenTxDeferred = async {
                etherscanApi.getTokenTx(
                    chainId = chainId,
                    address = walletAddress,
                    page = page,
                    offset = pageSize,
                    apiKey = apiKey
                )
            }

            val normalTx = normalTxDeferred.await()
            val tokenTx = tokenTxDeferred.await()

            val allTransactions = mutableListOf<Transaction>()

            normalTx.result?.forEach { allTransactions.add(it.toDomain()) }
            tokenTx.result?.forEach { allTransactions.add(it.toDomain()) }

            // Sort by timestamp descending
            val sorted = allTransactions.sortedByDescending { it.timeStamp }

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPortfolioData(
        walletAddress: String,
        chainId: Long
    ): PortfolioData = supervisorScope {
        val errors = mutableListOf<String>()

        val balanceDeferred = async {
            getBalance(walletAddress, chainId).getOrElse {
                errors.add("Balance: ${it.message}")
                0.0
            }
        }

        val tokensDeferred = async {
            getTokenHoldings(walletAddress, chainId).getOrElse {
                errors.add("Tokens: ${it.message}")
                emptyList()
            }
        }

        val transactionsDeferred = async {
            getTransactions(walletAddress, chainId).getOrElse {
                errors.add("Transactions: ${it.message}")
                emptyList()
            }
        }

        PortfolioData(
            balance = balanceDeferred.await(),
            tokens = tokensDeferred.await(),
            transactions = transactionsDeferred.await(),
            hasErrors = errors.isNotEmpty(),
            errorMessage = errors.joinToString("; ")
        )
    }

    /**
     * Aggregate token transfers to calculate current token balances.
     * Sums incoming - outgoing for each token contract.
     */
    private fun aggregateTokenBalances(
        transfers: List<TokenTxDto>,
        walletAddress: String
    ): List<TokenHolding> {
        val balanceMap = mutableMapOf<String, TokenBalanceAccumulator>()

        transfers.forEach { tx ->
            val contractAddress = tx.contractAddress
            val accumulator = balanceMap.getOrPut(contractAddress) {
                TokenBalanceAccumulator(
                    symbol = tx.tokenSymbol,
                    name = tx.tokenName,
                    decimals = tx.tokenDecimal.toIntOrNull() ?: 18
                )
            }

            val value = tx.value.toBigDecimalOrNull() ?: BigDecimal.ZERO

            when {
                tx.to.equals(walletAddress, ignoreCase = true) -> {
                    accumulator.balance += value
                }
                tx.from.equals(walletAddress, ignoreCase = true) -> {
                    accumulator.balance -= value
                }
            }
        }

        // Convert to TokenHolding, filter zero balances
        return balanceMap.values
            .filter { it.balance > BigDecimal.ZERO }
            .map { acc ->
                val balance = acc.balance.divide(
                    BigDecimal.TEN.pow(acc.decimals)
                ).toDouble()

                TokenHolding(
                    symbol = acc.symbol,
                    name = acc.name,
                    balance = balance,
                    balanceUsd = 0.0,  // TODO: Price oracle in future
                    priceUsd = 0.0,
                    priceChange24h = 0.0
                )
            }
    }
}

private data class TokenBalanceAccumulator(
    val symbol: String,
    val name: String,
    val decimals: Int,
    var balance: BigDecimal = BigDecimal.ZERO
)
```

### 3. Create Use Cases

```kotlin
// domain/usecase/GetBalanceUseCase.kt
class GetBalanceUseCase @Inject constructor(
    private val repository: PortfolioRepository
) {
    suspend operator fun invoke(
        walletAddress: String,
        networkMode: NetworkMode
    ): Result<Double> {
        return repository.getBalance(walletAddress, networkMode.chainId)
    }
}

// domain/usecase/GetTokensUseCase.kt
class GetTokensUseCase @Inject constructor(
    private val repository: PortfolioRepository
) {
    suspend operator fun invoke(
        walletAddress: String,
        networkMode: NetworkMode
    ): Result<List<TokenHolding>> {
        return repository.getTokenHoldings(walletAddress, networkMode.chainId)
    }
}

// domain/usecase/GetTransactionsUseCase.kt
class GetTransactionsUseCase @Inject constructor(
    private val repository: PortfolioRepository
) {
    suspend operator fun invoke(
        walletAddress: String,
        networkMode: NetworkMode,
        page: Int = 1
    ): Result<List<Transaction>> {
        return repository.getTransactions(
            walletAddress,
            networkMode.chainId,
            page
        )
    }
}
```

### 4. Create PortfolioModule for DI

```kotlin
// di/PortfolioModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class PortfolioModule {

    @Binds
    @Singleton
    abstract fun bindPortfolioRepository(
        impl: PortfolioRepositoryImpl
    ): PortfolioRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PortfolioConfigModule {

    @Provides
    @Named("etherscanApiKey")
    fun provideEtherscanApiKey(): String {
        return BuildConfig.ETHERSCAN_API_KEY
    }
}
```

---

## Success Criteria

- [ ] Repository interface compiles without errors
- [ ] Use cases injectable via Hilt in ViewModel
- [ ] Parallel fetching completes in <2s for all 3 data sources
- [ ] supervisorScope prevents cascade failures (one failed call doesn't cancel others)
- [ ] Token balance aggregation correctly calculates holdings from transfers
- [ ] Transactions merged and sorted by timestamp descending

---

## Testing Strategy

```kotlin
@Test
fun `getPortfolioData handles partial failures gracefully`() = runTest {
    // Mock API: balance succeeds, tokens fail, transactions succeed
    val mockApi = mock<EtherscanApiService> {
        onBlocking { getBalance(...) } returns BalanceResponseDto(...)
        onBlocking { getTokenTx(...) } throws IOException("Network error")
        onBlocking { getTxList(...) } returns TxListResponseDto(...)
    }

    val repository = PortfolioRepositoryImpl(mockApi, "key")
    val result = repository.getPortfolioData("0x123", 1L)

    assertTrue(result.hasErrors)
    assertTrue(result.balance > 0.0)  // Balance succeeded
    assertTrue(result.tokens.isEmpty())  // Tokens failed → empty list
    assertTrue(result.transactions.isNotEmpty())  // Transactions succeeded
}

@Test
fun `aggregateTokenBalances calculates correct holdings`() {
    val transfers = listOf(
        TokenTxDto(..., to = "0xUser", value = "1000000000000000000"),  // +1 token
        TokenTxDto(..., from = "0xUser", value = "500000000000000000")  // -0.5 token
    )

    val holdings = repository.aggregateTokenBalances(transfers, "0xUser")

    assertEquals(1, holdings.size)
    assertEquals(0.5, holdings[0].balance, 0.0001)
}
```

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| API calls exceed rate limit (5/sec) | Add exponential backoff, space out parallel calls |
| Token balance calculation wrong | Unit test with real Etherscan data, verify on-chain |
| CancellationException not re-thrown | Review all catch blocks, add `if (e is CancellationException) throw e` |
| Large transaction lists (1000+) cause memory issues | Pagination mandatory, warn users with >500 tx |

---

## Security Considerations

- **Address Validation**: Validate Ethereum address checksum before API calls (use Web3j or custom validator)
- **Sensitive Data**: Portfolio balance is PII - never log wallet addresses or balances
- **API Key Exposure**: Ensure BuildConfig not exposed in release builds (ProGuard obfuscation)

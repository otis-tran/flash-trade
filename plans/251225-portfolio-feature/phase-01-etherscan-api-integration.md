# Phase 01: Data Layer - Etherscan API Integration

**Parent Plan**: plan.md
**Dependencies**: None
**Date**: 2025-12-25
**Priority**: High
**Status**: Pending

---

## Overview

Create Retrofit API service for Etherscan V2 with DTOs, mappers, and network configuration. Establishes foundation for real-time portfolio data fetching across Ethereum and Linea networks.

---

## Key Insights (from researcher-01-etherscan-api.md)

- **Unified V2 Endpoint**: Single `https://api.etherscan.io/v2/api` with `chainid` parameter (mandatory)
- **Chain IDs**: Ethereum=1, Linea=59144 (matches existing `NetworkMode` enum)
- **Rate Limit**: 5 calls/sec default tier
- **Critical Endpoints**:
  - `action=balance` - Native ETH balance (returns Wei as string)
  - `action=tokentx` - ERC-20 transfers (includes token metadata)
  - `action=txlist` - Normal transactions
- **Response Format**: `{status: "1", message: "OK", result: "..."}`

---

## Requirements

1. Create Retrofit interface for 3 Etherscan endpoints (balance, tokentx, txlist)
2. Define DTOs for API responses (BalanceResponse, TokenTxResponse, TxListResponse)
3. Create mappers to convert DTOs → domain models (Transaction, TokenHolding)
4. Add Etherscan API key to `local.properties` (user-provided or use free tier)
5. Configure Retrofit client in `NetworkModule` with logging interceptor

---

## Architecture

```
data/remote/api/
├── EtherscanApiService.kt      # Retrofit interface
└── dto/etherscan/
    ├── BalanceResponseDto.kt
    ├── TokenTxResponseDto.kt
    ├── TxListResponseDto.kt
    └── EtherscanBaseResponse.kt

data/mapper/
└── EtherscanMapper.kt          # DTO → Domain

di/
└── NetworkModule.kt            # Add Etherscan client
```

---

## Related Code Files

- `data/remote/api/KyberApiService.kt` - Reference for Retrofit pattern
- `data/remote/dto/BaseResponse.kt` - Existing response wrapper (may reuse)
- `domain/model/Settings.kt` - NetworkMode enum (chainId source)
- `di/NetworkModule.kt` - DI for Retrofit clients

---

## Implementation Steps

### 1. Add Etherscan Dependency (build.gradle.kts)

```kotlin
// Already have Retrofit 2.11, just verify Moshi is set up
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
```

### 2. Create EtherscanApiService.kt

```kotlin
interface EtherscanApiService {
    @GET("v2/api")
    suspend fun getBalance(
        @Query("chainid") chainId: Long,
        @Query("module") module: String = "account",
        @Query("action") action: String = "balance",
        @Query("address") address: String,
        @Query("tag") tag: String = "latest",
        @Query("apikey") apiKey: String
    ): BalanceResponseDto

    @GET("v2/api")
    suspend fun getTokenTx(
        @Query("chainid") chainId: Long,
        @Query("module") module: String = "account",
        @Query("action") action: String = "tokentx",
        @Query("address") address: String,
        @Query("contractaddress") contractAddress: String? = null,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 100,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String
    ): TokenTxResponseDto

    @GET("v2/api")
    suspend fun getTxList(
        @Query("chainid") chainId: Long,
        @Query("module") module: String = "account",
        @Query("action") action: String = "txlist",
        @Query("address") address: String,
        @Query("startblock") startBlock: Long = 0,
        @Query("endblock") endBlock: Long = 99999999,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 100,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String
    ): TxListResponseDto
}
```

### 3. Create DTOs (data/remote/dto/etherscan/)

```kotlin
// EtherscanBaseResponse.kt
@JsonClass(generateAdapter = true)
data class EtherscanBaseResponse<T>(
    val status: String,       // "1" = success, "0" = error
    val message: String,      // "OK" or error message
    val result: T?
)

// BalanceResponseDto.kt
typealias BalanceResponseDto = EtherscanBaseResponse<String>

// TokenTxDto.kt
@JsonClass(generateAdapter = true)
data class TokenTxDto(
    val hash: String,
    val blockNumber: String,
    val timeStamp: String,
    val from: String,
    val to: String,
    val value: String,
    val tokenName: String,
    val tokenSymbol: String,
    val tokenDecimal: String,
    val contractAddress: String,
    val gas: String,
    val gasPrice: String,
    val gasUsed: String,
    val isError: String         // "0" or "1"
)
typealias TokenTxResponseDto = EtherscanBaseResponse<List<TokenTxDto>>

// TxDto.kt
@JsonClass(generateAdapter = true)
data class TxDto(
    val hash: String,
    val blockNumber: String,
    val timeStamp: String,
    val from: String,
    val to: String,
    val value: String,
    val gas: String,
    val gasPrice: String,
    val gasUsed: String,
    val isError: String
)
typealias TxListResponseDto = EtherscanBaseResponse<List<TxDto>>
```

### 4. Create EtherscanMapper.kt

```kotlin
object EtherscanMapper {
    // Map TokenTxDto to Transaction domain model
    fun TokenTxDto.toDomain(): Transaction = Transaction(
        hash = hash,
        blockNumber = blockNumber,
        timeStamp = timeStamp.toLongOrNull() ?: 0L,
        from = from,
        to = to,
        value = value,
        gas = gas,
        gasPrice = gasPrice,
        gasUsed = gasUsed,
        isError = isError == "1",
        txType = TransactionType.ERC20_TRANSFER,
        tokenSymbol = tokenSymbol,
        tokenName = tokenName,
        tokenDecimal = tokenDecimal.toIntOrNull(),
        contractAddress = contractAddress
    )

    // Map TxDto to Transaction domain model
    fun TxDto.toDomain(): Transaction = Transaction(
        hash = hash,
        blockNumber = blockNumber,
        timeStamp = timeStamp.toLongOrNull() ?: 0L,
        from = from,
        to = to,
        value = value,
        gas = gas,
        gasPrice = gasPrice,
        gasUsed = gasUsed,
        isError = isError == "1",
        txType = TransactionType.TRANSFER
    )

    // Convert Wei (string) to ETH (Double)
    fun weiToEth(weiString: String): Double {
        return weiString.toBigDecimalOrNull()
            ?.divide(BigDecimal("1000000000000000000"))
            ?.toDouble() ?: 0.0
    }
}
```

### 5. Configure NetworkModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @Named("etherscan")
    fun provideEtherscanRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.etherscan.io/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideEtherscanApiService(
        @Named("etherscan") retrofit: Retrofit
    ): EtherscanApiService {
        return retrofit.create(EtherscanApiService::class.java)
    }
}
```

### 6. Add API Key to local.properties

```properties
ETHERSCAN_API_KEY=YourApiKeyToken
```

Update BuildConfig in `build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        buildConfigField("String", "ETHERSCAN_API_KEY", "\"${project.findProperty("ETHERSCAN_API_KEY")}\"")
    }
}
```

---

## Success Criteria

- [ ] EtherscanApiService compiles without errors
- [ ] DTOs correctly parse Etherscan V2 responses (verify with API test)
- [ ] Mappers convert Wei strings to ETH doubles without precision loss
- [ ] NetworkModule provides EtherscanApiService via Hilt
- [ ] API key loads from BuildConfig (test with mock balance request)

---

## Testing Strategy

1. **Manual API Test**: Use curl/Postman to verify Etherscan endpoints return expected format
2. **Unit Test Mapper**: Test `weiToEth()` with edge cases (0, max uint256, decimals)
3. **Integration Test**: Inject EtherscanApiService in test, call getBalance() with real API key

```kotlin
// Example test
@Test
fun `weiToEth converts correctly`() {
    val wei = "2000000000000000000"  // 2 ETH
    val eth = EtherscanMapper.weiToEth(wei)
    assertEquals(2.0, eth, 0.0001)
}
```

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| API key not provided by user | Fail gracefully with error message, provide instructions |
| Etherscan response format changes | Version DTOs, add fallback parsing |
| Rate limit exceeded | Implement retry with exponential backoff (Phase 02) |
| Wei overflow for large balances | Use BigDecimal instead of Double (update in mapper) |

---

## Security Considerations

- **API Key Protection**: Never commit to Git, use local.properties (already in .gitignore)
- **Input Validation**: Validate wallet addresses before API calls (checksum format)
- **SSL Pinning**: Consider pinning Etherscan cert if handling large balances (future enhancement)

# Phase 04: Data Layer - Remote

## Context
- **Parent Plan:** [plan.md](plan.md)
- **Dependencies:** Phase 02 (domain entities)
- **Docs:** [system-architecture.md](../../docs/system-architecture.md)

## Overview
| Field | Value |
|-------|-------|
| Date | 2024-12-12 |
| Priority | Medium |
| Implementation Status | Pending |
| Review Status | Pending |

**Description:** Setup API service interfaces and DTOs for Kyber Aggregator (placeholder implementation).

## Key Insights
- Placeholder API interfaces for now (actual integration later)
- DTOs separate from domain entities
- Use Retrofit + Moshi for networking

## Requirements
1. KyberApiService interface (placeholder)
2. Basic DTOs for token/trade responses
3. Mapper utilities

## Architecture

```
data/remote/
├── kyber/
│   ├── KyberApiService.kt
│   └── dto/
│       └── TokenDto.kt
└── api/
    └── ApiConstants.kt
```

## Implementation Steps

### Step 1: Create ApiConstants.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/remote/api/ApiConstants.kt`

```kotlin
package com.otistran.flash_trade.data.remote.api

object ApiConstants {
    // Kyber Aggregator API
    const val KYBER_BASE_URL = "https://aggregator-api.kyberswap.com/"

    // API timeouts (seconds)
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    // Default chain (Ethereum mainnet)
    const val DEFAULT_CHAIN_ID = 1
}
```

### Step 2: Create TokenDto.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/remote/kyber/dto/TokenDto.kt`

```kotlin
package com.otistran.flash_trade.data.remote.kyber.dto

import com.otistran.flash_trade.domain.model.Token
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenDto(
    @Json(name = "address") val address: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "name") val name: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "chainId") val chainId: Int? = null,
    @Json(name = "logoURI") val logoUri: String? = null,
    @Json(name = "price") val price: Double? = null
) {
    fun toDomain(defaultChainId: Int): Token = Token(
        address = address,
        symbol = symbol,
        name = name,
        decimals = decimals,
        chainId = chainId ?: defaultChainId,
        logoUrl = logoUri,
        priceUsd = price ?: 0.0
    )
}

@JsonClass(generateAdapter = true)
data class TokenListResponse(
    @Json(name = "tokens") val tokens: List<TokenDto>
)
```

### Step 3: Create SwapDto.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/remote/kyber/dto/SwapDto.kt`

```kotlin
package com.otistran.flash_trade.data.remote.kyber.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SwapRouteRequest(
    @Json(name = "tokenIn") val tokenIn: String,
    @Json(name = "tokenOut") val tokenOut: String,
    @Json(name = "amountIn") val amountIn: String,
    @Json(name = "saveGas") val saveGas: Boolean = false,
    @Json(name = "gasInclude") val gasInclude: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SwapRouteResponse(
    @Json(name = "routeSummary") val routeSummary: RouteSummary?,
    @Json(name = "routerAddress") val routerAddress: String?
)

@JsonClass(generateAdapter = true)
data class RouteSummary(
    @Json(name = "tokenIn") val tokenIn: String,
    @Json(name = "tokenOut") val tokenOut: String,
    @Json(name = "amountIn") val amountIn: String,
    @Json(name = "amountOut") val amountOut: String,
    @Json(name = "amountOutUsd") val amountOutUsd: String?,
    @Json(name = "gas") val gas: String?,
    @Json(name = "gasUsd") val gasUsd: String?
)

@JsonClass(generateAdapter = true)
data class SwapBuildRequest(
    @Json(name = "routeSummary") val routeSummary: RouteSummary,
    @Json(name = "sender") val sender: String,
    @Json(name = "recipient") val recipient: String,
    @Json(name = "slippageTolerance") val slippageTolerance: Int = 50 // 0.5%
)

@JsonClass(generateAdapter = true)
data class SwapBuildResponse(
    @Json(name = "data") val data: String?,
    @Json(name = "routerAddress") val routerAddress: String?
)
```

### Step 4: Create KyberApiService.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/remote/kyber/KyberApiService.kt`

```kotlin
package com.otistran.flash_trade.data.remote.kyber

import com.otistran.flash_trade.data.remote.kyber.dto.SwapBuildRequest
import com.otistran.flash_trade.data.remote.kyber.dto.SwapBuildResponse
import com.otistran.flash_trade.data.remote.kyber.dto.SwapRouteResponse
import com.otistran.flash_trade.data.remote.kyber.dto.TokenListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Kyber Aggregator API Service.
 * Docs: https://docs.kyberswap.com/
 */
interface KyberApiService {

    /**
     * Get token list for a chain.
     */
    @GET("{chain}/api/v1/tokens")
    suspend fun getTokens(
        @Path("chain") chain: String = "ethereum"
    ): TokenListResponse

    /**
     * Get optimal swap route.
     */
    @GET("{chain}/api/v1/routes")
    suspend fun getSwapRoute(
        @Path("chain") chain: String = "ethereum",
        @Query("tokenIn") tokenIn: String,
        @Query("tokenOut") tokenOut: String,
        @Query("amountIn") amountIn: String,
        @Query("saveGas") saveGas: Boolean = false,
        @Query("gasInclude") gasInclude: Boolean = true
    ): SwapRouteResponse

    /**
     * Build swap transaction data.
     */
    @POST("{chain}/api/v1/route/build")
    suspend fun buildSwap(
        @Path("chain") chain: String = "ethereum",
        @Body request: SwapBuildRequest
    ): SwapBuildResponse
}
```

## Todo List
- [ ] Create data/remote/api/ directory
- [ ] Create ApiConstants.kt
- [ ] Create data/remote/kyber/dto/ directory
- [ ] Create TokenDto.kt
- [ ] Create SwapDto.kt
- [ ] Create KyberApiService.kt
- [ ] Verify compilation

## Success Criteria
- [ ] All 4 files created
- [ ] Each file <200 lines
- [ ] DTOs use @JsonClass for Moshi code generation
- [ ] API service uses suspend functions

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| API endpoints may change | Medium | Use official Kyber docs |

## Security Considerations
- API keys stored in BuildConfig (not in code)
- HTTPS only

## Next Steps
→ Phase 05: DI Modules

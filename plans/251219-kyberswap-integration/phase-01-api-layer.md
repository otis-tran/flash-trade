# Phase 1: API Layer Implementation

**Duration:** 30 minutes
**Dependencies:** None
**Risk Level:** Low

---

## Context Links

- **Research:** `research/researcher-01-kyberswap-api.md`
- **Existing API:** `app/src/main/java/com/otistran/flash_trade/data/remote/api/KyberApiService.kt`
- **Network Module:** `app/src/main/java/com/otistran/flash_trade/di/NetworkModule.kt`

---

## Overview

Extend existing KyberApiService with V1 swap endpoints. Create DTOs for route query, build request, and responses. Add client-id header interceptor for rate limit elevation.

---

## Key Insights

1. **Existing Infrastructure:**
   - Retrofit with Moshi already configured
   - KyberApiService exists for token list API
   - NetworkModule provides @Named("kyber") Retrofit instance

2. **API Requirements:**
   - GET /routes: Query phase (find optimal swap path)
   - POST /route/build: Execution phase (generate calldata)
   - Header: x-client-id mandatory for elevated rate limits
   - Base URL: https://aggregator-api.kyberswap.com

3. **Chain Support:**
   - Primary: base (chainId 8453)
   - Path format: /{chain}/api/v1/routes

---

## Requirements

### Functional
- Fetch swap quote with slippage tolerance
- Build encoded transaction data
- Support native token swaps (0xEeee...EEEE address)
- Handle error codes (4001-4011, 4221)

### Non-Functional
- Request timeout: 30s
- Client-id header on all requests
- JSON serialization with Moshi
- Error response parsing

---

## Architecture

```
KyberApiService (interface)
    ├── getSwapRoute() → RouteResponseDto
    └── buildSwapRoute() → BuildRouteResponseDto

DTOs:
    ├── RouteResponseDto
    │   └── RouteDataDto
    │       └── RouteSummaryDto
    ├── BuildRouteRequestDto
    │   └── RouteSummaryDto (reused)
    └── BuildRouteResponseDto
        └── EncodedSwapDataDto

Interceptor:
    └── ClientIdInterceptor (adds x-client-id header)
```

---

## Related Code Files

**Existing (to modify):**
1. `app/src/main/java/com/otistran/flash_trade/data/remote/api/KyberApiService.kt`
   - Add getSwapRoute() method
   - Add buildSwapRoute() method

2. `app/src/main/java/com/otistran/flash_trade/di/NetworkModule.kt`
   - Add ClientIdInterceptor to OkHttpClient

**New files:**
3. `app/src/main/java/com/otistran/flash_trade/data/remote/dto/kyber/RouteResponseDto.kt`
4. `app/src/main/java/com/otistran/flash_trade/data/remote/dto/kyber/BuildRouteRequestDto.kt`
5. `app/src/main/java/com/otistran/flash_trade/data/remote/dto/kyber/BuildRouteResponseDto.kt`
6. `app/src/main/java/com/otistran/flash_trade/core/network/interceptor/ClientIdInterceptor.kt`

---

## Implementation Steps

### Step 1: Create ClientIdInterceptor (5min)

**File:** `app/src/main/java/com/otistran/flash_trade/core/network/interceptor/ClientIdInterceptor.kt`

```kotlin
package com.otistran.flash_trade.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds x-client-id header to all KyberSwap API requests.
 * Required for elevated rate limits.
 */
class ClientIdInterceptor(private val clientId: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("x-client-id", clientId)
            .build()
        return chain.proceed(request)
    }
}
```

### Step 2: Create RouteResponseDto (8min)

**File:** `app/src/main/java/com/otistran/flash_trade/data/remote/dto/kyber/RouteResponseDto.kt`

```kotlin
package com.otistran.flash_trade.data.remote.dto.kyber

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RouteResponseDto(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: RouteDataDto,
    @Json(name = "requestId") val requestId: String
)

@JsonClass(generateAdapter = true)
data class RouteDataDto(
    @Json(name = "routeSummary") val routeSummary: RouteSummaryDto,
    @Json(name = "routerAddress") val routerAddress: String
)

@JsonClass(generateAdapter = true)
data class RouteSummaryDto(
    @Json(name = "tokenIn") val tokenIn: String,
    @Json(name = "amountIn") val amountIn: String,
    @Json(name = "amountInUsd") val amountInUsd: String,
    @Json(name = "tokenOut") val tokenOut: String,
    @Json(name = "amountOut") val amountOut: String,
    @Json(name = "amountOutUsd") val amountOutUsd: String,
    @Json(name = "gas") val gas: String,
    @Json(name = "gasPrice") val gasPrice: String,
    @Json(name = "gasUsd") val gasUsd: String,
    @Json(name = "routeID") val routeID: String,
    @Json(name = "checksum") val checksum: String,
    @Json(name = "timestamp") val timestamp: String
)
```

### Step 3: Create BuildRouteRequestDto (5min)

**File:** `app/src/main/java/com/otistran/flash_trade/data/remote/dto/kyber/BuildRouteRequestDto.kt`

```kotlin
package com.otistran.flash_trade.data.remote.dto.kyber

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BuildRouteRequestDto(
    @Json(name = "routeSummary") val routeSummary: RouteSummaryDto,
    @Json(name = "sender") val sender: String,
    @Json(name = "recipient") val recipient: String,
    @Json(name = "slippageTolerance") val slippageTolerance: Int = 10, // 0.1%
    @Json(name = "deadline") val deadline: Long? = null,
    @Json(name = "enableGasEstimation") val enableGasEstimation: Boolean = true,
    @Json(name = "source") val source: String = "FlashTrade",
    @Json(name = "origin") val origin: String? = null
)
```

### Step 4: Create BuildRouteResponseDto (6min)

**File:** `app/src/main/java/com/otistran/flash_trade/data/remote/dto/kyber/BuildRouteResponseDto.kt`

```kotlin
package com.otistran.flash_trade.data.remote.dto.kyber

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BuildRouteResponseDto(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: EncodedSwapDataDto,
    @Json(name = "requestId") val requestId: String
)

@JsonClass(generateAdapter = true)
data class EncodedSwapDataDto(
    @Json(name = "amountIn") val amountIn: String,
    @Json(name = "amountOut") val amountOut: String,
    @Json(name = "gas") val gas: String,
    @Json(name = "gasUsd") val gasUsd: String,
    @Json(name = "data") val data: String, // Hex-encoded calldata
    @Json(name = "routerAddress") val routerAddress: String,
    @Json(name = "transactionValue") val transactionValue: String
)
```

### Step 5: Extend KyberApiService (6min)

**File:** `app/src/main/java/com/otistran/flash_trade/data/remote/api/KyberApiService.kt`

Add these methods:

```kotlin
/**
 * Get optimal swap route.
 * @param chain Base chain identifier (e.g., "base", "ethereum")
 * @param tokenIn Input token address (use 0xEeee...EEEE for native)
 * @param tokenOut Output token address
 * @param amountIn Amount in wei (string)
 * @param slippageTolerance Slippage in bps (0-2000)
 * @param origin User wallet address for exclusive pools
 */
@GET("/{chain}/api/v1/routes")
suspend fun getSwapRoute(
    @Path("chain") chain: String,
    @Query("tokenIn") tokenIn: String,
    @Query("tokenOut") tokenOut: String,
    @Query("amountIn") amountIn: String,
    @Query("slippageTolerance") slippageTolerance: Int? = null,
    @Query("origin") origin: String? = null
): RouteResponseDto

/**
 * Build encoded swap transaction data.
 * @param chain Base chain identifier
 * @param request Build request with routeSummary + user addresses
 */
@POST("/{chain}/api/v1/route/build")
suspend fun buildSwapRoute(
    @Path("chain") chain: String,
    @Body request: BuildRouteRequestDto
): BuildRouteResponseDto
```

---

## Todo List

- [x] Create ClientIdInterceptor with x-client-id header
- [x] Create RouteResponseDto with nested data classes
- [x] Create BuildRouteRequestDto with default values
- [x] Create BuildRouteResponseDto with encoded data
- [x] Extend KyberApiService with getSwapRoute method
- [x] Extend KyberApiService with buildSwapRoute method
- [x] Update NetworkModule to add ClientIdInterceptor to kyber OkHttpClient
- [x] Verify Moshi adapters generated (rebuild project)

---

## Success Criteria

- [x] KyberApiService compiles with no errors
- [x] DTOs have @JsonClass(generateAdapter = true)
- [x] All API methods use suspend functions
- [x] x-client-id header added to all requests
- [⚠️] File sizes <200 lines each (NetworkModule.kt: 244 lines - see code review report)
- [x] Error response structure matches API docs

---

## Risk Assessment

**Low Risk:**
- DTOs are simple data classes
- Retrofit integration follows existing patterns
- Interceptor is standard OkHttp pattern

**Potential Issues:**
1. **Missing Moshi adapters:** Run kapt to generate adapters
2. **Wrong base URL:** Verify chain path parameter
3. **JSON parsing errors:** Test with real API responses

---

## Security Considerations

1. **Client-id:** Use BuildConfig or local.properties, not hardcoded
2. **Rate limiting:** Always include x-client-id to avoid IP bans
3. **Input validation:** Chain parameter must be whitelisted (base, ethereum, arbitrum)

---

## Next Steps

After completion:
1. Test API calls with Postman or unit tests
2. Proceed to Phase 2: Domain Layer
3. Create mappers to convert DTOs → domain models

---

## Phase 1 Status

**Completion:** 100% (all tasks completed)
**Review Date:** 2025-12-20
**Reviewer:** code-reviewer
**Report:** `reports/251220-from-code-reviewer-to-orchestrator-phase-01-api-layer-report.md`

**Findings:**
- ✅ 0 critical issues
- ✅ 0 high priority issues
- ⚠️ 1 medium priority issue (NetworkModule.kt exceeds 200 lines)
- 💡 3 low priority suggestions

**Build Status:** ✅ SUCCESS (compileDebugKotlin passed)

**Tech Debt:**
- NetworkModule.kt should be split into KyberNetworkModule.kt (can be deferred to Phase 6 optimization)

**Ready for Phase 2:** ✅ YES

# KyberSwap Aggregator V1 API Research for Android Integration

**Date**: 2025-12-19
**Target**: Flash Trade Android App (Kotlin/Retrofit)
**API Version**: V1 (Recommended - includes RFQ liquidity sources)

---

## API Endpoints Overview

### 1. Get Swap Route (Query Phase)
```
GET https://aggregator-api.kyberswap.com/{chain}/api/v1/routes
```
- **Purpose**: Find optimal swap paths with all liquidity sources (inc. RFQ)
- **Required Headers**: `x-client-id: MyAwesomeApp` (MANDATORY)
- **Use Case**: Pre-swap preview, display best rates, UX feedback

### 2. Build Route Encoded Data (Execution Phase)
```
POST https://aggregator-api.kyberswap.com/{chain}/api/v1/route/build
```
- **Purpose**: Generate encoded calldata for KyberSwap router contract
- **Required Headers**: `x-client-id: MyAwesomeApp` (MANDATORY)
- **Use Case**: One-step execution with transaction submission

---

## GET /routes - Request Parameters

| Parameter | Type | Required | Notes |
|-----------|------|----------|-------|
| `tokenIn` | string | ✅ | Use `0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE` for native tokens |
| `tokenOut` | string | ✅ | Output token address |
| `amountIn` | string | ✅ | Amount in wei (account for decimals) |
| `slippageTolerance` | number | ❌ | Range [0, 2000] bps; 10=0.1%; Default=0 |
| `includedSources` | string | ❌ | Comma-separated DEX IDs to include |
| `excludedSources` | string | ❌ | Comma-separated DEX IDs to exclude |
| `excludeRFQSources` | boolean | ❌ | Exclude RFQ liquidity; Default=false |
| `gasInclude` | boolean | ❌ | Include gas costs in route optimization; Default=true |
| `origin` | string | ❌ | User wallet for exclusive pool access |

---

## GET /routes - Response Structure

```json
{
  "code": 0,
  "message": "successfully",
  "data": {
    "routeSummary": {
      "tokenIn": "0x...",
      "amountIn": "1000000000000000000",
      "amountInUsd": "1668.95",
      "tokenOut": "0x...",
      "amountOut": "1666243758",
      "amountOutUsd": "1665.91",
      "gas": "253000",
      "gasPrice": "181968304449",
      "gasUsd": "0.0649",
      "route": [[{pool, tokenIn, tokenOut, swapAmount, amountOut, exchange, poolType}]],
      "routeID": "unique-id",
      "checksum": "checksum-value",
      "timestamp": "1703000000"
    },
    "routerAddress": "0x6131B5fae19EA4f9D964eAc0408E4408b66337b5"
  },
  "requestId": "unique-request-id"
}
```

---

## POST /route/build - Request Body

```json
{
  "routeSummary": {/* from GET response - REQUIRED */},
  "sender": "0x...",           // Address transferring input tokens - REQUIRED
  "recipient": "0x...",        // Address receiving output tokens - REQUIRED
  "slippageTolerance": 10,     // Bps; Range [0, 2000]
  "deadline": 1703100000,      // Unix timestamp (default +20min)
  "permit": "0x...",           // Optional encoded ERC20 permit calldata
  "enableGasEstimation": true, // RPC-based gas estimation to detect reverts
  "source": "MyAwesomeApp",    // Should match x-client-id header
  "origin": "0x..."            // User wallet (avoids rate-limit penalties)
}
```

---

## POST /route/build - Response Structure

```json
{
  "code": 0,
  "message": "successfully",
  "data": {
    "amountIn": "1000000000000000000",
    "amountInUsd": "1668.95",
    "amountOut": "1666243758",
    "amountOutUsd": "1665.91",
    "gas": "253000",
    "gasUsd": "0.0649",
    "additionalCostUsd": "0.02",      // L1 gas for L2s
    "additionalCostMessage": "L1 Fee", // Cost description
    "data": "0x...",                   // ENCODED CALLDATA FOR ROUTER
    "routerAddress": "0x6131B5fae19EA4f9D964eAc0408E4408b66337b5",
    "transactionValue": "0"            // Native token value for swaps from ETH
  },
  "requestId": "unique-request-id"
}
```

---

## Error Codes & Handling

| Code | HTTP | Meaning | Recovery |
|------|------|---------|----------|
| 4001 | 400 | Malformed query params | Validate tokenIn, tokenOut, amountIn |
| 4002 | 400 | Malformed request body | Validate routeSummary structure |
| 4005 | 400 | Fee > amountIn | Reduce fee or increase amountIn |
| 4007 | 400 | Fee > amountOut | Check swap route validity |
| 4008 | 400 | **No viable route** | Try: lower amount, different token pair, include/exclude sources |
| 4009 | 400 | amountIn > max allowed | Split swap into smaller chunks |
| 4010 | 400 | No eligible liquidity pools | Use different token pair or chain |
| 4011 | 400 | Token not found/invalid | Verify tokenIn/tokenOut addresses |
| 4221 | 422 | WETH not on chain | Wrong chain parameter |

**Retry Strategy**: Exponential backoff (500ms → 1s → 2s) for 4008/4010; fail-fast for 4001/4002/4011.

---

## Rate Limiting & clientId Impact

- **Without clientId**: Stricter limits (~10 req/sec per IP)
- **With clientId**: Elevated limits (~50-100 req/sec per source)
- **Implementation**: Always include `x-client-id` header in ALL requests
- **Best Practice**: Use app name as clientId; contact KyberSwap BD for higher limits

---

## ERC20 Token Approval Flow

### Standard Approval (2-Step):
1. User approves routerAddress to spend tokenIn (contract call)
2. Execute swap with encoded data from POST /route/build

### Permit Pattern (1-Step):
1. Generate ERC20 permit signature (off-chain)
2. Include encoded `permit` in POST body
3. Spender MUST match `routerAddress` from response
4. Eliminates separate approval transaction (gas savings)

**MAX_UINT256 Pattern**:
```solidity
uint256 MAX_UINT256 = 2^256 - 1;
// Approve once for unlimited swaps
IERC20(tokenIn).approve(routerAddress, MAX_UINT256);
```

---

## Android Retrofit Interface Definition

```kotlin
interface KyberSwapApi {

    @GET("/{chain}/api/v1/routes")
    suspend fun getSwapRoute(
        @Path("chain") chain: String,
        @Header("x-client-id") clientId: String,
        @Query("tokenIn") tokenIn: String,
        @Query("tokenOut") tokenOut: String,
        @Query("amountIn") amountIn: String,
        @Query("slippageTolerance") slippageTolerance: Int? = null,
        @Query("origin") origin: String? = null
    ): GetRouteResponse

    @POST("/{chain}/api/v1/route/build")
    suspend fun buildSwapRoute(
        @Path("chain") chain: String,
        @Header("x-client-id") clientId: String,
        @Body request: BuildRouteRequest
    ): BuildRouteResponse

    // Error Response (unified)
    data class ErrorResponse(
        val code: Int,
        val message: String,
        val requestId: String,
        val details: Map<String, Any>? = null
    )
}

// Models
data class GetRouteResponse(
    val code: Int,
    val message: String,
    val data: RouteData,
    val requestId: String
)

data class RouteData(
    val routeSummary: RouteSummary,
    val routerAddress: String
)

data class RouteSummary(
    val tokenIn: String,
    val amountIn: String,
    val amountInUsd: String,
    val tokenOut: String,
    val amountOut: String,
    val amountOutUsd: String,
    val gas: String,
    val gasPrice: String,
    val gasUsd: String,
    val route: List<List<SwapSequence>>,
    val routeID: String,
    val checksum: String,
    val timestamp: String
)

data class BuildRouteRequest(
    val routeSummary: RouteSummary,
    val sender: String,
    val recipient: String,
    val slippageTolerance: Int? = 10,
    val deadline: Long? = null,
    val permit: String? = null,
    val enableGasEstimation: Boolean? = false,
    val source: String? = "FlashTrade",
    val origin: String? = null
)

data class BuildRouteResponse(
    val code: Int,
    val message: String,
    val data: EncodedSwapData,
    val requestId: String
)

data class EncodedSwapData(
    val amountIn: String,
    val amountOut: String,
    val gas: String,
    val gasUsd: String,
    val data: String,           // Hex-encoded calldata
    val routerAddress: String,
    val transactionValue: String // Native token value if applicable
)
```

---

## Key Implementation Patterns

1. **Sequential Calls**: GET routes → POST build → Execute on-chain
2. **Caching**: Cache routes for 30s with route checksum validation
3. **Gas Estimation**: Set `enableGasEstimation=true` to detect execution failures
4. **Slippage**: Flash Trade default=10 bps (0.1%), configurable up to 2000 bps
5. **Native Tokens**: Ethereum mainnet uses `0xEeee...EEEE` address
6. **Chain Support**: ethereum, arbitrum, polygon, optimism, bsc, avalanche, base, linea, etc.

---

## Unresolved Questions

1. **Rate limit buckets**: Are limits per-chain or global across all chains?
2. **RFQ availability**: Which chains have RFQ liquidity sources enabled?
3. **Permit encoding**: Does KyberSwap provide permit generation utility or is it EIP-2612 standard?
4. **Batch swaps**: Can routeSummary be reused for multiple executions or is it one-time?
5. **Fallback chains**: If primary route fails (e.g., 4008), should app suggest alternative chains?

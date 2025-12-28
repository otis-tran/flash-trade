# KD Market Service API

## Base URL

```
https://kd-market-service-api.kyberengineering.io/ethereum/api/v1
```

## Overview

This is the API documentation for KD Market Service - a comprehensive API for accessing liquidity pool data, user positions, token information, and KEM reward configurations on the Ethereum blockchain.

---

## User Positions

### GET /account/{user_address}/positions

Get all positions for a user including total liquidity, total earn, and position counts.

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| user_address | string | path | User address | Yes | - |

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 500 | Internal Server Error |

**Response Example (200)**

```json
{
  "positions": [
    {
      "tokenID": "string",
      "userAddress": "string",
      "poolAddress": "string",
      "exchange": "string",
      "protocol": "string",
      "type": "string",
      "isActive": true,
      "createdAt": "string",
      "txHash": "string",
      "eventIndex": 0,
      "eventType": "string",
      "token0": {
        "address": "0x123...",
        "symbol": "ETH",
        "decimals": 1,
        "priceUsd": "1",
        "logo": "string"
      },
      "token1": {
        "address": "0x123...",
        "symbol": "ETH",
        "decimals": 1,
        "priceUsd": "1",
        "logo": "string"
      },
      "tickLower": 0,
      "tickUpper": 0,
      "currentTick": 0,
      "currentPrice": "string",
      "minPrice": "string",
      "maxPrice": "string",
      "currentAmounts": [
        {
          "balance": "string",
          "price": "string",
          "valueUsd": "string"
        }
      ],
      "providedAmounts": [
        {
          "balance": "string",
          "price": "string",
          "valueUsd": "string"
        }
      ],
      "feesClaimed": [
        {
          "balance": "string",
          "price": "string",
          "valueUsd": "string"
        }
      ],
      "feePending": [
        {
          "balance": "string",
          "price": "string",
          "valueUsd": "string"
        }
      ],
      "feeEarnedUsd": {
        "24h": "123456789",
        "7d": "1000000.00",
        "30d": "1000000.00.00",
        "all": "1000000.00"
      },
      "apr": {
        "24h": "123456789",
        "7d": "1000000.00",
        "30d": "1000000.00.00",
        "all": "0x123..."
      },
      "aprKem": {
        "24h": "123456789",
        "7d": "1000000.00",
        "30d": "1000000.00.00",
        "all": "0x123..."
      },
      "tvlUsd": "string",
      "chainID": "string",
      "blockNumber": 0,
      "blockTime": "string",
      "tokenReward": [
        {
          "tokenInfo": {
            "address": "0x123...",
            "symbol": "ETH",
            "decimals": 1,
            "priceUsd": "1",
            "logo": "string"
          },
          "totalReward": "string",
          "totalRewardUsd": "string",
          "apr": "string",
          "startTime": 0,
          "endTime": 0
        }
      ]
    }
  ],
  "totalLiquidity": "string",
  "totalEarn": "string",
  "openPositions": 0,
  "closedPositions": 0
}
```

---

## Position

### GET /positions/{position_id}

Get detailed information about a specific position.

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| position_id | string | path | Position ID | Yes | - |

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 400 | Bad Request |
| 500 | Internal Server Error |

### GET /positions/{position_id}/kem

Get KEM reward information for a position.

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| position_id | string | path | Position ID | Yes | - |

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 400 | Bad Request |
| 500 | Internal Server Error |

**Response Example (200)**

```json
{
  "tokenReward": [
    {
      "tokenInfo": {
        "address": "0x123...",
        "symbol": "ETH",
        "decimals": 1,
        "priceUsd": "1",
        "logo": "string"
      },
      "totalReward": "string",
      "totalRewardUsd": "string",
      "apr": "string",
      "startTime": 0,
      "endTime": 0
    }
  ]
}
```

### GET /positions/{position_id}/kem/cycle

Get total KEM rewards for a specific position in a cycle (defaults to current cycle starting from Wednesday 00:00 UTC).

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| position_id | string | path | Position ID | Yes | - |
| cycle_start_time | string | query | Cycle start time in Unix timestamp (defaults to last Wednesday 00:00 UTC) | No | - |
| check_time | string | query | Check time in Unix timestamp (defaults to current time) | No | - |

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 400 | Bad Request |
| 500 | Internal Server Error |

---

## Pool

### GET /pools

Get a list of all pools with their states.

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| exchange | string | query | Filter by exchange type (e.g. uniswap-v4-kem, uniswapv3, uniswap-v4) | No | - |
| type | string | query | Filter by type (e.g. uniswap-v4, uniswap-v3) | No | - |
| minTvl | string | query | Minimum TVL in USD (default: 1000) | No | - |
| minVolume | string | query | Minimum 24h volume in USD (default: 1000) | No | - |
| sort | string | query | Sort order (default: tvl_desc) | No | - |
| page | integer | query | Page number (default: 1) | No | - |
| limit | integer | query | Number of items per page (default: 100) | No | - |

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 500 | Internal Server Error |

**Response Example (200)**

```json
{
  "data": [
    {
      "poolAddress": "0x123...",
      "exchange": "string",
      "type": "string",
      "hooks": "string",
      "tvlUsd": "1000000.00",
      "liquidity": "1000000",
      "sqrtPriceX96": "123456789",
      "blockNumber": 12345678,
      "blockTime": 1234567890
    }
  ],
  "page": 0,
  "limit": 0,
  "filter": {
    "exchange": ["string"],
    "type": "string",
    "minTvl": 0,
    "maxTvl": 0,
    "minVolume": 0,
    "sort": "volume_desc"
  }
}
```

### GET /poolState

Get list pool state.

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| addresses | string | query | List pool Address | No | - |

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 500 | Internal Server Error |

**Response Example (200)**

```json
{
  "data": [
    {
      "poolAddress": "0x123...",
      "exchange": "string",
      "type": "string",
      "tvlUsd": "1000000.00",
      "liquidity": "1000000",
      "sqrtPriceX96": "123456789",
      "tick": 12345,
      "tickSpacing": 12345,
      "blockNumber": 12345678,
      "blockTime": 1234567890,
      "poolCreatedAt": 0,
      "latestBlock": 0,
      "processBlock": 0,
      "feesTier": 0.003,
      "totalVolumeUsd": "500000.00",
      "volume24h": "string",
      "volumeUsdByDay": {},
      "feesCollectedUsd24h": {},
      "feesCollectedUsdByDay": {},
      "token0": {
        "address": "0x123...",
        "symbol": "ETH",
        "decimals": 1,
        "priceUsd": "1",
        "logo": "string"
      },
      "token1": {
        "address": "0x123...",
        "symbol": "ETH",
        "decimals": 1,
        "priceUsd": "1",
        "logo": "string"
      },
      "token0Amount": "string",
      "token1Amount": "string",
      "feeEarnedUsd": {
        "24h": "123456789",
        "7d": "1000000.00",
        "30d": "1000000.00.00",
        "all": "1000000.00"
      },
      "apr": {
        "24h": "123456789",
        "7d": "1000000.00",
        "30d": "1000000.00.00",
        "all": "0x123..."
      },
      "aprKem": {
        "24h": "123456789",
        "7d": "1000000.00",
        "30d": "1000000.00.00",
        "all": "0x123..."
      },
      "kemLMApr": {
        "24h": "123456789",
        "7d": "1000000.00",
        "30d": "1000000.00.00",
        "all": "0x123..."
      },
      "kemEGApr": {
        "24h": "123456789",
        "7d": "1000000.00",
        "30d": "1000000.00.00",
        "all": "0x123..."
      },
      "tokenReward": {},
      "tokenRewardEG": {}
    }
  ]
}
```

---

## Token

### GET /tokens

Get a list of all tokens that have pools with TVL and volume above thresholds.

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| minTvl | string | query | Minimum TVL in USD (default: 1000) | No | - |
| maxTvl | string | query | Maximum TVL in USD | No | - |
| minVolume | string | query | Minimum total volume in USD (default: 1000) | No | - |
| maxVolume | string | query | Maximum total volume in USD | No | - |
| minPoolCreated | integer | query | Minimum pool creation timestamp (Unix timestamp) | No | - |
| maxPoolCreated | integer | query | Maximum pool creation timestamp (Unix timestamp) | No | - |
| sort | string | query | Sort order: tvl_desc, tvl_asc, created_desc, created_asc (default: tvl_desc) | No | - |
| page | integer | query | Page number (default: 1) | No | - |
| limit | integer | query | Number of items per page (default: 100) | No | - |

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 400 | Bad Request |
| 500 | Internal Server Error |

**Response Example (200)**

```json
{
  "data": [
    {
      "address": "0x123...",
      "symbol": "ETH",
      "name": "Wrapped Ether",
      "decimals": 18,
      "logoUrl": "string",
      "websites": "https://example.com",
      "totalSupply": 1000000,
      "isFot": false,
      "isHoneypot": false,
      "isStable": false,
      "isVerified": true,
      "isWhitelisted": true,
      "tax": 0.1,
      "poolCount": 5,
      "totalTvlAllPools": "5000000",
      "avgPoolTvl": "1000000",
      "maxPoolTvl": "1000000",
      "maxPoolTvlAddress": "0xabc...",
      "maxPoolVolume": "5000000",
      "earliestPoolCreatedAt": 0,
      "cmcRank": 1,
      "cgkRank": 1
    }
  ],
  "count": 0,
  "page": 0,
  "pageSize": 0,
  "total": 0,
  "totalPages": 0,
  "filter": {
    "minTvl": 0,
    "maxTvl": 0,
    "minVolume": 0,
    "maxVolume": 0,
    "minPoolCreated": 0,
    "maxPoolCreated": 0,
    "sort": "tvl_desc"
  }
}
```

---

## KEM Reward Config

### GET /reward-config/:pool_address

Retrieve KEM reward configuration based on pool address and time range.

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| pool_address | string | path | Pool address | Yes | - |
| check_time | integer | query | check time filter (Unix timestamp) | No | - |

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 500 | Internal server error |

**Response Example (200)**

```json
{
  "poolAddress": "0xPool123",
  "startTime": 100,
  "endTime": 200,
  "egSharingPercentage": 0.7,
  "rewardCfg": [
    {
      "tokenAddress": "0xToken",
      "amountReward": 1000,
      "weightFee": 1,
      "weightAt": 0.5,
      "weightFeeEg": 0.2,
      "weightAt3Fee": 0.7,
      "weightAtFee3": 0.3
    }
  ]
}
```

### GET /admin/kem-reward-config

Retrieve KEM reward configuration based on pool address and time range.

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| x-admin-api-key | string | header | Admin API Key | Yes | - |
| pool_address | string | query | Pool address to filter by | No | - |
| start_time | integer | query | Start time filter (Unix timestamp) | No | - |
| end_time | integer | query | End time filter (Unix timestamp) | No | - |

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 401 | Unauthorized - Invalid or missing API key |
| 500 | Internal server error |

**Response Example (200)**

```json
{
  "data": [
    {
      "poolAddress": "0xPool123",
      "startTime": 100,
      "endTime": 200,
      "egSharingPercentage": 0.7,
      "rewardCfg": [
        {
          "tokenAddress": "0xToken",
          "amountReward": 1000,
          "weightFee": 1,
          "weightAt": 0.5,
          "weightFeeEg": 0.2,
          "weightAt3Fee": 0.7,
          "weightAtFee3": 0.3
        }
      ]
    }
  ]
}
```

**Response Example (401)**

```json
{
  "error": "Internal Server Error",
  "message": "An error occurred while processing the request",
  "time": "2023-01-01T00:00:00Z"
}
```

### POST /admin/kem-reward-config

Add a new KEM reward configuration.

**Request Body**

Content-Type: `application/json`

```json
{
  "poolAddress": "0xPool123",
  "startTime": 100,
  "endTime": 200,
  "egSharingPercentage": 0.7,
  "rewardCfg": [
    {
      "tokenAddress": "0xToken",
      "amountReward": 1000,
      "weightFee": 1,
      "weightAt": 0.5,
      "weightFeeEg": 0.2,
      "weightAt3Fee": 0.7,
      "weightAtFee3": 0.3
    }
  ]
}
```

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| x-admin-api-key | string | header | Admin API Key | Yes | - |
| request | body | KEM reward configuration data | Yes | KemRewardCfgDTO |

**Responses**

| Code | Description |
|------|-------------|
| 201 | Created |
| 400 | Bad request - invalid input |
| 401 | Unauthorized - Invalid or missing API key |
| 500 | Internal server error |

### DELETE /admin/kem-reward-config

Delete an existing KEM reward configuration by pool_address and startTime.

**Parameters**

| Name | Type | In | Description | Required | Schema |
|------|------|----|-------------|:----:|--------|
| x-admin-api-key | string | header | Admin API Key | Yes | - |
| pool_address | string | query | Pool address to delete configuration for | Yes | - |
| start_time | integer | query | Start time of the configuration to delete (Unix timestamp) | Yes | - |

**Responses**

| Code | Description |
|------|-------------|
| 201 | Created |
| 400 | Bad request - invalid input |
| 401 | Unauthorized - Invalid or missing API key |
| 500 | Internal server error |

---

## Block

### GET /block/latest

Retrieve the latest block number from the blockchain.

**Responses**

| Code | Description |
|------|-------------|
| 200 | OK |
| 500 | Internal server error |

**Response Example (200)**

```json
{
  "height": 12345678
}
```

---

# Data Models

## AmountDto

| Property | Type | Description |
|----------|------|-------------|
| balance | string | Token balance |
| price | string | Token price |
| valueUsd | string | USD value |

## AprDto

| Property | Type | Description | Example |
|----------|------|-------------|--------|
| 24h | string | APR for last 24 hours | "123456789" |
| 7d | string | APR for last 7 days | "1000000.00" |
| 30d | string | APR for last 30 days | "1000000.00.00" |
| all | string | All-time APR | "0x123..." |

## BasicPoolInfoDto

| Property | Type | Description | Example |
|----------|------|-------------|--------|
| poolAddress | string | Pool contract address | "0x123..." |
| exchange | string | Exchange type | - |
| type | string | Pool type | - |
| hooks | string | Hooks address | - |
| tvlUsd | string | Total Value Locked in USD | "1000000.00" |
| liquidity | string | Current pool liquidity | "1000000" |
| sqrtPriceX96 | string | Current sqrt price (X96) | "123456789" |
| blockNumber | integer | Current block number | 12345678 |
| blockTime | integer | Block timestamp | 1234567890 |

## BlockLatestResponse

| Property | Type | Description | Example |
|----------|------|-------------|--------|
| height | integer | Latest block height | 12345678 |

## Empty

Empty object - used for successful operations with no return data.

## ErrorResponse

| Property | Type | Description | Example |
|----------|------|-------------|--------|
| error | string | Error type | "Internal Server Error" |
| message | string | Error message | "An error occurred while processing the request" |
| time | string | Timestamp | "2023-01-01T00:00:00Z" |

## FeeEarnedUsdDto

| Property | Type | Description | Example |
|----------|------|-------------|--------|
| 24h | string | Fees earned (24h) | "123456789" |
| 7d | string | Fees earned (7d) | "1000000.00" |
| 30d | string | Fees earned (30d) | "1000000.00.00" |
| all | string | All-time fees earned | "1000000.00" |

## KemRewardCfgDTO

| Property | Type | Required | Description | Example |
|----------|------|:----:|-------------|--------|
| poolAddress | string | Yes | Pool contract address | "0xPool123" |
| startTime | integer | Yes | Reward start time (Unix timestamp) | 100 |
| endTime | integer | Yes | Reward end time (Unix timestamp) | 200 |
| egSharingPercentage | number | Yes | EG sharing percentage (0-1) | 0.7 |
| rewardCfg | array | Yes | Reward configuration array | - |

## KemRewardConfigResp

| Property | Type | Description |
|----------|------|-------------|
| data | array[KemRewardCfgDTO] | List of KEM reward configurations |

## ListPoolInfoRep

| Property | Type | Description |
|----------|------|-------------|
| data | array[BasicPoolInfoDto] | List of pool information |
| page | integer | Current page number |
| limit | integer | Items per page |
| filter | PoolFilters | Applied filters |

## PoolFilters

| Property | Type | Description |
|----------|------|-------------|
| exchange | array[string] | Exchange types to filter |
| type | string | Pool type to filter |
| minTvl | number | Minimum TVL threshold |
| maxTvl | number | Maximum TVL threshold |
| minVolume | number | Minimum volume threshold |
| sort | string | Sort order: `volume_desc`, `volume_asc`, `tvl_desc`, `tvl_asc` |

## PoolStateDto

| Property | Type | Description | Example |
|----------|------|-------------|--------|
| poolAddress | string | Pool contract address | "0x123..." |
| exchange | string | Exchange type | - |
| type | string | Pool type | - |
| tvlUsd | string | Total Value Locked in USD | "1000000.00" |
| liquidity | string | Current pool liquidity | "1000000" |
| sqrtPriceX96 | string | Current sqrt price (X96) | "123456789" |
| tick | integer | Current tick | 12345 |
| tickSpacing | integer | Tick spacing | 12345 |
| blockNumber | integer | Current block number | 12345678 |
| blockTime | integer | Block timestamp | 1234567890 |
| poolCreatedAt | integer | Pool creation timestamp | - |
| latestBlock | integer | Latest processed block | - |
| processBlock | integer | Currently processing block | - |
| feesTier | number | Fee tier (e.g., 0.003 = 0.3%) | 0.003 |
| totalVolumeUsd | string | Total trading volume in USD | "500000.00" |
| volume24h | string | 24h volume | - |
| volumeUsdByDay | object | Daily volume data | - |
| feesCollectedUsd24h | object | 24h fees collected | - |
| feesCollectedUsdByDay | object | Daily fees collected | - |
| token0 | TokenInfo | Token0 information | - |
| token1 | TokenInfo | Token1 information | - |
| token0Amount | string | Token0 amount in pool | - |
| token1Amount | string | Token1 amount in pool | - |
| feeEarnedUsd | FeeEarnedUsdDto | Fees earned data | - |
| apr | AprDto | APR data | - |
| aprKem | AprDto | KEM APR (deprecated) | - |
| kemLMApr | AprDto | KEM LM APR | - |
| kemEGApr | AprDto | KEM EG APR | - |
| tokenReward | object | Token rewards data | - |
| tokenRewardEG | object | EG token rewards data | - |

## TokenDto

| Property | Type | Description | Example |
|----------|------|-------------|--------|
| address | string | Token contract address | "0x123..." |
| symbol | string | Token symbol | "ETH" |
| name | string | Token name | "Wrapped Ether" |
| decimals | integer | Token decimals | 18 |
| logoUrl | string | Token logo URL | - |
| websites | string | Token website | "https://example.com" |
| totalSupply | number | Total token supply | 1000000 |
| isFot | boolean | Is FoT token | false |
| isHoneypot | boolean | Is honeypot | false |
| isStable | boolean | Is stablecoin | false |
| isVerified | boolean | Is verified | true |
| isWhitelisted | boolean | Is whitelisted | true |
| tax | number | Tax percentage | 0.1 |
| poolCount | integer | Number of pools | 5 |
| totalTvlAllPools | string | Total TVL across all pools | "5000000" |
| avgPoolTvl | string | Average pool TVL | "1000000" |
| maxPoolTvl | string | Maximum pool TVL | "1000000" |
| maxPoolTvlAddress | string | Address of max TVL pool | "0xabc..." |
| maxPoolVolume | string | Maximum pool volume | "5000000" |
| earliestPoolCreatedAt | integer | Earliest pool creation | - |
| cmcRank | integer | CoinMarketCap rank | 1 |
| cgkRank | integer | CoinGecko rank | 1 |

## TokenFilters

| Property | Type | Description |
|----------|------|-------------|
| minTvl | number | Minimum TVL threshold |
| maxTvl | number | Maximum TVL threshold |
| minVolume | number | Minimum volume threshold |
| maxVolume | number | Maximum volume threshold |
| minPoolCreated | integer | Minimum pool creation timestamp |
| maxPoolCreated | integer | Maximum pool creation timestamp |
| sort | string | Sort order: `tvl_desc`, `tvl_asc`, `created_desc`, `created_asc` |

## TokenInfo

| Property | Type | Description | Example |
|----------|------|-------------|--------|
| address | string | Token contract address | "0x123..." |
| symbol | string | Token symbol | "ETH" |
| decimals | integer | Token decimals | 1 |
| priceUsd | string | Token price in USD | "1" |
| logo | string | Token logo URL | - |

## TokenListResponse

| Property | Type | Description |
|----------|------|-------------|
| data | array[TokenDto] | List of tokens |
| count | integer | Number of items in current page |
| page | integer | Current page number |
| pageSize | integer | Items per page |
| total | integer | Total number of items |
| totalPages | integer | Total number of pages |
| filter | TokenFilters | Applied filters |

## TokenRewardConfigDTO

| Property | Type | Required | Description | Example |
|----------|------|:----:|-------------|--------|
| tokenAddress | string | Yes | Reward token address | "0xToken" |
| amountReward | number | Yes | Total reward amount | 1000 |
| weightFee | number | Yes | Fee weight | 1 |
| weightAt | number | Yes | Asset weight | 0.5 |
| weightFeeEg | number | No | Fee EG weight | 0.2 |
| weightAt3Fee | number | No | Asset 3 Fee weight | 0.7 |
| weightAtFee3 | number | No | Asset Fee 3 weight | 0.3 |

## TokenRewardDto

| Property | Type | Description |
|----------|------|-------------|
| tokenInfo | TokenInfo | Token information |
| totalReward | string | Total reward in token |
| totalRewardUsd | string | Total reward in USD |
| apr | string | Annual percentage rate |
| startTime | integer | Cycle start time |
| endTime | integer | Cycle end time |

## TokenRewardResponse

| Property | Type | Description |
|----------|------|-------------|
| tokenReward | array[TokenRewardDto] | List of token rewards |

## UserPositionDto

| Property | Type | Description |
|----------|------|-------------|
| tokenID | string | NFT position token ID |
| userAddress | string | Owner address |
| poolAddress | string | Pool address |
| exchange | string | Exchange type |
| protocol | string | Protocol type |
| type | string | Position type |
| isActive | string | Is position active |
| createdAt | string | Creation timestamp |
| txHash | string | Creation transaction hash |
| eventIndex | integer | Event index |
| eventType | string | Event type |
| token0 | TokenInfo | Token0 information |
| token1 | TokenInfo | Token1 information |
| tickLower | integer | Lower tick boundary |
| tickUpper | integer | Upper tick boundary |
| currentTick | integer | Current pool tick |
| currentPrice | string | Current pool price |
| minPrice | string | Minimum in-range price |
| maxPrice | string | Maximum in-range price |
| currentAmounts | array[AmountDto] | Current token amounts |
| providedAmounts | array[AmountDto] | Initial provided amounts |
| feesClaimed | array[AmountDto] | Fees already claimed |
| feePending | array[AmountDto] | Pending unclaimed fees |
| feeEarnedUsd | FeeEarnedUsdDto | Fees earned in USD |
| apr | AprDto | Annual percentage rate |
| aprKem | AprDto | KEM APR |
| tvlUsd | string | Total value locked in USD |
| chainID | string | Chain ID |
| blockNumber | integer | Block number |
| blockTime | string | Block timestamp |
| tokenReward | array[TokenRewardDto] | KEM token rewards |

## UserPositionsResponse

| Property | Type | Description |
|----------|------|-------------|
| positions | array[UserPositionDto] | List of user positions |
| totalLiquidity | string | Total liquidity across all positions |
| totalEarn | string | Total earnings across all positions |
| openPositions | integer | Number of open positions |
| closedPositions | integer | Number of closed positions |

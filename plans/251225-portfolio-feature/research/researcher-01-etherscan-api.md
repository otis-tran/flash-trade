# Etherscan API V2 Research Report

## Summary
Etherscan API V2 provides unified multi-chain support via single endpoint `https://api.etherscan.io/v2/api` with chainid parameter. Supports 60+ networks including Ethereum (chainid=1) and Linea (chainid=59144). All requests require API key authentication and rate limiting applies per API tier.

## API Structure

### Base Endpoint
```
https://api.etherscan.io/v2/api
```

### Request Format
```
https://api.etherscan.io/v2/api
?chainid={chainid}
&module=account
&action={action}
&address={address}
&apikey={apikey}
&tag=latest
```

**Critical:** `chainid` is mandatory for V2 API. Missing it returns error: "Missing chainid parameter (required for v2 api)".

### Supported Networks
- Ethereum Mainnet: chainid=**1**
- Linea Mainnet: chainid=**59144**
- Base: chainid=8453
- Arbitrum: chainid=42161
- Optimism: chainid=10

## Key Endpoints

### 1. Get Native Balance
**Endpoint:** `action=balance`

```
https://api.etherscan.io/v2/api?chainid=1&module=account&action=balance&address={address}&tag=latest&apikey={key}
```

**Response:**
```json
{
  "status": "1",
  "message": "OK",
  "result": "2000000000000000000"
}
```

### 2. Get Token Balance (ERC-20)
**Endpoint:** `action=tokenbalance`

```
https://api.etherscan.io/v2/api
?chainid=1
&module=account
&action=tokenbalance
&contractaddress={token_address}
&address={wallet_address}
&tag=latest
&apikey={key}
```

**Response:** Raw token amount (includes decimals in response). Example: 215241526476136819398 = 215.24 with 18 decimals.

### 3. ERC-20 Transfer History
**Endpoint:** `action=tokentx`

```
https://api.etherscan.io/v2/api
?chainid=1
&module=account
&action=tokentx
&address={address}
&contractaddress={token_contract}
&page=1
&offset=100
&startblock=0
&endblock=99999999
&sort=asc
&apikey={key}
```

**Parameters:**
- `contractaddress` - optional; omit to get all token transfers
- `page`, `offset` - pagination (max 10,000 records per request)
- `startblock`, `endblock` - filter by block range
- `sort` - asc or desc

**Response Fields per Transfer:**
- `from`, `to` - addresses
- `value` - token amount (includes decimals)
- `tokenName`, `tokenSymbol`, `tokenDecimal`
- `hash` - transaction hash
- `blockNumber`, `timeStamp` - timing

### 4. Normal Transactions
**Endpoint:** `action=txlist`

```
https://api.etherscan.io/v2/api
?chainid=1
&module=account
&action=txlist
&address={address}
&startblock=0
&endblock=99999999
&sort=asc
&apikey={key}
```

## Rate Limits & Best Practices

### Rate Limiting
- Default free tier: 5 calls/second
- Paid plans available for higher throughput
- Per documentation: check supported chains endpoint at `https://api.etherscan.io/v2/chainlist`

### Best Practices
1. **Batch requests by network** - Loop through chainids rather than making concurrent requests to same endpoint
2. **Pagination** - Use offset/page params; default 100 results per request
3. **Block ranges** - Use startblock/endblock to limit historical data and improve performance
4. **Caching** - Cache balance data and only refresh periodically; cache token lists
5. **Error handling** - Status="0" indicates error; check message field for details

## Implementation Notes (Kotlin/Retrofit)

### Retrofit Service Definition
```kotlin
interface EtherscanService {
    @GET("v2/api")
    suspend fun getBalance(
        @Query("chainid") chainId: Int,
        @Query("module") module: String = "account",
        @Query("action") action: String = "balance",
        @Query("address") address: String,
        @Query("tag") tag: String = "latest",
        @Query("apikey") apiKey: String
    ): BalanceResponse

    @GET("v2/api")
    suspend fun getTokenTx(
        @Query("chainid") chainId: Int,
        @Query("module") module: String = "account",
        @Query("action") action: String = "tokentx",
        @Query("address") address: String,
        @Query("contractaddress") contractAddress: String? = null,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 100,
        @Query("sort") sort: String = "asc",
        @Query("apikey") apiKey: String
    ): TokenTxResponse
}
```

### Chain Support
- Use enum for chainIds: `Ethereum(1), Linea(59144)`
- Single API key works across all chains
- Each network requires separate API call (endpoint accepts one chainid at a time)

## Unresolved Questions
1. Exact rate limit thresholds per API tier (need to check paid plan documentation)
2. Etherscan's recommended polling interval for updating portfolio data
3. Token metadata enrichment - does API include token logo URLs or additional metadata?

## Citations
- [V2 Migration Guide](https://docs.etherscan.io/v2-migration)
- [V2 API Multichain Article](https://info.etherscan.com/etherscan-api-v2-multichain/)
- [API Accounts Endpoints](https://docs.etherscan.io/etherscan-v2/api-endpoints/accounts)
- [Tokens Endpoints](https://docs.etherscan.io/etherscan-v2/api-endpoints/tokens)
- [Linea Network Info](https://docs.linea.build/get-started/build/network-info)
- [AlgoTrading101 Etherscan Guide](https://algotrading101.com/learn/etherscan-api-guide/)

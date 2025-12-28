# Privy SDK for Android - Transaction Signing & Execution Research

**Date:** 2025-12-19
**Focus:** KyberSwap integration transaction flow

---

## 1. Privy Wallet Access

### Getting EmbeddedEthereumWallet

```kotlin
// From PrivyAuthService - ensure wallet exists
suspend fun ensureEthereumWallet(user: PrivyUser): Result<EmbeddedEthereumWallet> {
    val existingWallet = user.embeddedEthereumWallets.firstOrNull()
    if (existingWallet != null) return Result.success(existingWallet)

    // Create if missing
    return user.createEthereumWallet(allowAdditional = false)
}

// Access in transaction context
val ethereumWallets = user.embeddedEthereumWallets
if (ethereumWallets.isNotEmpty()) {
    val wallet = ethereumWallets.first()
    val address = wallet.address          // "0x..."
    val chainId = wallet.chainId          // "0x1" (mainnet) or other
    val provider = wallet.provider        // RpcProvider for requests
}
```

**Key detail:** Wallet creation is automatic via `user.createEthereumWallet()`. Subsequent calls return existing wallet via `embeddedEthereumWallets` list. Private keys use Shamir secret sharing—never accessible to app or Privy.

---

## 2. Transaction Signing & Execution

### Primary Method: eth_sendTransaction via RpcProvider

```kotlin
// Transaction request structure
val transactionRequest = JSONObject().apply {
    put("from", wallet.address)          // Required: sender
    put("to", "0xRecipient...")          // Required: target contract/address
    put("value", "0x186a0")              // Optional: wei amount (hex)
    put("data", "0x...")                 // Optional: contract calldata (hex)
    put("chainId", "0x2105")             // Required: 8453 (Base) in hex
    put("gas", "0x5208")                 // Optional: gas limit (21000 for ETH transfer)
    put("gasPrice", "0x...")             // Optional: legacy gas price (hex)
    // OR for EIP-1559:
    put("maxFeePerGas", "0x...")         // Maximum fee per gas (wei, hex)
    put("maxPriorityFeePerGas", "0x...")  // Priority fee per gas (wei, hex)
}.toString()

// Send via provider
val result = wallet.provider.request(
    request = EthereumRpcRequest.ethSendTransaction(transactionRequest)
)

when (result) {
    is Result.Success -> {
        val txHash = result.data.data  // Transaction hash
        // Track tx, show confirmation UI
    }
    is Result.Failure -> {
        val error = result.exceptionOrNull()
        // Handle user rejection, insufficient balance, etc.
    }
}
```

**Behavior:** `provider.request()` suspends until user approves/rejects on-device. Signature happens locally—never leaves device. Returns tx hash upon broadcast.

---

## 3. Message Signing (for auth/verification)

```kotlin
// Sign typed data (EIP-712) - for off-chain auth
val messageHash = "0x..." // Pre-computed EIP-712 hash
val signResult = wallet.provider.request(
    request = EthereumRpcRequest.personalSign(
        message = messageHash,
        address = wallet.address
    )
)

when (signResult) {
    is Result.Success -> {
        val signature = signResult.data.data  // "0x..."
    }
    is Result.Failure -> { /* handle error */ }
}
```

---

## 4. Error Handling & Recovery

### Common Error Patterns

| Error | Cause | Recovery |
|-------|-------|----------|
| **UserRejected** | User declined signature | Retry or inform user |
| **InsufficientBalance** | Gas fees exceed balance | Top-up wallet, show warning |
| **InvalidGasLimit** | Gas estimation failed | Fallback to manual limit or check contract logic |
| **NetworkError** | RPC provider unavailable | Retry via `privy.onNetworkRestored()` |
| **InvalidChainId** | Wrong network selected | Verify chainId in request vs. wallet |

### Error Handling Code Pattern

```kotlin
suspend fun executeSwapTransaction(
    wallet: EmbeddedEthereumWallet,
    swapData: SwapRequest
): Result<String> = try {
    // 1. Validate preconditions
    if (swapData.amountInWei.isEmpty()) {
        return Result.failure(IllegalArgumentException("Invalid amount"))
    }

    // 2. Build transaction
    val txRequest = buildSwapTransaction(wallet, swapData)

    // 3. Send with user confirmation
    val result = wallet.provider.request(
        request = EthereumRpcRequest.ethSendTransaction(txRequest.toJsonString())
    )

    // 4. Handle result
    when {
        result.isSuccess -> {
            val txHash = result.getOrNull()?.data ?: ""
            Log.d(TAG, "Swap submitted: $txHash")
            Result.success(txHash)
        }
        result.isFailure -> {
            val error = result.exceptionOrNull()
            when (error) {
                is UserRejectedError -> {
                    Log.w(TAG, "User rejected swap")
                    Result.failure(UserRejectedError("Swap cancelled by user"))
                }
                else -> {
                    Log.e(TAG, "Swap failed", error)
                    Result.failure(error ?: Exception("Unknown error"))
                }
            }
        }
    }
} catch (e: Exception) {
    Log.e(TAG, "Exception during swap", e)
    Result.failure(e)
}
```

---

## 5. Gas Estimation (via RPC)

### Estimate gas before sending

```kotlin
// RPC call: eth_estimateGas
val estimateRequest = JSONObject().apply {
    put("from", wallet.address)
    put("to", contractAddress)
    put("data", calldata)        // Contract method encoded
    put("value", amountInWei)    // If sending ETH
}.toString()

val gasResult = wallet.provider.request(
    request = EthereumRpcRequest(
        method = "eth_estimateGas",
        params = arrayOf(estimateRequest)
    )
)

when (gasResult) {
    is Result.Success -> {
        val estimatedGas = gasResult.data.data.hexToBigInteger()
        // Add safety margin: estimatedGas * 1.2
        val safeGasLimit = estimatedGas * BigInteger.valueOf(120) / BigInteger.valueOf(100)
    }
    is Result.Failure -> {
        // Estimation failed—use fallback limit (300k for swaps)
        val fallbackGas = "0x927c0"  // 600,000
    }
}
```

### Get gas price (eth_gasPrice)

```kotlin
val priceResult = wallet.provider.request(
    request = EthereumRpcRequest(
        method = "eth_gasPrice",
        params = emptyArray()
    )
)

when (priceResult) {
    is Result.Success -> {
        val gasPrice = priceResult.data.data.hexToWei()  // Wei per gas
    }
    is Result.Failure -> {
        // Fallback: use fixed gwei or fetch from external API
    }
}
```

---

## 6. Integration with PrivyAuthService (existing code)

### Current strengths:
- ✅ Lazy initialization via `PrivyProvider.getInstance()`
- ✅ Wallet auto-creation on login
- ✅ Parallel wallet setup via `ensureWallets()`
- ✅ Auth state flow for reactive updates

### Extension points for KyberSwap:
1. Add `signTransaction()` method wrapping `eth_sendTransaction`
2. Add `estimateSwapGas()` method for pre-flight checks
3. Add `getGasPrice()` for fee display
4. Implement retry logic for flaky RPC providers

### Recommended addition to PrivyAuthService:

```kotlin
suspend fun sendTransaction(
    wallet: EmbeddedEthereumWallet,
    to: String,
    data: String,
    value: String = "0x0",
    gas: String? = null
): Result<String> {
    // Build, sign, broadcast transaction
    // Returns txHash or error
}

suspend fun estimateGas(
    wallet: EmbeddedEthereumWallet,
    to: String,
    data: String
): Result<String> {
    // Returns hex gas limit
}
```

---

## 7. Chain-Specific Configuration

By default, embedded wallets connect to **Ethereum mainnet**. For other chains (Base, Polygon, etc.):

```kotlin
// Set chainId in transaction request
val baseChainId = "0x2105"  // 8453 in decimal
val polygonChainId = "0x89"  // 137 in decimal

transactionRequest.put("chainId", baseChainId)
```

KyberSwap integration must:
1. Detect active wallet chainId
2. Route swap requests to correct contract on that chain
3. Confirm user before switching chains

---

## 8. Key Security Notes

- **On-device signing only:** Keys never leave device, Privy only facilitates
- **User confirmation required:** Every transaction/signature requires user approval
- **Single wallet per auth:** `allowAdditional: false` prevents key fragmentation
- **Recovery via Shamir:** Private keys are sharded—neither app nor Privy can recover independently

---

## Unresolved Questions

1. **RpcProvider request timeout:** What's default timeout for awaiting user signature? Is it configurable?
2. **Batch transaction support:** Can multiple swaps be batched into one tx via Privy's provider?
3. **EIP-1559 vs legacy gas:** Does Privy auto-detect or require explicit fee structure based on chain?
4. **On-ramp wallet funding:** After creating embedded wallet, what's the intended funding path? (External transfer, fiat on-ramp, etc.)

---

## Sources

- [Privy Send a Transaction](https://docs.privy.io/wallets/using-wallets/ethereum/send-a-transaction)
- [Privy Android Quickstart](https://docs.privy.io/basics/android/quickstart)
- [Privy Client-Side Error Codes](https://docs.privy.io/basics/troubleshooting/error-handling/client-errors)
- [dkrabdev/privy-wallet - GitHub Example](https://github.com/dkrabdev/privy-wallet)
- [Privy Biconomy Example](https://github.com/privy-io/biconomy-example)
- [Ethereum eth_estimateGas - MetaMask Docs](https://docs.metamask.io/services/reference/ethereum/json-rpc-methods/eth_estimategas/)

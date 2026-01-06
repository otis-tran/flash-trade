package com.otistran.flash_trade.domain.service

import com.otistran.flash_trade.domain.model.NetworkMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** EIP-2612 DOMAIN_SEPARATOR() selector */
private const val DOMAIN_SEPARATOR_SELECTOR = "0x3644e515"

/** EIP-2612 nonces(address) selector */
private const val NONCES_SELECTOR = "0x7ecebe00"

/** ERC20 name() selector */
private const val NAME_SELECTOR = "0x06fdde03"

/**
 * Service to detect token capabilities like EIP-2612 permit support.
 * Results are cached to minimize RPC calls.
 */
@Singleton
class TokenCapabilityService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** Cache for EIP-2612 support detection (key: chainId:tokenAddress) */
    private val eip2612Cache = ConcurrentHashMap<String, Boolean>()

    /** Cache for token names (key: chainId:tokenAddress) */
    private val tokenNameCache = ConcurrentHashMap<String, String>()

    /**
     * Check if token supports EIP-2612 permit.
     * Detection is done by calling DOMAIN_SEPARATOR() - if it returns
     * a non-empty 32-byte value, the token likely supports permit.
     *
     * @param tokenAddress Token contract address
     * @param chainId Chain ID
     * @return true if token supports EIP-2612 permit
     */
    suspend fun supportsEIP2612(tokenAddress: String, chainId: Long): Boolean {
        val cacheKey = "$chainId:$tokenAddress"

        // Return cached result if available
        eip2612Cache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val rpcUrl = NetworkMode.fromChainId(chainId).rpcUrl
                val result = callDomainSeparator(tokenAddress, rpcUrl)

                // EIP-2612 tokens return 32-byte DOMAIN_SEPARATOR
                val supportsPermit = result != null && 
                    result.length >= 66 && // 0x + 64 hex chars = 32 bytes
                    result != "0x" && 
                    result != "0x0000000000000000000000000000000000000000000000000000000000000000"

                Timber.d("Token $tokenAddress supportsEIP2612: $supportsPermit")

                // Cache result
                eip2612Cache[cacheKey] = supportsPermit
                supportsPermit

            } catch (e: Exception) {
                Timber.e("Error checking EIP-2612 support for $tokenAddress", e)
                eip2612Cache[cacheKey] = false
                false
            }
        }
    }

    /**
     * Get permit nonce for an address.
     * Required for EIP-2612 permit signing.
     *
     * @param tokenAddress Token contract address
     * @param owner User wallet address
     * @param chainId Chain ID
     * @return Current nonce, or null if not supported
     */
    suspend fun getPermitNonce(
        tokenAddress: String,
        owner: String,
        chainId: Long
    ): BigInteger? = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = NetworkMode.fromChainId(chainId).rpcUrl

            // Encode nonces(address): selector + address padded to 32 bytes
            val ownerPadded = owner.removePrefix("0x").padStart(64, '0')
            val calldata = "$NONCES_SELECTOR$ownerPadded"

            val requestBody = """
                {"jsonrpc":"2.0","method":"eth_call","params":[{"to":"$tokenAddress","data":"$calldata"},"latest"],"id":1}
            """.trimIndent()

            val result = executeRpcCall(requestBody, rpcUrl)

            if (result != null && result.startsWith("0x") && result.length > 2) {
                val hexValue = result.removePrefix("0x")
                if (hexValue.isNotEmpty()) BigInteger(hexValue, 16) else BigInteger.ZERO
            } else {
                null
            }

        } catch (e: Exception) {
            Timber.e("Error getting permit nonce for $owner on $tokenAddress", e)
            null
        }
    }

    /**
     * Get token name (used for EIP-712 domain).
     *
     * @param tokenAddress Token contract address
     * @param chainId Chain ID
     * @return Token name, or null if not available
     */
    suspend fun getTokenName(tokenAddress: String, chainId: Long): String? {
        val cacheKey = "$chainId:$tokenAddress"

        // Return cached result if available
        tokenNameCache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val rpcUrl = NetworkMode.fromChainId(chainId).rpcUrl

                val requestBody = """
                    {"jsonrpc":"2.0","method":"eth_call","params":[{"to":"$tokenAddress","data":"$NAME_SELECTOR"},"latest"],"id":1}
                """.trimIndent()

                val result = executeRpcCall(requestBody, rpcUrl)

                if (result != null && result.startsWith("0x") && result.length > 130) {
                    // ABI decode string: first 32 bytes = offset, next 32 bytes = length, then data
                    val data = result.removePrefix("0x")
                    val length = BigInteger(data.substring(64, 128), 16).toInt()
                    val nameHex = data.substring(128, 128 + length * 2)
                    val name = String(nameHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray())

                    Timber.d("Token $tokenAddress name: $name")
                    tokenNameCache[cacheKey] = name
                    name
                } else {
                    null
                }

            } catch (e: Exception) {
                Timber.e("Error getting token name for $tokenAddress", e)
                null
            }
        }
    }

    /**
     * Call DOMAIN_SEPARATOR() on token contract.
     */
    private fun callDomainSeparator(tokenAddress: String, rpcUrl: String): String? {
        val requestBody = """
            {"jsonrpc":"2.0","method":"eth_call","params":[{"to":"$tokenAddress","data":"$DOMAIN_SEPARATOR_SELECTOR"},"latest"],"id":1}
        """.trimIndent()

        return executeRpcCall(requestBody, rpcUrl)
    }

    /**
     * Execute RPC call and return raw hex result.
     */
    private fun executeRpcCall(requestBody: String, rpcUrl: String): String? {
        return try {
            val request = Request.Builder()
                .url(rpcUrl)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Timber.e("RPC call failed: ${response.code}")
                return null
            }

            val jsonResponse = json.parseToJsonElement(responseBody)
            jsonResponse.jsonObject["result"]?.jsonPrimitive?.content

        } catch (e: IOException) {
            Timber.e("Network error in RPC call", e)
            null
        }
    }
}

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

private const val DOMAIN_SEPARATOR_SELECTOR = "0x3644e515"
private const val NONCES_SELECTOR = "0x7ecebe00"
private const val NAME_SELECTOR = "0x06fdde03"

/**
 * Detects EIP-2612 permit support for tokens via RPC calls.
 *
 * Results are cached to minimize network overhead. Detection works by calling
 * DOMAIN_SEPARATOR() on the token contract - a valid 32-byte response indicates support.
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-2612">EIP-2612</a>
 */
@Singleton
class TokenCapabilityService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val eip2612Cache = ConcurrentHashMap<String, Boolean>()
    private val tokenNameCache = ConcurrentHashMap<String, String>()

    /**
     * Checks if a token supports EIP-2612 permit.
     *
     * @param tokenAddress Token contract address
     * @param chainId Network chain ID
     * @return true if token supports gasless permit approvals
     */
    suspend fun supportsEIP2612(tokenAddress: String, chainId: Long): Boolean {
        val cacheKey = "$chainId:$tokenAddress"
        eip2612Cache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val rpcUrl = NetworkMode.fromChainId(chainId).rpcUrl
                val result = callDomainSeparator(tokenAddress, rpcUrl)

                val supportsPermit = result != null &&
                    result.length >= 66 &&
                    result != "0x" &&
                    result != "0x0000000000000000000000000000000000000000000000000000000000000000"

                Timber.d("Token $tokenAddress supportsEIP2612: $supportsPermit")
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
     * Gets the permit nonce for a wallet address.
     *
     * @param tokenAddress Token contract address
     * @param owner Wallet address
     * @param chainId Network chain ID
     * @return Current nonce, or null if unavailable
     */
    suspend fun getPermitNonce(
        tokenAddress: String,
        owner: String,
        chainId: Long
    ): BigInteger? = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = NetworkMode.fromChainId(chainId).rpcUrl
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
     * Gets the token name for EIP-712 domain construction.
     *
     * @param tokenAddress Token contract address
     * @param chainId Network chain ID
     * @return Token name, or null if unavailable
     */
    suspend fun getTokenName(tokenAddress: String, chainId: Long): String? {
        val cacheKey = "$chainId:$tokenAddress"
        tokenNameCache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val rpcUrl = NetworkMode.fromChainId(chainId).rpcUrl

                val requestBody = """
                    {"jsonrpc":"2.0","method":"eth_call","params":[{"to":"$tokenAddress","data":"$NAME_SELECTOR"},"latest"],"id":1}
                """.trimIndent()

                val result = executeRpcCall(requestBody, rpcUrl)

                if (result != null && result.startsWith("0x") && result.length > 130) {
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

    private fun callDomainSeparator(tokenAddress: String, rpcUrl: String): String? {
        val requestBody = """
            {"jsonrpc":"2.0","method":"eth_call","params":[{"to":"$tokenAddress","data":"$DOMAIN_SEPARATOR_SELECTOR"},"latest"],"id":1}
        """.trimIndent()

        return executeRpcCall(requestBody, rpcUrl)
    }

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

            json.parseToJsonElement(responseBody).jsonObject["result"]?.jsonPrimitive?.content

        } catch (e: IOException) {
            Timber.e("Network error in RPC call", e)
            null
        }
    }
}


package com.otistran.flash_trade.domain.service

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.util.Result
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Native token placeholder address */
private const val NATIVE_TOKEN_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"

/** ERC20 balanceOf(address) function selector */
private const val BALANCE_OF_SELECTOR = "0x70a08231"

/**
 * Service to check token balances before swap execution.
 * Supports both native ETH and ERC20 tokens.
 */
@Singleton
class BalanceChecker @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get balance of native token or ERC20 token.
     *
     * @param address User wallet address
     * @param tokenAddress Token contract address (or native token placeholder)
     * @param chainId Chain ID
     * @return Balance in wei/smallest unit
     */
    suspend fun getBalance(
        address: String,
        tokenAddress: String,
        chainId: Long
    ): Result<BigInteger> = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = NetworkMode.fromChainId(chainId).rpcUrl

            val balance = if (isNativeToken(tokenAddress)) {
                getEthBalance(address, rpcUrl)
            } else {
                getErc20Balance(address, tokenAddress, rpcUrl)
            }

            if (balance != null) {
                Timber.d("Balance for $address: $balance")
                Result.Success(balance)
            } else {
                Result.Error("Failed to get balance")
            }

        } catch (e: IOException) {
            Timber.e("Network error getting balance", e)
            Result.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Timber.e("Error getting balance", e)
            Result.Error("Error: ${e.message}")
        }
    }

    /**
     * Check if user has enough balance for the swap.
     */
    suspend fun hasEnoughBalance(
        address: String,
        tokenAddress: String,
        requiredAmount: BigInteger,
        chainId: Long
    ): Boolean {
        val result = getBalance(address, tokenAddress, chainId)
        return if (result is Result.Success) {
            result.data >= requiredAmount
        } else {
            false
        }
    }

    /**
     * Get native ETH balance using eth_getBalance RPC call.
     */
    private fun getEthBalance(address: String, rpcUrl: String): BigInteger? {
        val requestBody = """
            {"jsonrpc":"2.0","method":"eth_getBalance","params":["$address","latest"],"id":1}
        """.trimIndent()

        return executeRpcCall(requestBody, rpcUrl)
    }

    /**
     * Get ERC20 token balance using eth_call to balanceOf(address).
     */
    private fun getErc20Balance(
        owner: String,
        tokenAddress: String,
        rpcUrl: String
    ): BigInteger? {
        // Encode balanceOf(address): selector + address padded to 32 bytes
        val ownerPadded = owner.removePrefix("0x").padStart(64, '0')
        val calldata = "$BALANCE_OF_SELECTOR$ownerPadded"

        val requestBody = """
            {"jsonrpc":"2.0","method":"eth_call","params":[{"to":"$tokenAddress","data":"$calldata"},"latest"],"id":1}
        """.trimIndent()

        return executeRpcCall(requestBody, rpcUrl)
    }

    /**
     * Execute RPC call and parse hex result to BigInteger.
     */
    private fun executeRpcCall(requestBody: String, rpcUrl: String): BigInteger? {
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
        val result = jsonResponse.jsonObject["result"]?.jsonPrimitive?.content

        return if (result != null && result.startsWith("0x")) {
            val hexValue = result.removePrefix("0x")
            if (hexValue.isEmpty()) BigInteger.ZERO else BigInteger(hexValue, 16)
        } else {
            null
        }
    }

    private fun isNativeToken(address: String): Boolean {
        return address.equals(NATIVE_TOKEN_ADDRESS, ignoreCase = true)
    }
}

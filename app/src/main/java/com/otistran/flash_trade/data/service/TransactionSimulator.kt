package com.otistran.flash_trade.data.service

import com.otistran.flash_trade.data.service.model.SimulationResult
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Error(string) selector for standard reverts */
private const val ERROR_SELECTOR = "0x08c379a0"

/** Panic(uint256) selector for Solidity panics */
private const val PANIC_SELECTOR = "0x4e487b71"

/**
 * Service to simulate transactions using eth_call before execution.
 * Detects reverts and decodes error messages to prevent wasted gas.
 */
@Singleton
class TransactionSimulator @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Simulate a transaction using eth_call.
     * This checks if the transaction would succeed without actually executing it.
     *
     * @param from Sender address
     * @param to Contract address (router)
     * @param data Encoded transaction data
     * @param value Transaction value in hex (e.g., "0x0")
     * @param chainId Chain ID
     * @return SimulationResult with success status and revert reason if failed
     */
    suspend fun simulate(
        from: String,
        to: String,
        data: String,
        value: String,
        chainId: Long
    ): SimulationResult = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = NetworkMode.fromChainId(chainId).rpcUrl

            Timber.d("Simulating tx: from=$from, to=$to, value=$value")

            val callObject = """{"from":"$from","to":"$to","data":"$data","value":"$value"}"""
            val requestBody = """{"jsonrpc":"2.0","method":"eth_call","params":[$callObject,"latest"],"id":1}"""

            val request = Request.Builder()
                .url(rpcUrl)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Timber.e("RPC call failed: ${response.code}")
                return@withContext SimulationResult(
                    success = false,
                    revertReason = "RPC call failed: ${response.code}"
                )
            }

            parseSimulationResponse(responseBody)

        } catch (e: IOException) {
            Timber.e("Network error during simulation", e)
            SimulationResult(
                success = false,
                revertReason = "Network error: ${e.message}"
            )
        } catch (e: Exception) {
            Timber.e("Error during simulation", e)
            SimulationResult(
                success = false,
                revertReason = "Simulation error: ${e.message}"
            )
        }
    }

    /**
     * Parse JSON-RPC response from eth_call.
     */
    private fun parseSimulationResponse(responseBody: String): SimulationResult {
        val jsonResponse = json.parseToJsonElement(responseBody)
        val jsonObj = jsonResponse.jsonObject

        // Check for error in response (revert)
        val error = jsonObj["error"]
        if (error != null) {
            val errorObj = error.jsonObject
            val message = errorObj["message"]?.jsonPrimitive?.content ?: "Unknown error"
            val errorData = errorObj["data"]?.jsonPrimitive?.content

            val revertReason = if (errorData != null) {
                decodeRevertReason(errorData)
            } else {
                message
            }

            Timber.w("Simulation failed: $revertReason")
            return SimulationResult(
                success = false,
                revertReason = revertReason
            )
        }

        // Success - check result
        val result = jsonObj["result"]?.jsonPrimitive?.content

        Timber.d("Simulation succeeded")
        return SimulationResult(
            success = true,
            returnData = result
        )
    }

    /**
     * Decode revert reason from error data.
     * Handles Error(string), Panic(uint256), and custom errors.
     */
    private fun decodeRevertReason(errorData: String): String {
        if (errorData.length < 10) return "Unknown error"

        val selector = errorData.substring(0, 10).lowercase()
        val data = errorData.substring(10)

        return when (selector) {
            ERROR_SELECTOR.lowercase() -> decodeErrorString(data)
            PANIC_SELECTOR.lowercase() -> decodePanicCode(data)
            else -> {
                // Try to decode as Error(string) anyway for some providers
                val decoded = decodeErrorString(data)
                if (decoded != "Malformed error" && decoded != "Unknown error") {
                    decoded
                } else {
                    "Custom error: $selector"
                }
            }
        }
    }

    /**
     * Decode Error(string) revert message.
     * ABI encoding: offset (32 bytes) + length (32 bytes) + string data
     */
    private fun decodeErrorString(data: String): String {
        return try {
            if (data.length < 128) return "Malformed error"

            // Skip offset (first 32 bytes / 64 hex chars), read length (next 32 bytes)
            val length = BigInteger(data.substring(64, 128), 16).toInt()

            if (length == 0 || 128 + length * 2 > data.length) {
                return "Malformed error"
            }

            val msgHex = data.substring(128, 128 + length * 2)
            String(msgHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray())

        } catch (e: Exception) {
            Timber.e("Error decoding revert string", e)
            "Unknown error"
        }
    }

    /**
     * Decode Panic(uint256) code to human-readable message.
     */
    private fun decodePanicCode(data: String): String {
        return try {
            if (data.length < 64) return "Panic: unknown code"

            val code = BigInteger(data.substring(0, 64), 16).toInt()

            when (code) {
                0x00 -> "Panic: generic/compiler error"
                0x01 -> "Panic: assertion failed"
                0x11 -> "Panic: arithmetic overflow/underflow"
                0x12 -> "Panic: division by zero"
                0x21 -> "Panic: invalid enum conversion"
                0x22 -> "Panic: storage encoding error"
                0x31 -> "Panic: pop on empty array"
                0x32 -> "Panic: array index out of bounds"
                0x41 -> "Panic: memory allocation error"
                0x51 -> "Panic: uninitialized function pointer"
                else -> "Panic: code 0x${code.toString(16)}"
            }

        } catch (e: Exception) {
            Timber.e("Error decoding panic code", e)
            "Panic: unknown code"
        }
    }
}

package com.otistran.flash_trade.data.service

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.TransactionReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

/**
 * Service to poll for transaction receipts via RPC.
 * Used to verify on-chain execution status after broadcast.
 */
@Singleton
class TransactionReceiptService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Wait for transaction receipt by polling RPC endpoint.
     *
     * @param txHash Transaction hash (0x-prefixed)
     * @param chainId Chain ID for RPC URL lookup
     * @param maxAttempts Maximum polling attempts (default 30)
     * @param delayMs Delay between polls in ms (default 2000)
     * @return TransactionReceipt if found, null if timeout
     */
    suspend fun waitForReceipt(
        txHash: String,
        chainId: Long,
        maxAttempts: Int = 30,
        delayMs: Long = 2000L
    ): TransactionReceipt? = withContext(Dispatchers.IO) {
        Timber.d("Waiting for receipt: $txHash on chain $chainId")

        val rpcUrl = NetworkMode.fromChainId(chainId).rpcUrl

        repeat(maxAttempts) { attempt ->
            val receipt = getTransactionReceipt(txHash, rpcUrl)

            if (receipt != null) {
                Timber.i("Receipt found at attempt ${attempt + 1}: status=${receipt.status}")
                return@withContext receipt
            }

            Timber.d("Attempt ${attempt + 1}/$maxAttempts: receipt not yet available")
            delay(delayMs)
        }

        Timber.w("Timeout waiting for receipt after $maxAttempts attempts")
        null
    }

    /**
     * Single RPC call to get transaction receipt.
     */
    private fun getTransactionReceipt(txHash: String, rpcUrl: String): TransactionReceipt? {
        return try {
            val requestBody = """
                {"jsonrpc":"2.0","method":"eth_getTransactionReceipt","params":["$txHash"],"id":1}
            """.trimIndent()

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

            parseReceiptResponse(responseBody)

        } catch (e: IOException) {
            Timber.e("Network error getting receipt", e)
            null
        } catch (e: Exception) {
            Timber.e("Error parsing receipt", e)
            null
        }
    }

    /**
     * Parse JSON-RPC response into TransactionReceipt.
     */
    private fun parseReceiptResponse(responseBody: String): TransactionReceipt? {
        val jsonResponse = json.parseToJsonElement(responseBody)
        val result = jsonResponse.jsonObject["result"]

        // result is null if tx not yet mined
        if (result == null || result.toString() == "null") {
            return null
        }

        val resultObj = result.jsonObject

        val txHash = resultObj["transactionHash"]?.jsonPrimitive?.content ?: return null
        val statusHex = resultObj["status"]?.jsonPrimitive?.content ?: return null
        val blockNumberHex = resultObj["blockNumber"]?.jsonPrimitive?.content ?: return null
        val gasUsedHex = resultObj["gasUsed"]?.jsonPrimitive?.content ?: return null

        // Parse values (status: 0x1=success, 0x0=fail)
        val status = statusHex == "0x1"
        val blockNumber = BigInteger(blockNumberHex.removePrefix("0x"), 16)
        val gasUsed = BigInteger(gasUsedHex.removePrefix("0x"), 16)

        return TransactionReceipt(
            transactionHash = txHash,
            status = status,
            blockNumber = blockNumber,
            gasUsed = gasUsed
        )
    }
}

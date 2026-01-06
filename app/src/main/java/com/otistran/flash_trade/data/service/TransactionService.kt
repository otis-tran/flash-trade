package com.otistran.flash_trade.data.service

import android.os.Build
import androidx.annotation.RequiresApi
import com.otistran.flash_trade.domain.model.NetworkMode
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import io.privy.wallet.ethereum.EthereumRpcRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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

@Singleton
class TransactionService @Inject constructor(
    private val privyAuthService: PrivyAuthService
) {

    suspend fun sendTransaction(
        to: String,
        data: String,
        value: BigInteger,
        chainId: Long,
        gas: BigInteger? = null
    ): Result<String> {
        return try {
            val user = privyAuthService.getUser()
                ?: return Result.failure(Exception("User not authenticated"))

            val walletResult = privyAuthService.ensureEthereumWallet(user)
            if (walletResult.isFailure) {
                return Result.failure(
                    walletResult.exceptionOrNull() ?: Exception("Failed to get wallet")
                )
            }

            val wallet = walletResult.getOrNull()
                ?: return Result.failure(Exception("Wallet is null"))

            val txHash = executeTransaction(
                wallet = wallet,
                to = to,
                data = data,
                value = value,
                chainId = chainId,
                gas = gas
            )

            if (txHash != null) {
                Timber.i("Transaction sent: $txHash on chain $chainId")
                Result.success(txHash)
            } else {
                Result.failure(Exception("Transaction rejected or failed"))
            }

        } catch (e: Exception) {
            Timber.e("Failed to send transaction", e)
            Result.failure(e)
        }
    }

    private suspend fun executeTransaction(
        wallet: EmbeddedEthereumWallet,
        to: String,
        data: String,
        value: BigInteger,
        chainId: Long,
        gas: BigInteger?
    ): String? {
        val txJson = buildJsonObject {
            put("to", to)
            put("data", data)
            put("value", "0x${value.toString(16)}")
            put("chainId", "0x${chainId.toString(16)}")
            gas?.let {
                put("gas", "0x${it.toString(16)}")
            }
        }.toString()

        Timber.d("Sending tx: to=$to, chainId=$chainId, value=$value")

        val request = EthereumRpcRequest.ethSendTransaction(txJson)
        val result = wallet.provider.request(request)

        return result.getOrNull() as? String
    }

    suspend fun sendApproval(
        tokenAddress: String,
        spenderAddress: String,
        amount: BigInteger,
        chainId: Long
    ): Result<String> {
        val functionSelector = "0x095ea7b3"
        val spenderPadded = spenderAddress.removePrefix("0x").padStart(64, '0')
        val amountPadded = amount.toString(16).padStart(64, '0')
        val calldata = "$functionSelector$spenderPadded$amountPadded"

        Timber.d("ERC20 approve encoded: $calldata")

        return sendTransaction(
            to = tokenAddress,
            data = calldata,
            value = BigInteger.ZERO,
            chainId = chainId,
            gas = BigInteger.valueOf(60000)
        )
    }

    suspend fun getAllowance(
        tokenAddress: String,
        owner: String,
        spender: String,
        chainId: Long
    ): Result<BigInteger> {
        return withContext(Dispatchers.IO) {
            try {
                // Get RPC URL for the chain
                val network = NetworkMode.fromChainId(chainId)
                val rpcUrl = network.rpcUrl

                // Encode allowance call: allowance(address owner, address spender)
                val ownerPadded = owner.removePrefix("0x").padStart(64, '0')
                val spenderPadded = spender.removePrefix("0x").padStart(64, '0')
                val calldata = "$ERC20_ALLOWANCE_SELECTOR$ownerPadded$spenderPadded"

                Timber.d("Checking allowance: token=$tokenAddress, owner=$owner, spender=$spender, chainId=$chainId")

                // Create eth_call request body (manual JSON for simplicity)
                val callObject = """{"to":"$tokenAddress","data":"$calldata"}"""
                val requestBody = """{"jsonrpc":"2.0","method":"eth_call","params":[$callObject,"latest"],"id":1}"""

                // OkHttp client with timeout
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(rpcUrl)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    Timber.e("RPC call failed: ${response.code}")
                    return@withContext Result.failure(Exception("RPC call failed: ${response.code}"))
                }

                // Parse JSON response
                val json = Json { ignoreUnknownKeys = true }
                val jsonResponse = json.parseToJsonElement(responseBody)
                val resultElement = jsonResponse.jsonObject["result"]

                if (resultElement != null) {
                    // Get the raw string value from JSON primitive (removes quotes automatically)
                    val hexResult = resultElement.jsonPrimitive.content
                    if (hexResult.isNotEmpty() && hexResult != "null") {
                        // Remove 0x prefix and parse as hex
                        val allowance = BigInteger(hexResult.removePrefix("0x"), 16)
                        Timber.d("Allowance retrieved successfully: $allowance")
                        Result.success(allowance)
                    } else {
                        Timber.e("eth_call returned empty or null result")
                        Result.failure(Exception("Empty allowance data"))
                    }
                } else {
                    // Check for error in response
                    val error = jsonResponse.jsonObject["error"]
                    if (error != null) {
                        val errorMsg = error.jsonObject["message"]?.toString() ?: error.toString()
                        Timber.e("eth_call error: $errorMsg")
                        return@withContext Result.failure(Exception("RPC error: $errorMsg"))
                    }
                    Timber.e("eth_call returned empty result")
                    Result.failure(Exception("Empty allowance data"))
                }

            } catch (e: IOException) {
                Timber.e("Network error checking allowance", e)
                Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                Timber.e("Failed to check allowance", e)
                Result.failure(e)
            }
        }
    }

    fun isNativeToken(address: String): Boolean {
        return address.equals(NATIVE_TOKEN_ADDRESS, ignoreCase = true)
    }

    companion object {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        val MAX_UINT256: BigInteger = BigInteger.TWO.pow(256).subtract(BigInteger.ONE)

        const val NATIVE_TOKEN_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"
        private const val ERC20_ALLOWANCE_SELECTOR = "0xdd62ed3e"
    }
}

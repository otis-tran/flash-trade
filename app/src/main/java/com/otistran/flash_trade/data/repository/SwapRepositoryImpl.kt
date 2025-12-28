package com.otistran.flash_trade.data.repository

import android.util.Log
import com.otistran.flash_trade.data.mapper.SwapMapper
import com.otistran.flash_trade.data.remote.api.KyberSwapApiService
import com.otistran.flash_trade.data.remote.dto.kyber.BuildRouteRequestDto
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.manager.QuoteCacheManager
import com.otistran.flash_trade.domain.model.EncodedSwap
import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.model.SwapResult
import com.otistran.flash_trade.domain.model.SwapStatus
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.util.Result
import java.math.BigInteger
import javax.inject.Inject

private const val TAG = "SwapRepositoryImpl"

class SwapRepositoryImpl @Inject constructor(
    private val kyberSwapApi: KyberSwapApiService,
    private val privyAuthService: PrivyAuthService,
    private val swapMapper: SwapMapper,
    private val quoteCacheManager: QuoteCacheManager
) : SwapRepository {

    // Store checksum for build request (needed by KyberSwap API)
    private var lastChecksum: String = ""

    override suspend fun getQuote(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger,
        slippageTolerance: Int,
        userAddress: String?
    ): Result<Quote> {
        return try {
            // Check cache first
            val cached = quoteCacheManager.get(tokenIn, tokenOut, amountIn.toString())
            if (cached != null) {
                Log.d(TAG, "Quote cache hit")
                return Result.Success(cached)
            }

            // Fetch from API
            val response = kyberSwapApi.getSwapRoute(
                chain = chain,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn.toString(),
                slippageTolerance = slippageTolerance,
                origin = userAddress
            )

            if (response.code != 0) {
                return Result.Error("Failed to get quote: ${response.message}")
            }

            if (response.data == null) {
                return Result.Error("No route found for this swap")
            }

            // Store checksum for later build request
            lastChecksum = response.data.routeSummary.checksum

            val quote = swapMapper.toQuote(response)
            quoteCacheManager.put(quote)

            Result.Success(quote)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting quote", e)
            Result.Error(e.message ?: "Unknown error getting quote")
        }
    }

    override suspend fun buildSwap(
        chain: String,
        quote: Quote,
        senderAddress: String,
        recipientAddress: String?
    ): Result<EncodedSwap> {
        return try {
            val request = BuildRouteRequestDto(
                routeSummary = swapMapper.toRouteSummaryDto(quote, lastChecksum),
                sender = senderAddress,
                recipient = recipientAddress ?: senderAddress,
                slippageTolerance = 50, // 0.5% default
                enableGasEstimation = true,
                origin = senderAddress
            )

            val response = kyberSwapApi.buildSwapRoute(chain = chain, request = request)

            if (response.code != 0) {
                return Result.Error("Failed to build swap: ${response.message}")
            }

            if (response.data == null) {
                return Result.Error("No swap data returned")
            }

            val encodedSwap = swapMapper.toEncodedSwap(response)
            Result.Success(encodedSwap)
        } catch (e: Exception) {
            Log.e(TAG, "Error building swap", e)
            Result.Error(e.message ?: "Unknown error building swap")
        }
    }

    override suspend fun executeSwap(
        encodedSwap: EncodedSwap
    ): Result<SwapResult> {
        return try {
            // Get authenticated user
            val user = privyAuthService.getUser()
                ?: return Result.Error("User not authenticated")

            // Ensure wallet exists
            val walletResult = privyAuthService.ensureEthereumWallet(user)
            if (walletResult.isFailure) {
                return Result.Error(
                    "Failed to get wallet: ${walletResult.exceptionOrNull()?.message}"
                )
            }

            val wallet = walletResult.getOrNull()
                ?: return Result.Error("Wallet is null")

            // Send transaction via Privy wallet provider
            // Note: Actual implementation depends on Privy SDK version
            val txHash = sendTransaction(
                wallet = wallet,
                to = encodedSwap.routerAddress,
                data = encodedSwap.calldata,
                value = encodedSwap.value,
                gas = encodedSwap.gas
            )

            if (txHash != null) {
                Log.d(TAG, "Swap submitted: $txHash")
                // Invalidate cache after successful swap
                quoteCacheManager.clear()
                Result.Success(
                    SwapResult(
                        txHash = txHash,
                        status = SwapStatus.PENDING
                    )
                )
            } else {
                Result.Error("Transaction rejected or failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during swap execution", e)
            Result.Error(e.message ?: "Unknown error executing swap")
        }
    }

    /**
     * Send transaction via Privy embedded wallet.
     * Uses Privy SDK's EthereumRpcRequest for eth_sendTransaction.
     * @return Transaction hash or null if failed
     */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun sendTransaction(
        wallet: io.privy.wallet.ethereum.EmbeddedEthereumWallet,
        to: String,
        data: String,
        value: BigInteger,
        gas: BigInteger
    ): String? {
        // TODO: Implement actual transaction sending when Privy SDK API is confirmed
        // The implementation will depend on the exact Privy SDK version.
        // Expected flow:
        // 1. Build transaction params (from, to, data, value, gas)
        // 2. Call wallet.provider.request() with eth_sendTransaction
        // 3. Return transaction hash from result
        Log.w(TAG, "Transaction sending not yet implemented")
        return null
    }
}

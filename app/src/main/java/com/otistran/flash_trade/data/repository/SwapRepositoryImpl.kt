package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.mapper.toRequest
import com.otistran.flash_trade.data.mapper.toRouteSummary
import com.otistran.flash_trade.data.remote.api.KyberSwapApiService
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.model.BuildRouteData
import com.otistran.flash_trade.domain.model.EncodedRouteResponse
import com.otistran.flash_trade.domain.model.RouteSummaryResponse
import com.otistran.flash_trade.domain.model.SwapQuote
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.util.Result
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import javax.inject.Inject

/** Gas multiplier to add buffer for transaction execution (1.5 = 50% buffer) */
private const val GAS_MULTIPLIER = 1.5

class SwapRepositoryImpl @Inject constructor(
    private val kyberSwapApi: KyberSwapApiService,
    private val privyAuthService: PrivyAuthService
) : SwapRepository {
    override suspend fun getRoutes(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger
    ): Result<RouteSummaryResponse> {
        return try {
            val response = kyberSwapApi.getSwapRoute(
                chain = chain,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn.toString()
            )

            if (response.code != 0) {
                return Result.Error("Failed to get route: ${response.message}")
            }

            if (response.data == null) {
                return Result.Error("No route found for this swap")
            }

            if (response.data.routeSummary == null) {
                return Result.Error("Route summary is null")
            }

            val routeSummary = response.data.toRouteSummary()
            Result.Success(routeSummary)
        } catch (e: Exception) {
            Timber.e("Error getting route", e)
            Result.Error(e.message ?: "Unknown error getting route")
        }
    }

    override suspend fun getQuote(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger,
        tokenInDecimals: Int,
        tokenOutDecimals: Int
    ): Result<SwapQuote> {
        return try {
            val response = kyberSwapApi.getSwapRoute(
                chain = chain,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                amountIn = amountIn.toString()
            )

            if (response.code != 0 || response.data?.routeSummary == null) {
                return Result.Error(response.message ?: "No route found")
            }

            val summary = response.data.routeSummary!!

            // Parse amounts
            val amtIn = BigDecimal(summary.amountIn ?: "0")
                .divide(BigDecimal.TEN.pow(tokenInDecimals), 18, RoundingMode.HALF_UP)
            val amtOut = BigDecimal(summary.amountOut ?: "0")
                .divide(BigDecimal.TEN.pow(tokenOutDecimals), 18, RoundingMode.HALF_UP)
            val amtInUsd = summary.amountInUsd?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val amtOutUsd = summary.amountOutUsd?.toBigDecimalOrNull() ?: BigDecimal.ZERO

            // Calculate exchange rate
            val rate = if (amtIn > BigDecimal.ZERO) {
                amtOut.divide(amtIn, 18, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO

            // Parse network fee
            val gasUsd = summary.gasUsd?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val gasNative = BigDecimal(summary.gas ?: "0")
                .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP) // Gas in ETH

            // Calculate price impact
            // Impact = ((amtInUsd - amtOutUsd) / amtInUsd) * 100
            val priceImpact = if (amtInUsd > BigDecimal.ZERO) {
                amtInUsd.subtract(amtOutUsd)
                    .divide(amtInUsd, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal("100"))
                    .negate() // Negative means user receives less
            } else BigDecimal.ZERO

            Timber.d("Quote: rate=$rate, gasUsd=$gasUsd, priceImpact=$priceImpact%")

            Result.Success(
                SwapQuote(
                    amountIn = amtIn,
                    amountInUsd = amtInUsd,
                    amountOut = amtOut,
                    amountOutUsd = amtOutUsd,
                    exchangeRate = rate,
                    networkFeeUsd = gasUsd,
                    networkFeeNative = gasNative,
                    priceImpactPercent = priceImpact
                )
            )
        } catch (e: Exception) {
            Timber.e("Error getting quote", e)
            Result.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun buildEncodedRoute(
        chain: String,
        routeSummary: RouteSummaryResponse,
        senderAddress: String,
        recipientAddress: String?,
        permit: String?,
        deadline: Long?
    ): Result<EncodedRouteResponse> {
        return try {
            val request = routeSummary.toRequest(
                sender = senderAddress,
                receipt = recipientAddress ?: senderAddress,
                permit = permit,
                deadline = deadline
            )

            val response = kyberSwapApi.buildSwapRoute(chain = chain, request = request)

            if (response.code != 0) {
                return Result.Error("Failed to build encoded route: ${response.message}")
            }

            if (response.data == null) {
                return Result.Error("No route data returned")
            }

            val encodedSwapRoute = EncodedRouteResponse(
                code = response.code,
                message = response.message,
                data = BuildRouteData(
                    amountIn = response.data.amountIn,
                    amountInUsd = response.data.amountInUsd,
                    amountOut = response.data.amountOut,
                    amountOutUsd = response.data.amountOutUsd,
                    gas = response.data.gas,
                    gasUsd = response.data.gasUsd,
                    additionalCostUsd = response.data.additionalCostUsd,
                    additionalCostMessage = response.data.additionalCostMessage,
                    data = response.data.data,
                    routerAddress = response.data.routerAddress,
                    transactionValue = response.data.transactionValue,
                ),
                requestId = response.requestId,
            )
            Result.Success(encodedSwapRoute)
        } catch (e: Exception) {
            Timber.e("Error building encoded route", e)
            Result.Error(e.message ?: "Unknown error building encoded route")
        }
    }

    override suspend fun signTransaction(
        wallet: io.privy.wallet.ethereum.EmbeddedEthereumWallet,
        encodedRoute: EncodedRouteResponse,
        chainId: String,
        senderAddress: String
    ): Result<String> {
        return try {
            val routeData = encodedRoute.data
                ?: return Result.Error("No route data available")

            val to = routeData.routerAddress
                ?: return Result.Error("Router address is null")

            val value = routeData.transactionValue
            val data = routeData.data
                ?: return Result.Error("Encoded data is null")
            val gas = routeData.gas
                ?: return Result.Error("Gas is null")

            // Apply gas multiplier to add buffer for transaction execution
            val gasLimit = BigDecimal(gas)
                .multiply(BigDecimal(GAS_MULTIPLIER))
                .toBigInteger()

            Timber.d("Gas limit: original=$gas, adjusted=$gasLimit (multiplier=$GAS_MULTIPLIER)")

            val signResult = privyAuthService.signTransaction(
                wallet = wallet,
                to = to,
                value = value,
                chainId = chainId,
                data = data,
                gasLimit = "0x${gasLimit.toString(16)}",
                from = senderAddress
            )

            // Convert kotlin.Result to our custom Result type
            signResult.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(it.message ?: "Failed to sign transaction") }
            )
        } catch (e: Exception) {
            Timber.e("Error signing transaction", e)
            Result.Error(e.message ?: "Unknown error signing transaction")
        }
    }
}

package com.otistran.flash_trade.domain.usecase.swap.step

import com.otistran.flash_trade.domain.model.RouteSummaryResponse
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.domain.service.BalanceChecker
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

/**
 * Result of pre-validation step.
 */
data class PreValidationResult(
    val balance: BigInteger,
    val routeSummary: RouteSummaryResponse
)

/**
 * Step 1: Parallel pre-validation
 * - Checks token balance
 * - Fetches initial route
 */
class PreValidationStep @Inject constructor(
    private val balanceChecker: BalanceChecker,
    private val swapRepository: SwapRepository
) {
    /**
     * Execute pre-validation in parallel.
     *
     * @param userAddress User wallet address
     * @param tokenInAddress Token to sell address
     * @param tokenInSymbol Token to sell symbol (for error message)
     * @param tokenOutAddress Token to buy address
     * @param amountIn Amount to swap in wei
     * @param chainId Chain ID
     * @param chainName Chain name for API
     * @return PreValidationResult or error
     */
    suspend fun execute(
        userAddress: String,
        tokenInAddress: String,
        tokenInSymbol: String,
        tokenOutAddress: String,
        amountIn: BigInteger,
        chainId: Long,
        chainName: String
    ): Result<PreValidationResult> {
        Timber.d("Starting parallel pre-validation")

        val (balanceResult, routeResult) = coroutineScope {
            val balanceDeferred = async {
                balanceChecker.getBalance(userAddress, tokenInAddress, chainId)
            }
            val routeDeferred = async {
                swapRepository.getRoutes(chainName, tokenInAddress, tokenOutAddress, amountIn)
            }
            Pair(balanceDeferred.await(), routeDeferred.await())
        }

        // Validate balance
        if (balanceResult is Result.Error) {
            Timber.e("Failed to check balance: ${balanceResult.message}")
            return Result.Error("Failed to check balance: ${balanceResult.message}")
        }
        val balance = (balanceResult as Result.Success).data
        if (balance < amountIn) {
            Timber.e("Insufficient balance: $balance < $amountIn")
            return Result.Error("Insufficient $tokenInSymbol balance")
        }
        Timber.d("Balance OK: $balance >= $amountIn")

        // Validate route
        if (routeResult is Result.Error) {
            Timber.e("Failed to get route: ${routeResult.message}")
            return Result.Error("Failed to get route: ${routeResult.message}")
        }
        val routeSummary = (routeResult as Result.Success).data
        Timber.d("Route received: ${routeSummary.amountOut}")

        return Result.Success(PreValidationResult(balance, routeSummary))
    }
}

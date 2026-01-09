package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.domain.usecase.swap.step.PreValidationStep
import com.otistran.flash_trade.presentation.feature.swap.SwapToken
import com.otistran.flash_trade.util.Result
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

/**
 * Use case for executing token swaps from the UI.
 *
 * Flow:
 * 1. PRE-VALIDATION: Check balance + get route (parallel)
 * 2-6. Delegate to ExecuteSwapUseCase (approval, build, simulate, execute, receipt)
 */
class SwapTokenUseCase @Inject constructor(
    private val preValidationStep: PreValidationStep,
    private val executeSwapUseCase: ExecuteSwapUseCase
) {
    suspend operator fun invoke(
        tokenIn: SwapToken,
        tokenOut: SwapToken,
        amountIn: BigInteger,
        userAddress: String,
        chainId: Long,
        chainName: String,
        wallet: EmbeddedEthereumWallet,
        slippageBps: Int
    ): Result<String> {
        Timber.d("Starting swap: ${tokenIn.symbol} → ${tokenOut.symbol}, amount: $amountIn")

        // ========================================
        // STEP 1: PRE-VALIDATION
        // ========================================
        val preValidationResult = preValidationStep.execute(
            userAddress = userAddress,
            tokenInAddress = tokenIn.address,
            tokenInSymbol = tokenIn.symbol,
            tokenOutAddress = tokenOut.address,
            amountIn = amountIn,
            chainId = chainId,
            chainName = chainName
        )

        if (preValidationResult is Result.Error) {
            return preValidationResult
        }
        val routeSummary = (preValidationResult as Result.Success).data.routeSummary

        // ========================================
        // STEPS 2-6: EXECUTE VIA SHARED USE CASE
        // ========================================
        val executeResult = executeSwapUseCase(
            ExecuteSwapParams(
                tokenIn = TokenInfo(
                    address = tokenIn.address,
                    symbol = tokenIn.symbol,
                    decimals = tokenIn.decimals
                ),
                tokenOut = TokenInfo(
                    address = tokenOut.address,
                    symbol = tokenOut.symbol,
                    decimals = tokenOut.decimals
                ),
                routeSummary = routeSummary,
                amountIn = amountIn,
                userAddress = userAddress,
                wallet = wallet,
                chainId = chainId,
                chainName = chainName,
                slippageBps = slippageBps
            )
        )

        return when (executeResult) {
            is ExecuteSwapResult.Success -> Result.Success(executeResult.txHash)
            is ExecuteSwapResult.Pending -> Result.Error(
                "Transaction pending. Please wait and check your wallet. TX: ${executeResult.txHash}"
            )
            is ExecuteSwapResult.Reverted -> Result.Error("Swap failed on-chain. Transaction reverted.")
            is ExecuteSwapResult.Error -> Result.Error(executeResult.message)
        }
    }
}

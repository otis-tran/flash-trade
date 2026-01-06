package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.data.service.TransactionReceiptService
import com.otistran.flash_trade.domain.repository.Erc20Repository
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.domain.usecase.swap.step.ApprovalResult
import com.otistran.flash_trade.domain.usecase.swap.step.ApprovalStep
import com.otistran.flash_trade.domain.usecase.swap.step.PreValidationStep
import com.otistran.flash_trade.domain.usecase.swap.step.SimulationStep
import com.otistran.flash_trade.presentation.feature.swap.SwapToken
import com.otistran.flash_trade.util.Result
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import kotlinx.coroutines.delay
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

/**
 * Use case for executing token swaps with full validation framework.
 *
 * Flow:
 * 1. PRE-VALIDATION: Check balance + get route (parallel)
 * 2. APPROVAL: Native skip | EIP-2612 permit | Traditional approval
 * 3. FRESH ROUTE: Get new route after approval (if needed)
 * 4. BUILD TX: Include permit if available
 * 5. SIMULATE: eth_call before execute
 * 6. EXECUTE: Sign & broadcast, wait for receipt
 */
class SwapTokenUseCase @Inject constructor(
    private val swapRepository: SwapRepository,
    private val erc20Repository: Erc20Repository,
    private val receiptService: TransactionReceiptService,
    private val preValidationStep: PreValidationStep,
    private val approvalStep: ApprovalStep,
    private val simulationStep: SimulationStep
) {
    suspend operator fun invoke(
        tokenIn: SwapToken,
        tokenOut: SwapToken,
        amountIn: BigInteger,
        userAddress: String,
        chainId: Long,
        chainName: String,
        wallet: EmbeddedEthereumWallet
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
        var routeSummary = (preValidationResult as Result.Success).data.routeSummary
        val routerAddress = routeSummary.routerAddress

        // ========================================
        // STEP 2: APPROVAL
        // ========================================
        var permit: String? = null
        var permitDeadline: Long? = null

        if (routerAddress != null) {
            val approvalResult = approvalStep.execute(
                tokenAddress = tokenIn.address,
                tokenSymbol = tokenIn.symbol,
                userAddress = userAddress,
                spenderAddress = routerAddress,
                amount = amountIn,
                wallet = wallet,
                chainId = chainId
            )

            when (approvalResult) {
                is ApprovalResult.Error -> {
                    return Result.Error(approvalResult.message)
                }
                is ApprovalResult.Permit -> {
                    permit = approvalResult.calldata
                    permitDeadline = approvalResult.deadline
                }
                is ApprovalResult.TraditionalApprovalSent -> {
                    // Wait for approval confirmation and get fresh route
                    val confirmed = waitForApprovalConfirmation(
                        tokenAddress = tokenIn.address,
                        owner = userAddress,
                        spender = routerAddress,
                        requiredAmount = amountIn,
                        chainId = chainId
                    )
                    if (!confirmed) {
                        return Result.Error("Approval confirmation timeout. Please wait and try again.")
                    }

                    // STEP 3: Get fresh route after approval
                    Timber.d("Getting fresh route after approval")
                    val freshRouteResult = swapRepository.getRoutes(
                        chainName, tokenIn.address, tokenOut.address, amountIn
                    )
                    if (freshRouteResult is Result.Error) {
                        return Result.Error("Failed to refresh route: ${freshRouteResult.message}")
                    }
                    routeSummary = (freshRouteResult as Result.Success).data
                }
                ApprovalResult.AlreadyApproved, ApprovalResult.NotRequired -> {
                    // No action needed
                }
            }
        }

        // ========================================
        // STEP 4: BUILD ENCODED TRANSACTION
        // ========================================
        Timber.d("Building encoded transaction")

        val buildResult = swapRepository.buildEncodedRoute(
            chain = chainName,
            routeSummary = routeSummary,
            senderAddress = userAddress,
            permit = permit,
            deadline = permitDeadline
        )

        if (buildResult is Result.Error) {
            return Result.Error("Failed to build route: ${buildResult.message}")
        }
        val encodedRoute = (buildResult as Result.Success).data

        // ========================================
        // STEP 5: SIMULATE
        // ========================================
        val simulationResult = simulationStep.execute(
            userAddress = userAddress,
            encodedRoute = encodedRoute,
            chainId = chainId
        )

        if (simulationResult is Result.Error) {
            return simulationResult
        }

        // ========================================
        // STEP 6: EXECUTE
        // ========================================
        Timber.d("Executing swap")

        val chainIdHex = "0x${chainId.toString(16)}"
        val signResult = swapRepository.signTransaction(
            wallet = wallet,
            encodedRoute = encodedRoute,
            chainId = chainIdHex,
            senderAddress = userAddress
        )

        if (signResult is Result.Error) {
            return Result.Error("Failed to sign transaction: ${signResult.message}")
        }

        val txHash = (signResult as Result.Success).data
        Timber.d("Transaction broadcast: $txHash")

        // Wait for receipt
        Timber.i("Waiting for transaction receipt...")
        val receipt = receiptService.waitForReceipt(txHash, chainId)

        return when {
            receipt == null -> {
                Timber.w("Receipt timeout - transaction still pending")
                Result.Success(txHash)
            }
            receipt.status -> {
                Timber.i("Swap confirmed! Block: ${receipt.blockNumber}")
                Result.Success(txHash)
            }
            else -> {
                Timber.e("Swap reverted in block ${receipt.blockNumber}")
                Result.Error("Swap failed on-chain. Transaction reverted.")
            }
        }
    }

    /**
     * Poll for allowance confirmation after approval sent.
     */
    private suspend fun waitForApprovalConfirmation(
        tokenAddress: String,
        owner: String,
        spender: String,
        requiredAmount: BigInteger,
        chainId: Long,
        maxRetries: Int = 20,
        retryDelayMs: Long = 3000L
    ): Boolean {
        Timber.d("Waiting for approval confirmation...")

        repeat(maxRetries) { attempt ->
            delay(retryDelayMs)

            val allowanceResult = erc20Repository.getAllowance(
                tokenAddress = tokenAddress,
                owner = owner,
                spender = spender,
                chainId = chainId
            )

            if (allowanceResult is Result.Success && allowanceResult.data >= requiredAmount) {
                Timber.i("Approval confirmed! Allowance: ${allowanceResult.data}")
                return true
            }
        }

        Timber.e("Approval confirmation timeout")
        return false
    }
}

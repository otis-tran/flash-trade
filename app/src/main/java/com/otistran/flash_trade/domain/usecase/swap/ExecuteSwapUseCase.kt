package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.data.service.TransactionReceiptService
import com.otistran.flash_trade.domain.repository.Erc20Repository
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.domain.usecase.swap.step.ApprovalResult
import com.otistran.flash_trade.domain.usecase.swap.step.ApprovalStep
import com.otistran.flash_trade.domain.usecase.swap.step.SimulationStep
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.delay
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared use case for executing swap transactions.
 * Used by both UI (SwapTokenUseCase) and background (AutoSellWorker).
 *
 * Flow:
 * 1. APPROVAL: Check allowance, try permit, or send traditional approval
 * 2. BUILD TX: Include permit if available
 * 3. SIMULATE: eth_call before execute
 * 4. EXECUTE: Sign & broadcast
 * 5. RECEIPT: Wait for confirmation
 */
@Singleton
class ExecuteSwapUseCase @Inject constructor(
    private val swapRepository: SwapRepository,
    private val erc20Repository: Erc20Repository,
    private val approvalStep: ApprovalStep,
    private val simulationStep: SimulationStep,
    private val receiptService: TransactionReceiptService
) {
    suspend operator fun invoke(params: ExecuteSwapParams): ExecuteSwapResult {
        Timber.d("ExecuteSwap: ${params.tokenIn.symbol} → ${params.tokenOut.symbol}")

        var routeSummary = params.routeSummary
        val routerAddress = routeSummary.routerAddress

        // ========================================
        // STEP 1: APPROVAL
        // ========================================
        var permit: String? = null
        var permitDeadline: Long? = null

        if (routerAddress != null) {
            val approvalResult = approvalStep.execute(
                tokenAddress = params.tokenIn.address,
                tokenSymbol = params.tokenIn.symbol,
                userAddress = params.userAddress,
                spenderAddress = routerAddress,
                amount = params.amountIn,
                wallet = params.wallet,
                chainId = params.chainId
            )

            when (approvalResult) {
                is ApprovalResult.Error -> {
                    return ExecuteSwapResult.Error(approvalResult.message)
                }
                is ApprovalResult.Permit -> {
                    permit = approvalResult.calldata
                    permitDeadline = approvalResult.deadline
                }
                is ApprovalResult.TraditionalApprovalSent -> {
                    // Wait for approval confirmation and get fresh route
                    val confirmed = waitForApprovalConfirmation(
                        tokenAddress = params.tokenIn.address,
                        owner = params.userAddress,
                        spender = routerAddress,
                        requiredAmount = params.amountIn,
                        chainId = params.chainId
                    )
                    if (!confirmed) {
                        return ExecuteSwapResult.Error("Approval confirmation timeout. Please wait and try again.")
                    }

                    // Get fresh route after approval
                    Timber.d("Getting fresh route after approval")
                    val freshRouteResult = swapRepository.getRoutes(
                        params.chainName,
                        params.tokenIn.address,
                        params.tokenOut.address,
                        params.amountIn
                    )
                    if (freshRouteResult is Result.Error) {
                        return ExecuteSwapResult.Error("Failed to refresh route: ${freshRouteResult.message}")
                    }
                    routeSummary = (freshRouteResult as Result.Success).data
                }
                ApprovalResult.AlreadyApproved, ApprovalResult.NotRequired -> {
                    // No action needed
                }
            }
        }

        // ========================================
        // STEP 2: BUILD ENCODED TRANSACTION
        // ========================================
        Timber.d("Building encoded transaction")

        val buildResult = swapRepository.buildEncodedRoute(
            chain = params.chainName,
            routeSummary = routeSummary,
            senderAddress = params.userAddress,
            slippageTolerance = params.slippageBps,
            permit = permit,
            deadline = permitDeadline
        )

        if (buildResult is Result.Error) {
            return ExecuteSwapResult.Error("Failed to build route: ${buildResult.message}")
        }
        val encodedRoute = (buildResult as Result.Success).data

        // ========================================
        // STEP 3: SIMULATE
        // ========================================
        val simulationResult = simulationStep.execute(
            userAddress = params.userAddress,
            encodedRoute = encodedRoute,
            chainId = params.chainId
        )

        if (simulationResult is Result.Error) {
            return ExecuteSwapResult.Error(simulationResult.message)
        }

        // ========================================
        // STEP 4: EXECUTE
        // ========================================
        Timber.d("Executing swap")

        val chainIdHex = "0x${params.chainId.toString(16)}"
        val signResult = swapRepository.signTransaction(
            wallet = params.wallet,
            encodedRoute = encodedRoute,
            chainId = chainIdHex,
            senderAddress = params.userAddress
        )

        if (signResult is Result.Error) {
            return ExecuteSwapResult.Error("Failed to sign transaction: ${signResult.message}")
        }

        val txHash = (signResult as Result.Success).data
        Timber.d("Transaction broadcast: $txHash")

        // ========================================
        // STEP 5: WAIT FOR RECEIPT
        // ========================================
        Timber.i("Waiting for transaction receipt...")
        val receipt = receiptService.waitForReceipt(txHash, params.chainId)

        return when {
            receipt == null -> {
                Timber.w("Receipt timeout - transaction still pending")
                ExecuteSwapResult.Pending(txHash)
            }
            receipt.status -> {
                Timber.i("Swap confirmed! Block: ${receipt.blockNumber}")
                ExecuteSwapResult.Success(txHash)
            }
            else -> {
                Timber.e("Swap reverted in block ${receipt.blockNumber}")
                ExecuteSwapResult.Reverted(txHash)
            }
        }
    }

    /**
     * Poll for allowance confirmation after approval sent.
     * Uses optimized 10s timeout (10 retries × 1s delay).
     */
    private suspend fun waitForApprovalConfirmation(
        tokenAddress: String,
        owner: String,
        spender: String,
        requiredAmount: BigInteger,
        chainId: Long,
        maxRetries: Int = 10,
        retryDelayMs: Long = 1000L
    ): Boolean {
        Timber.d("Waiting for approval confirmation (maxWait=${maxRetries * retryDelayMs}ms)...")

        repeat(maxRetries) { attempt ->
            delay(retryDelayMs)

            val allowanceResult = erc20Repository.getAllowance(
                tokenAddress = tokenAddress,
                owner = owner,
                spender = spender,
                chainId = chainId
            )

            if (allowanceResult is Result.Success && allowanceResult.data >= requiredAmount) {
                Timber.i("Approval confirmed at attempt ${attempt + 1}! Allowance: ${allowanceResult.data}")
                return true
            }

            Timber.d("Attempt ${attempt + 1}/$maxRetries: allowance not yet sufficient")
        }

        Timber.e("Approval confirmation timeout after ${maxRetries * retryDelayMs}ms")
        return false
    }
}

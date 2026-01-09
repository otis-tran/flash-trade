package com.otistran.flash_trade.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.otistran.flash_trade.core.event.AppEventBus
import com.otistran.flash_trade.data.local.database.dao.PurchaseDao
import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.BalanceRepository
import com.otistran.flash_trade.domain.usecase.swap.ExecuteSwapParams
import com.otistran.flash_trade.domain.usecase.swap.ExecuteSwapResult
import com.otistran.flash_trade.domain.usecase.swap.ExecuteSwapUseCase
import com.otistran.flash_trade.domain.usecase.swap.TokenInfo
import com.otistran.flash_trade.domain.usecase.swap.step.PreValidationStep
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

/**
 * WorkManager worker for auto-selling tokens ~24h after purchase.
 *
 * Behavior:
 * - Retries indefinitely on transient failures (network, API)
 * - Stops only if purchase is CANCELLED or not found
 * - Sells at market price (any price)
 * - Uses shared ExecuteSwapUseCase for consistent swap logic
 * - Queries actual wallet balance instead of using stored amountOut
 */
@HiltWorker
class AutoSellWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val purchaseDao: PurchaseDao,
    private val balanceRepository: BalanceRepository,
    private val privyAuthService: PrivyAuthService,
    private val executeSwapUseCase: ExecuteSwapUseCase,
    private val preValidationStep: PreValidationStep,
    private val appEventBus: AppEventBus
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TX_HASH = "tx_hash"
        /** 5% slippage for auto-sell (meme tokens are volatile) */
        private const val AUTO_SELL_SLIPPAGE_BPS = 500
        /** Maximum retry attempts before marking as FAILED */
        const val MAX_RETRY_ATTEMPTS = 10
    }

    override suspend fun doWork(): Result {
        val txHash = inputData.getString(KEY_TX_HASH)
        if (txHash.isNullOrBlank()) {
            Timber.e("Missing txHash in input data")
            return Result.failure()  // Unrecoverable - no retry
        }

        // Check max retries - fail gracefully after limit
        if (runAttemptCount >= MAX_RETRY_ATTEMPTS) {
            Timber.e("Auto-sell max retries ($MAX_RETRY_ATTEMPTS) reached for $txHash")
            purchaseDao.updateStatus(txHash, PurchaseStatus.FAILED)
            return Result.failure()
        }

        Timber.i("[AutoSell] ========== Starting auto-sell for $txHash (attempt ${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS) ==========")

        return try {
            executeAutoSell(txHash)
        } catch (e: Exception) {
            Timber.e("Auto-sell failed for $txHash", e)
            // Retry on all exceptions
            Result.retry()
        }
    }

    private suspend fun executeAutoSell(txHash: String): Result {
        // Step 1: Load purchase
        val purchase = purchaseDao.getByTxHash(txHash)
        if (purchase == null) {
            Timber.w("Purchase not found: $txHash")
            return Result.failure()  // Unrecoverable
        }

        // Step 2: Check if cancelled
        if (purchase.status == PurchaseStatus.CANCELLED) {
            Timber.i("Purchase cancelled, skipping: $txHash")
            return Result.success()  // No action needed
        }

        // Step 3: Check if already sold
        if (purchase.status == PurchaseStatus.SOLD) {
            Timber.i("Purchase already sold: $txHash")
            return Result.success()
        }

        // Step 4: Mark as SELLING
        purchaseDao.updateStatus(txHash, PurchaseStatus.SELLING)

        // Step 5: Determine chain
        val chain = NetworkMode.fromChainId(purchase.chainId)

        // Step 6: Get wallet address from stored purchase data
        val walletAddress = purchase.walletAddress
        if (walletAddress.isBlank()) {
            Timber.e("No wallet address stored for purchase")
            purchaseDao.updateStatus(txHash, PurchaseStatus.HELD) // Revert
            return Result.failure()  // Unrecoverable
        }

        // Step 7: Get wallet from Privy session
        Timber.d("[AutoSell] Step 7: Getting Privy user...")
        val user = privyAuthService.getUser()
        Timber.d("[AutoSell] Privy user result: ${if (user != null) "USER EXISTS (id=${user.id})" else "NULL - SESSION NOT ACTIVE"}")
        if (user == null) {
            Timber.w("[AutoSell] ❌ USER IS NULL - Privy session not active in background! Will retry later.")
            purchaseDao.updateStatus(txHash, PurchaseStatus.HELD)
            return Result.retry() // Retry later when user opens app
        }

        Timber.d("[AutoSell] Step 8: Getting wallet from user...")
        val wallet = user.embeddedEthereumWallets.firstOrNull()
        if (wallet == null) {
            Timber.e("[AutoSell] ❌ No wallet found for user")
            purchaseDao.updateStatus(txHash, PurchaseStatus.HELD)
            return Result.failure() // Unrecoverable
        }

        // Step 8: Query ACTUAL wallet balance (instead of using stored amountOut)
        // This is critical because the actual received amount may differ from quote due to slippage
        Timber.d("[AutoSell] Step 8: Querying balance - wallet=$walletAddress, token=${purchase.tokenAddress}, decimals=${purchase.tokenDecimals}, chain=${chain.chainName}")
        val balanceResult = balanceRepository.getTokenBalance(
            walletAddress = walletAddress,
            tokenAddress = purchase.tokenAddress,
            tokenDecimals = purchase.tokenDecimals,
            network = chain
        )

        if (balanceResult is com.otistran.flash_trade.util.Result.Error) {
            Timber.w("Failed to get token balance: ${balanceResult.message}")
            purchaseDao.updateStatus(txHash, PurchaseStatus.HELD)
            return Result.retry()
        }

        val balance = (balanceResult as com.otistran.flash_trade.util.Result.Success).data
        if (balance <= BigDecimal.ZERO) {
            Timber.w("[AutoSell] ❌ Token balance is zero! Query params: wallet=$walletAddress, token=${purchase.tokenAddress}, decimals=${purchase.tokenDecimals}")
            purchaseDao.updateStatus(txHash, PurchaseStatus.HELD)
            return Result.retry() // Token may not have arrived yet
        }

        // Convert decimal balance to raw (wei) for API
        val amountToSell = balance
            .multiply(BigDecimal.TEN.pow(purchase.tokenDecimals))
            .toBigInteger()

        Timber.d("[AutoSell] Step 9: Balance query success - actual balance=$balance, amountToSell=$amountToSell")

        // Step 9: Pre-validation (unified with Swap flow)
        Timber.d("[AutoSell] Step 9: Running PreValidationStep...")
        val preValidationResult = preValidationStep.execute(
            userAddress = walletAddress,
            tokenInAddress = purchase.tokenAddress,
            tokenInSymbol = purchase.tokenSymbol,
            tokenOutAddress = purchase.stablecoinAddress,
            amountIn = amountToSell,
            chainId = chain.chainId,
            chainName = chain.chainName
        )

        if (preValidationResult is com.otistran.flash_trade.util.Result.Error) {
            Timber.w("[AutoSell] Pre-validation failed: ${preValidationResult.message}")
            purchaseDao.updateStatus(txHash, PurchaseStatus.HELD)
            return Result.retry()
        }

        val route = (preValidationResult as com.otistran.flash_trade.util.Result.Success).data.routeSummary
        Timber.d("[AutoSell] Step 10: Route obtained - output=${route.amountOut} ${purchase.stablecoinSymbol}")

        // Step 11: Execute swap via shared use case
        val executeResult = executeSwapUseCase(
            ExecuteSwapParams(
                tokenIn = TokenInfo(
                    address = purchase.tokenAddress,
                    symbol = purchase.tokenSymbol
                ),
                tokenOut = TokenInfo(
                    address = purchase.stablecoinAddress,
                    symbol = purchase.stablecoinSymbol
                ),
                routeSummary = route,
                amountIn = amountToSell,
                userAddress = walletAddress,
                wallet = wallet,
                chainId = chain.chainId,
                chainName = chain.chainName,
                slippageBps = AUTO_SELL_SLIPPAGE_BPS
            )
        )

        // Step 12: Handle result
        return when (executeResult) {
            is ExecuteSwapResult.Success -> {
                Timber.i("[AutoSell] ✅ SUCCESS! Sell tx: ${executeResult.txHash}")
                Timber.d("[AutoSell] Updating DB: txHash=$txHash -> SOLD, sellTxHash=${executeResult.txHash}")
                purchaseDao.updateSold(txHash, PurchaseStatus.SOLD, executeResult.txHash)
                Timber.d("[AutoSell] DB update complete for $txHash")
                // Trigger portfolio refresh so user sees updated balances
                appEventBus.triggerRefreshPortfolio()
                Result.success()
            }
            is ExecuteSwapResult.Pending -> {
                // TX submitted but receipt not confirmed yet - still mark as SOLD
                purchaseDao.updateSold(txHash, PurchaseStatus.SOLD, executeResult.txHash)
                Timber.i("Auto-sell submitted (pending receipt): ${executeResult.txHash}")
                // Trigger portfolio refresh so user sees updated balances
                appEventBus.triggerRefreshPortfolio()
                Result.success()
            }
            is ExecuteSwapResult.Reverted -> {
                Timber.e("[AutoSell] ❌ Transaction REVERTED - will retry with new route")
                purchaseDao.updateStatus(txHash, PurchaseStatus.RETRYING)
                Result.retry() // Retry with new route
            }
            is ExecuteSwapResult.Error -> {
                Timber.w("[AutoSell] ❌ Execution failed: ${executeResult.message}")
                purchaseDao.updateStatus(txHash, PurchaseStatus.RETRYING)
                Result.retry()
            }
        }
    }
}

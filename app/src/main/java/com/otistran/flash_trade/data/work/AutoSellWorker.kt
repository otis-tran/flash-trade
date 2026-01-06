package com.otistran.flash_trade.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.otistran.flash_trade.data.local.database.dao.PurchaseDao
import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.SwapRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.math.BigInteger

/**
 * WorkManager worker for auto-selling tokens ~24h after purchase.
 *
 * Behavior:
 * - Retries indefinitely on transient failures (network, API)
 * - Stops only if purchase is CANCELLED or not found
 * - Sells at market price (any price)
 */
@HiltWorker
class AutoSellWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val purchaseDao: PurchaseDao,
    private val swapRepository: SwapRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TX_HASH = "tx_hash"
    }

    override suspend fun doWork(): Result {
        val txHash = inputData.getString(KEY_TX_HASH)
        if (txHash.isNullOrBlank()) {
            Timber.e("Missing txHash in input data")
            return Result.failure()  // Unrecoverable - no retry
        }

        Timber.i("Starting auto-sell for $txHash (attempt ${runAttemptCount + 1})")

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

        // Step 7: Get route (token → stablecoin)
        val amountToSell = purchase.amountOut.toBigIntegerOrNull()
        if (amountToSell == null || amountToSell <= BigInteger.ZERO) {
            Timber.e("Invalid amountOut: ${purchase.amountOut}")
            purchaseDao.updateStatus(txHash, PurchaseStatus.HELD) // Revert
            return Result.failure()  // Unrecoverable
        }

        val routeResult = swapRepository.getRoutes(
            chain = chain.chainName,
            tokenIn = purchase.tokenAddress,
            tokenOut = purchase.stablecoinAddress,
            amountIn = amountToSell
        )

        if (routeResult is com.otistran.flash_trade.util.Result.Error) {
            Timber.w("Failed to get sell route: ${routeResult.message}")
            purchaseDao.updateStatus(txHash, PurchaseStatus.HELD) // Revert
            return Result.retry()
        }

        val route = (routeResult as com.otistran.flash_trade.util.Result.Success<com.otistran.flash_trade.domain.model.RouteSummaryResponse>).data
        Timber.d("Sell route: ${route.amountOut} ${purchase.stablecoinSymbol}")

        // Step 8: Build encoded route transaction
        val buildResult = swapRepository.buildEncodedRoute(
            chain = chain.chainName,
            routeSummary = route,
            senderAddress = walletAddress
        )

        if (buildResult is com.otistran.flash_trade.util.Result.Error) {
            Timber.w("Failed to build sell route: ${buildResult.message}")
            purchaseDao.updateStatus(txHash, PurchaseStatus.HELD) // Revert
            return Result.retry()
        }

        return Result.success()
    }

    private fun String.toBigIntegerOrNull(): BigInteger? {
        return try {
            BigInteger(this)
        } catch (e: Exception) {
            null
        }
    }
}

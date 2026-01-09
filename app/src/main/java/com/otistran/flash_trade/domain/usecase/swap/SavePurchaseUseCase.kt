package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import com.otistran.flash_trade.domain.model.Purchase
import com.otistran.flash_trade.domain.repository.PurchaseRepository
import com.otistran.flash_trade.domain.repository.SettingsRepository
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

/**
 * Saves a purchase record after successful swap.
 * Used for auto-sell tracking.
 */
class SavePurchaseUseCase @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Save purchase for auto-sell tracking.
     * 
     * @param txHash Transaction hash of the buy swap
     * @param tokenAddress Address of token bought
     * @param tokenSymbol Symbol of token bought
     * @param tokenName Name of token bought
     * @param tokenDecimals Decimals of token bought (for balance queries)
     * @param stablecoinAddress Address of stablecoin used to buy
     * @param stablecoinSymbol Symbol of stablecoin used
     * @param amountIn Amount of stablecoin spent (raw)
     * @param amountOut Amount of token received (raw)
     * @param chainId Chain ID
     * @param walletAddress User's wallet address
     */
    suspend operator fun invoke(
        txHash: String,
        tokenAddress: String,
        tokenSymbol: String,
        tokenName: String,
        tokenDecimals: Int,
        stablecoinAddress: String,
        stablecoinSymbol: String,
        amountIn: BigInteger,
        amountOut: BigInteger,
        chainId: Long,
        walletAddress: String
    ) {
        val durationMinutes = settingsRepository.getAutoSellDurationMinutes()
        val now = System.currentTimeMillis()
        val autoSellTime = now + (durationMinutes * 60 * 1000L)

        val purchase = Purchase(
            txHash = txHash,
            tokenAddress = tokenAddress,
            tokenSymbol = tokenSymbol,
            tokenName = tokenName,
            tokenDecimals = tokenDecimals,
            stablecoinAddress = stablecoinAddress,
            stablecoinSymbol = stablecoinSymbol,
            amountIn = amountIn.toString(),
            amountOut = amountOut.toString(),
            chainId = chainId,
            purchaseTime = now,
            autoSellTime = autoSellTime,
            status = PurchaseStatus.HELD,
            sellTxHash = null,
            workerId = null,
            walletAddress = walletAddress
        )

        purchaseRepository.insertPurchase(purchase)
        Timber.i("Saved purchase: $tokenSymbol, txHash=$txHash, autoSell in ${durationMinutes}min")
    }
}

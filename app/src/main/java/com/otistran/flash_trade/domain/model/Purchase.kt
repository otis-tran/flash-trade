package com.otistran.flash_trade.domain.model

import com.otistran.flash_trade.data.local.entity.PurchaseStatus

data class Purchase(
    val txHash: String,
    val tokenAddress: String,
    val tokenSymbol: String,
    val tokenName: String,
    val tokenDecimals: Int = 18,
    val stablecoinAddress: String,
    val stablecoinSymbol: String,
    val amountIn: String,
    val amountOut: String,
    val chainId: Long,
    val purchaseTime: Long,
    val autoSellTime: Long,
    val status: PurchaseStatus,
    val sellTxHash: String? = null,
    val workerId: String? = null,
    val walletAddress: String
) {
    /** Check if auto-sell can be cancelled (only PENDING or HELD status) */
    val canCancel: Boolean
        get() = status == PurchaseStatus.PENDING || status == PurchaseStatus.HELD

    /** Check if auto-sell time has been reached */
    val isAutoSellDue: Boolean
        get() = System.currentTimeMillis() >= autoSellTime
}

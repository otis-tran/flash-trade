package com.otistran.flash_trade.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PurchaseStatus {
    PENDING,    // Tx submitted, awaiting confirmation
    HELD,       // Confirmed, awaiting auto-sell
    SELLING,    // Auto-sell in progress
    SOLD,       // Successfully sold
    CANCELLED   // User cancelled auto-sell
}

@Entity(
    tableName = "purchases",
    indices = [
        Index("status"),
        Index("chainId"),
        Index("autoSellTime")
    ]
)
data class PurchaseEntity(
    @PrimaryKey val txHash: String,
    val tokenAddress: String,
    val tokenSymbol: String,
    val tokenName: String,
    val stablecoinAddress: String,
    val stablecoinSymbol: String,
    val amountIn: String,           // Stablecoin amount (raw)
    val amountOut: String,          // Token amount received (raw)
    val chainId: Long,
    val purchaseTime: Long,         // Unix millis
    val autoSellTime: Long,         // purchaseTime + 24h
    val status: PurchaseStatus,
    val sellTxHash: String? = null,
    val workerId: String? = null,   // WorkManager work ID
    val walletAddress: String       // User wallet for auto-sell
)

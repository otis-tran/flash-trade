package com.otistran.flash_trade.domain.model

/**
 * Domain entity representing a user's wallet.
 */
data class Wallet(
    val address: String,
    val chainId: Int,
    val balance: Double = 0.0,
    val createdAt: Long
)

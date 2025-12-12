package com.otistran.flash_trade.domain.model

/**
 * Domain entity representing a tradeable token.
 */
data class Token(
    val address: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val chainId: Int,
    val logoUrl: String? = null,
    val priceUsd: Double = 0.0
)

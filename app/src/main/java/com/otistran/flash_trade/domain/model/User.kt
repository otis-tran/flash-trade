package com.otistran.flash_trade.domain.model

/**
 * Domain entity representing the app user.
 */
data class User(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val walletAddress: String? = null,
    val isOnboarded: Boolean = false
)

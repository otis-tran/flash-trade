package com.otistran.flash_trade.domain.model

/**
 * Domain entity representing the app user.
 */
data class User(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val walletAddress: String? = null,
    val linkedAccounts: List<LinkedAccount> = emptyList()
)

data class LinkedAccount(
    val type: String,
    val address: String? = null
)

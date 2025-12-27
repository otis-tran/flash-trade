package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.domain.model.LinkedAccount
import com.otistran.flash_trade.domain.model.User
import io.privy.auth.PrivyUser

/**
 * Maps Privy SDK PrivyUser to domain User model.
 */
fun PrivyUser.toUser(): User {
    // Get first Ethereum wallet address if exists
    val ethWalletAddress = embeddedEthereumWallets.firstOrNull()?.address

    // Map linked accounts
    val accounts = linkedAccounts.map { account ->
        LinkedAccount(
            type = account::class.simpleName ?: "Unknown",
            address = when (account) {
                is io.privy.auth.LinkedAccount.EmbeddedEthereumWalletAccount -> account.address
                else -> null
            }
        )
    }

    // Extract email from linked accounts
    val email = linkedAccounts.filterIsInstance<io.privy.auth.LinkedAccount.GoogleOAuthAccount>()
        .firstOrNull()?.email

    return User(
        id = id,
        email = email,
        displayName = email?.substringBefore("@"),
        walletAddress = ethWalletAddress,
        linkedAccounts = accounts
    )
}
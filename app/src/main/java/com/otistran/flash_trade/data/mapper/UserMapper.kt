package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.domain.model.User
import io.privy.auth.LinkedAccount
import io.privy.auth.PrivyUser

/**
 * Maps Privy SDK PrivyUser to domain User model.
 */
fun PrivyUser.toUser(): User {
    // Extract email from linked accounts
    val email = linkedAccounts
        .filterIsInstance<LinkedAccount.EmailAccount>()
        .firstOrNull()
        ?.emailAddress

    // Extract Google account info
    val googleAccount = linkedAccounts
        .filterIsInstance<LinkedAccount.GoogleOAuthAccount>()
        .firstOrNull()

    // Extract display name (prefer Google name, fallback to email prefix)
    val displayName = googleAccount?.name
        ?: email?.substringBefore('@')

    // Extract avatar URL from Google account
//    val avatarUrl = googleAccount?.profilePictureUrl
    val avatarUrl = null

    // Extract wallet address (first embedded wallet)
    val walletAddress = linkedAccounts
        .filterIsInstance<LinkedAccount.EmbeddedEthereumWalletAccount>()
        .firstOrNull()
        ?.address

    return User(
        id = this.id,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        walletAddress = walletAddress,
        isOnboarded = true // Authenticated means onboarded
    )
}

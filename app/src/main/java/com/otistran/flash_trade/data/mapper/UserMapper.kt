package com.otistran.flash_trade.data.mapper
import com.otistran.flash_trade.data.remote.dto.LinkedAccountDto
import com.otistran.flash_trade.data.remote.dto.UserDto
import com.otistran.flash_trade.domain.model.LinkedAccount
import com.otistran.flash_trade.domain.model.User
import io.privy.auth.PrivyUser

/**
 * Mapper functions for User domain model.
 */

// ==================== PrivyUser -> Domain ====================

/**
 * Maps Privy SDK PrivyUser to domain User model.
 */
fun PrivyUser.toUser(): User {
    val ethWalletAddress = embeddedEthereumWallets.firstOrNull()?.address

    val accounts = linkedAccounts.map { account ->
        LinkedAccount(
            type = account::class.simpleName ?: "Unknown",
            address = when (account) {
                is io.privy.auth.LinkedAccount.EmbeddedEthereumWalletAccount -> account.address
                else -> null
            }
        )
    }

    val email = linkedAccounts
        .filterIsInstance<io.privy.auth.LinkedAccount.GoogleOAuthAccount>()
        .firstOrNull()?.email

    return User(
        id = id,
        email = email,
        displayName = email?.substringBefore("@"),
        walletAddress = ethWalletAddress,
        linkedAccounts = accounts
    )
}

/**
 * Maps PrivyUser to UserDto (for caching/serialization).
 */
fun PrivyUser.toDto(): UserDto {
    val user = this.toUser()
    return user.toDto()
}

// ==================== DTO <-> Domain ====================

/**
 * Maps UserDto to domain User.
 */
fun UserDto.toDomain(): User {
    return User(
        id = id,
        email = email,
        displayName = displayName,
        walletAddress = walletAddress,
        linkedAccounts = linkedAccounts.map { it.toDomain() }
    )
}

/**
 * Maps domain User to UserDto.
 */
fun User.toDto(): UserDto {
    return UserDto(
        id = id,
        email = email,
        displayName = displayName,
        walletAddress = walletAddress,
        linkedAccounts = linkedAccounts.map { it.toDto() }
    )
}

/**
 * Maps LinkedAccountDto to domain LinkedAccount.
 */
fun LinkedAccountDto.toDomain(): LinkedAccount {
    return LinkedAccount(
        type = type,
        address = address
    )
}

/**
 * Maps domain LinkedAccount to LinkedAccountDto.
 */
fun LinkedAccount.toDto(): LinkedAccountDto {
    return LinkedAccountDto(
        type = type,
        address = address
    )
}
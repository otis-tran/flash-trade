package com.otistran.flash_trade.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for user data from API/SDK.
 * Maps to domain User model.
 */
@Serializable
data class UserDto(
    @SerialName("id")
    val id: String,

    @SerialName("email")
    val email: String? = null,

    @SerialName("display_name")
    val displayName: String? = null,

    @SerialName("wallet_address")
    val walletAddress: String? = null,

    @SerialName("linked_accounts")
    val linkedAccounts: List<LinkedAccountDto> = emptyList(),

    @SerialName("created_at")
    val createdAt: Long? = null
)

@Serializable
data class LinkedAccountDto(
    @SerialName("type")
    val type: String,

    @SerialName("address")
    val address: String? = null,

    @SerialName("verified")
    val verified: Boolean = false
)

/**
 * DTO for wallet creation result.
 */
@Serializable
data class WalletDto(
    @SerialName("address")
    val address: String,

    @SerialName("chain")
    val chain: String,

    @SerialName("type")
    val type: String = "embedded"
)
package com.otistran.flash_trade.data.remote.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for passkey login.
 */
@Serializable
data class PasskeyLoginRequest(
    @SerialName("relying_party")
    val relyingParty: String
)

/**
 * Request body for OAuth login.
 */
@Serializable
data class OAuthLoginRequest(
    @SerialName("provider")
    val provider: String,

    @SerialName("callback_url")
    val callbackUrl: String
)

/**
 * Request body for token refresh.
 */
@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

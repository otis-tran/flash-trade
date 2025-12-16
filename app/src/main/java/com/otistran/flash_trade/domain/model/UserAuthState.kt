package com.otistran.flash_trade.domain.model

data class UserAuthState(
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val token: String? = null,
    val userEmail: String? = null,
    val displayName: String? = null,
    val walletAddress: String? = null,
    val loginTimestamp: Long = 0L
) {
    val isSessionValid: Boolean
        get() {
            if (!isLoggedIn) return false

            // Check if session is expired (7 days)
            val sessionAge = System.currentTimeMillis() - loginTimestamp
            val maxAge = 7 * 24 * 60 * 60 * 1000L // 7 days

            return sessionAge < maxAge
        }

    val isEmpty: Boolean
        get() = userId.isNullOrBlank()

    val hasWallet: Boolean
        get() = !walletAddress.isNullOrBlank()
}
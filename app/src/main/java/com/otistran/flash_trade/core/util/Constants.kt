package com.otistran.flash_trade.core.util

object Constants {
    // Auth
    const val OAUTH_SCHEME = "flashtrade"
    const val PASSKEY_RELYING_PARTY = "flashtrade.app" // Update with real domain

    // Network
    const val BASE_URL_MAINNET = "https://api.kyberswap.com/"
    const val BASE_URL_TESTNET = "https://testnet-api.kyberswap.com/"

    // Timeouts
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    // Session
    const val SESSION_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
}
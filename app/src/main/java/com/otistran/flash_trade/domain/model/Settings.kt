package com.otistran.flash_trade.domain.model

/**
 * Domain model for app settings.
 * Represents user preferences for network, theme, and other configs.
 */
data class Settings(
    val networkMode: NetworkMode = NetworkMode.TESTNET,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isAutoSellEnabled: Boolean = true
)

/**
 * Network modes for trading.
 */
enum class NetworkMode {
    TESTNET,  // Safe for testing, no real money
    MAINNET;  // Production, real money transactions

    val displayName: String
        get() = when (this) {
            TESTNET -> "Testnet"
            MAINNET -> "Mainnet"
        }

    val isProduction: Boolean get() = this == MAINNET
}

/**
 * Theme modes for app UI.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;  // Follow system settings

    val displayName: String
        get() = when (this) {
            LIGHT -> "Light"
            DARK -> "Dark"
            SYSTEM -> "System"
        }
}

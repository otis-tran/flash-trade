package com.otistran.flash_trade.domain.model

/**
 * Domain model for app settings.
 * Represents user preferences for network, theme, and other configs.
 */
data class Settings(
    val networkMode: NetworkMode = NetworkMode.LINEA,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isAutoSellEnabled: Boolean = true
)

/**
 * Supported blockchain networks.
 * Single source of truth for network configuration across the app.
 */
enum class NetworkMode(
    val chainId: Long,
    val chainName: String,      // KyberSwap API path parameter
    val displayName: String,
    val symbol: String,         // Native token symbol
    val iconColor: Long,        // For UI display
    val explorerUrl: String
) {
    ETHEREUM(
        chainId = 1L,
        chainName = "ethereum",
        displayName = "Ethereum",
        symbol = "ETH",
        iconColor = 0xFF627EEA,
        explorerUrl = "https://etherscan.io"
    ),
    LINEA(
        chainId = 59144L,
        chainName = "linea",
        displayName = "Linea",
        symbol = "ETH",
        iconColor = 0xFF61DFFF,
        explorerUrl = "https://lineascan.build"
    );

    companion object {
        val DEFAULT = LINEA

        fun fromChainId(chainId: Long): NetworkMode =
            entries.find { it.chainId == chainId } ?: DEFAULT

        fun fromChainName(name: String): NetworkMode =
            entries.find { it.chainName.equals(name, ignoreCase = true) } ?: DEFAULT

        fun fromNameSafe(name: String): NetworkMode =
            entries.find { it.name == name } ?: DEFAULT
    }
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

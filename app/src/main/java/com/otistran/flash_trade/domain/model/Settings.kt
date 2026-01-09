package com.otistran.flash_trade.domain.model

/**
 * Domain model for app settings.
 * Represents user preferences for network, theme, and other configs.
 */
data class Settings(
    val networkMode: NetworkMode = NetworkMode.ETHEREUM,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isAutoSellEnabled: Boolean = true,
    val autoSellDurationMinutes: Int = 1  // Default 1 min for demo
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
    val explorerUrl: String,
    val rpcUrl: String         // Public RPC endpoint for web3j calls
) {
    ETHEREUM(
        chainId = 1L,
        chainName = "ethereum",
        displayName = "Ethereum",
        symbol = "ETH",
        iconColor = 0xFF627EEA,
        explorerUrl = "https://etherscan.io",
        rpcUrl = "https://eth.llamarpc.com"
    );

    companion object {
        val DEFAULT = ETHEREUM

        fun fromChainId(chainId: Long): NetworkMode =
            entries.find { it.chainId == chainId } ?: DEFAULT

        fun fromChainName(name: String): NetworkMode =
            entries.find { it.chainName.equals(name, ignoreCase = true) } ?: DEFAULT

        fun fromNameSafe(name: String): NetworkMode =
            entries.find { it.name == name } ?: DEFAULT
    }
}

/**
 * Maps NetworkMode to Alchemy network identifier.
 * Used by Alchemy Data API for token balances.
 */
fun NetworkMode.toAlchemyNetwork(): String = "eth-mainnet"

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

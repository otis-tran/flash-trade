package com.otistran.flash_trade.presentation.portfolio

data class PortfolioState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val userName: String = "",
    val userEmail: String? = null,
    val walletAddress: String? = null,

    // Balance
    val totalBalanceUsd: Double = 0.0,
    val ethBalance: Double = 0.0,
    val ethPriceUsd: Double = 0.0,

    // Tokens
    val tokens: List<TokenHolding> = emptyList(),

    // Selected network
    val selectedNetwork: Network = Network.ETHEREUM,

    // Error
    val error: String? = null
) {
    val shortWalletAddress: String?
        get() = walletAddress?.let {
            if (it.length > 10) "${it.take(6)}...${it.takeLast(4)}" else it
        }

    val displayAddress: String?
        get() = walletAddress

    val displayShortAddress: String?
        get() = shortWalletAddress

    val ethBalanceUsd: Double
        get() = ethBalance * ethPriceUsd

    val formattedTotalBalance: String
        get() = "$${String.format("%,.2f", totalBalanceUsd)}"

    val formattedEthBalance: String
        get() = String.format("%.4f", ethBalance)
}

data class TokenHolding(
    val symbol: String,
    val name: String,
    val balance: Double,
    val balanceUsd: Double,
    val priceUsd: Double,
    val priceChange24h: Double = 0.0,
    val iconUrl: String? = null
) {
    val formattedBalance: String
        get() = if (balance < 0.0001) "<0.0001" else String.format("%.4f", balance)

    val formattedBalanceUsd: String
        get() = "$${String.format("%,.2f", balanceUsd)}"

    val formattedPrice: String
        get() = "$${String.format("%,.2f", priceUsd)}"

    val formattedPriceChange: String
        get() = "${if (priceChange24h >= 0) "+" else ""}${String.format("%.2f", priceChange24h)}%"

    val isPriceUp: Boolean
        get() = priceChange24h >= 0
}

enum class Network(val displayName: String, val symbol: String) {
    ETHEREUM("Ethereum", "ETH")
}


package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.NetworkMode

@Stable
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

    // Price Changes
    val priceChanges: PriceChanges = PriceChanges(),
    val selectedTimeframe: Timeframe = Timeframe.H24,

    // Tokens
    val tokens: List<TokenHolding> = emptyList(),

    // Transaction History
    val transactions: List<Transaction> = emptyList(),
    val isLoadingTransactions: Boolean = false,

    // Network - observe từ Settings (read-only)
    val currentNetwork: NetworkMode = NetworkMode.LINEA,

    // Error
    val error: String? = null
) : UiState {

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

    val hasWallet: Boolean
        get() = !walletAddress.isNullOrBlank()

    val canRefresh: Boolean
        get() = !isLoading && !isRefreshing

    val currentPriceChange: Double
        get() = when (selectedTimeframe) {
            Timeframe.M15 -> priceChanges.change15m
            Timeframe.H1 -> priceChanges.change1h
            Timeframe.H24 -> priceChanges.change24h
            Timeframe.D7 -> priceChanges.change7d
        }

    val formattedPriceChange: String
        get() {
            val change = currentPriceChange
            val prefix = if (change >= 0) "+" else ""
            return "$prefix${String.format("%.2f", change)}%"
        }

    val isPriceUp: Boolean
        get() = currentPriceChange >= 0

    val groupedTransactions: Map<String, List<Transaction>>
        get() = transactions.groupBy { it.dateGroup }
}

/**
 * Price changes for different timeframes.
 */
@Stable
data class PriceChanges(
    val change15m: Double = 0.0,
    val change1h: Double = 0.0,
    val change24h: Double = 0.0,
    val change7d: Double = 0.0
)

/**
 * Timeframe options for price change display.
 */
enum class Timeframe(val label: String) {
    M15("15m"),
    H1("1h"),
    H24("24h"),
    D7("7d")
}

/**
 * Token holding with price data.
 */
@Stable
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

/**
 * Transaction model following Etherscan V2 API structure.
 */
@Stable
data class Transaction(
    val hash: String,
    val blockNumber: String,
    val timeStamp: Long,
    val from: String,
    val to: String,
    val value: String,
    val gas: String,
    val gasPrice: String,
    val gasUsed: String,
    val isError: Boolean = false,
    val txType: TransactionType = TransactionType.TRANSFER,
    val tokenSymbol: String? = null,
    val tokenName: String? = null,
    val tokenDecimal: Int? = null,
    val contractAddress: String? = null
) {
    val shortHash: String
        get() = "${hash.take(10)}...${hash.takeLast(6)}"

    val shortFrom: String
        get() = "${from.take(6)}...${from.takeLast(4)}"

    val shortTo: String
        get() = "${to.take(6)}...${to.takeLast(4)}"

    val formattedValue: String
        get() {
            val valueInEth = value.toBigDecimalOrNull()?.divide(
                java.math.BigDecimal("1000000000000000000")
            ) ?: java.math.BigDecimal.ZERO
            return String.format("%.4f", valueInEth)
        }

    val dateGroup: String
        get() {
            val date = java.util.Date(timeStamp * 1000)
            val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
            return formatter.format(date)
        }

    val formattedTime: String
        get() {
            val date = java.util.Date(timeStamp * 1000)
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
            return formatter.format(date)
        }

    val status: TransactionStatus
        get() = if (isError) TransactionStatus.FAILED else TransactionStatus.SUCCESS
}

enum class TransactionType {
    TRANSFER,
    SWAP,
    CONTRACT_CALL,
    ERC20_TRANSFER
}

enum class TransactionStatus {
    SUCCESS,
    FAILED,
    PENDING
}
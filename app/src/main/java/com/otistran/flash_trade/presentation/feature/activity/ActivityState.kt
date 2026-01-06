package com.otistran.flash_trade.presentation.feature.activity

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.NetworkMode

/**
 * Activity screen state - displays transaction history.
 * Extracted from PortfolioScreen for dedicated Activity tab.
 */
@Stable
data class ActivityState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val walletAddress: String? = null,

    // Transaction History
    val transactions: List<Transaction> = emptyList(),
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMoreTransactions: Boolean = true,

    // Network - observe from Settings (read-only)
    val currentNetwork: NetworkMode = NetworkMode.ETHEREUM,

    // Error
    val error: String? = null
) : UiState {

    val hasWallet: Boolean
        get() = !walletAddress.isNullOrBlank()

    val canRefresh: Boolean
        get() = !isLoading && !isRefreshing

    val groupedTransactions: Map<String, List<Transaction>>
        get() = transactions.groupBy { it.dateGroup }
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

/**
 * Display name for transaction type in UI.
 */
val TransactionType.displayName: String
    get() = when (this) {
        TransactionType.TRANSFER -> "Transfer"
        TransactionType.SWAP -> "Swap"
        TransactionType.CONTRACT_CALL -> "Contract Call"
        TransactionType.ERC20_TRANSFER -> "Token Transfer"
    }

enum class TransactionStatus {
    SUCCESS,
    FAILED,
    PENDING
}

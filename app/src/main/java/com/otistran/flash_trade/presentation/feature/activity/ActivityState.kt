package com.otistran.flash_trade.presentation.feature.activity

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.NetworkMode

/**
 * Tab selection for Activity screen.
 */
enum class ActivityTab {
    ALL,
    AUTO_SELL
}

/**
 * Auto-sell history record.
 */
@Stable
data class AutoSellRecord(
    val tokenName: String,
    val tokenSymbol: String,
    val purchaseAmount: String,
    val sellAmount: String?,
    val purchaseTime: Long,
    val sellTime: Long?,
    val status: String,  // HELD, SELLING, SOLD, CANCELLED, FAILED
    val buyTxHash: String,
    val sellTxHash: String?,
    val autoSellTime: Long = 0L
) {
    val hasActiveCountdown: Boolean
        get() = status in listOf("HELD", "SELLING", "RETRYING") && autoSellTime > System.currentTimeMillis()
    
    val remainingMs: Long
        get() = (autoSellTime - System.currentTimeMillis()).coerceAtLeast(0)
    
    val canRetry: Boolean
        get() = status in listOf("HELD", "FAILED", "RETRYING")
}

/**
 * Activity screen state - displays transaction history.
 */
@Stable
data class ActivityState(
    val isLoading: Boolean = false,
    val isLoadingTransactions: Boolean = false,
    val isRefreshing: Boolean = false,
    val walletAddress: String? = null,

    // Tab selection
    val selectedTab: ActivityTab = ActivityTab.ALL,

    // Transaction History (All tab)
    val transactions: List<Transaction> = emptyList(),
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMoreTransactions: Boolean = true,

    // Auto-sell history (Auto Sell tab) - loads from local DB, very fast
    val autoSellHistory: List<AutoSellRecord> = emptyList(),

    // Network - observe from Settings (read-only)
    val currentNetwork: NetworkMode = NetworkMode.ETHEREUM,

    // Error
    val error: String? = null
) : UiState {

    val hasWallet: Boolean
        get() = !walletAddress.isNullOrBlank()

    val canRefresh: Boolean
        get() = !isLoadingTransactions && !isRefreshing

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

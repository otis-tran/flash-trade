package com.otistran.flash_trade.presentation.feature.activity

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otistran.flash_trade.core.ui.components.LoadingIndicator
import com.otistran.flash_trade.presentation.feature.activity.components.TransactionItem
import com.otistran.flash_trade.presentation.feature.activity.components.TransactionListSkeleton
import com.otistran.flash_trade.ui.theme.KyberTeal
import kotlinx.coroutines.delay

private val PriceUp = Color(0xFF00C853)
private val PriceDown = Color(0xFFFF5252)

/**
 * Activity screen - dedicated tab for transaction history.
 * Extracted from PortfolioScreen for 4-tab navigation structure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ActivityEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ActivityEffect.OpenExplorerTx -> {
                    val url = "${effect.explorerUrl}/tx/${effect.txHash}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Activity") },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(ActivityEvent.RefreshTransactions) },
                        enabled = state.canRefresh
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            // Tab Row for switching between All and Auto-Sell
            TabSelector(
                selectedTab = state.selectedTab,
                onTabSelected = { viewModel.onEvent(ActivityEvent.SelectTab(it)) }
            )

            when (state.selectedTab) {
                ActivityTab.ALL -> {
                    if (state.isLoadingTransactions) {
                        TransactionListSkeleton(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    } else {
                        TransactionsContent(state = state, onEvent = viewModel::onEvent)
                    }
                }
                ActivityTab.AUTO_SELL -> AutoSellContent(state = state, onEvent = viewModel::onEvent)
            }
        }
    }
}

@Composable
private fun TabSelector(
    selectedTab: ActivityTab,
    onTabSelected: (ActivityTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActivityTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Surface(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (tab) {
                        ActivityTab.ALL -> "All"
                        ActivityTab.AUTO_SELL -> "🤖 Auto-Sell"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsContent(
    state: ActivityState,
    onEvent: (ActivityEvent) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(ActivityEvent.RefreshTransactions) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.transactions.isEmpty()) {
                item(key = "empty_placeholder") { EmptyTransactionsPlaceholder() }
            } else {
                state.groupedTransactions.forEach { (dateGroup, transactions) ->
                    item(key = "header_$dateGroup", contentType = "date_header") {
                        Text(
                            text = dateGroup,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        items = transactions,
                        key = { it.hash },
                        contentType = { "transaction" }
                    ) { tx ->
                        TransactionItem(
                            transaction = tx,
                            walletAddress = state.walletAddress ?: "",
                            onClick = { onEvent(ActivityEvent.OpenTransactionDetails(tx.hash)) }
                        )
                    }
                }

                // Load more indicator
                if (state.hasMoreTransactions && !state.isLoadingMore) {
                    item(key = "load_more_trigger") {
                        LaunchedEffect(Unit) {
                            onEvent(ActivityEvent.LoadMoreTransactions)
                        }
                    }
                }

                if (state.isLoadingMore) {
                    item(key = "loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoSellContent(
    state: ActivityState,
    onEvent: (ActivityEvent) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(ActivityEvent.RefreshTransactions) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.autoSellHistory.isEmpty()) {
                item(key = "empty_autosell") { EmptyAutoSellPlaceholder() }
            } else {
                items(
                    items = state.autoSellHistory,
                    key = { it.buyTxHash }
                ) { record ->
                    AutoSellRecordItem(
                        record = record,
                        onRetry = { onEvent(ActivityEvent.RetryAutoSell(record.buyTxHash)) },
                        onOpenTx = { txHash -> 
                            onEvent(ActivityEvent.OpenTransactionDetails(txHash)) 
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AutoSellRecordItem(
    record: AutoSellRecord,
    onRetry: () -> Unit,
    onOpenTx: (String) -> Unit
) {
    // Real-time countdown state
    var remainingMs by remember(record.buyTxHash, record.autoSellTime) { 
        mutableStateOf((record.autoSellTime - System.currentTimeMillis()).coerceAtLeast(0)) 
    }
    
    // Update countdown every second
    LaunchedEffect(record.buyTxHash, record.autoSellTime) {
        while (remainingMs > 0) {
            delay(1000)
            remainingMs = (record.autoSellTime - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }
    
    val hasActiveCountdown = record.status in listOf("HELD", "SELLING", "RETRYING") && remainingMs > 0
    
    val statusColor = when (record.status) {
        "SOLD" -> PriceUp
        "FAILED" -> PriceDown
        "RETRYING" -> Color(0xFFFF9800)  // Orange for retrying
        "SELLING" -> KyberTeal
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Token name + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.tokenSymbol}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Show countdown if active
                    if (hasActiveCountdown) {
                        val remainingMinutes = (remainingMs / 60000).toInt()
                        val remainingSeconds = ((remainingMs % 60000) / 1000).toInt()
                        Text(
                            text = "⏱ ${remainingMinutes}:${remainingSeconds.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = KyberTeal
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = record.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Sell info (if sold)
            if (record.sellAmount != null && record.status == "SOLD") {
                val sellTimeText = record.sellTime?.let { " • ${formatRelativeTime(it)}" } ?: ""
                Text(
                    text = "Sold for: ${record.sellAmount}$sellTimeText",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceUp
                )
            }

            // Dummy indicator
            if (record.sellTxHash?.startsWith("0xDUMMY") == true) {
                Text(
                    text = "🧪 DUMMY - No real swap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View buy tx
                Surface(
                    onClick = { onOpenTx(record.buyTxHash) },
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "View Buy",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // View sell tx (if exists)
                if (record.sellTxHash != null && !record.sellTxHash.startsWith("0xDUMMY")) {
                    Surface(
                        onClick = { onOpenTx(record.sellTxHash) },
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "View Sell",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Retry button for failed
                if (record.canRetry) {
                    Surface(
                        onClick = onRetry,
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAutoSellPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🤖",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No auto-sell records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Buy tokens with auto-sell enabled to see history here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyTransactionsPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your transaction history will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/** Format timestamp to relative time (e.g., "2h ago", "5m ago") or date if older than 24h */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

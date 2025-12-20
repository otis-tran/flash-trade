package com.otistran.flash_trade.presentation.feature.portfolio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.otistran.flash_trade.core.ui.components.LoadingIndicator
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.ui.theme.KyberTeal

private val PriceUp = Color(0xFF00C853)
private val PriceDown = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    modifier: Modifier = Modifier,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PortfolioEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is PortfolioEffect.CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Wallet Address", effect.text)
                    clipboard.setPrimaryClip(clip)
                }
                is PortfolioEffect.OpenExplorerTx -> {
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
                title = { Text("Portfolio") },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(PortfolioEvent.RefreshPortfolio) },
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

            when {
                state.isLoading -> LoadingIndicator(modifier = Modifier.fillMaxSize())
                else -> PortfolioContent(state = state, onEvent = viewModel::onEvent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortfolioContent(
    state: PortfolioState,
    onEvent: (PortfolioEvent) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(PortfolioEvent.RefreshPortfolio) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { WalletAddressCard(state = state, onEvent = onEvent) }
            item { BalanceCard(state = state, onEvent = onEvent) }

            item {
                Text(
                    text = "Holdings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.tokens.isEmpty()) {
                item { EmptyTokensPlaceholder() }
            } else {
                items(items = state.tokens, key = { it.symbol }) { token ->
                    TokenHoldingItem(token = token)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (state.transactions.isEmpty()) {
                item { EmptyTransactionsPlaceholder() }
            } else {
                state.groupedTransactions.forEach { (dateGroup, transactions) ->
                    item {
                        Text(
                            text = dateGroup,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(items = transactions, key = { it.hash }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            walletAddress = state.walletAddress ?: "",
                            onClick = { onEvent(PortfolioEvent.OpenTransactionDetails(tx.hash)) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun WalletAddressCard(
    state: PortfolioState,
    onEvent: (PortfolioEvent) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Network Label (read-only, không clickable)
            NetworkLabel(network = state.currentNetwork)

            // Wallet Address
            if (state.hasWallet) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onEvent(PortfolioEvent.CopyWalletAddress) },
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.displayShortAddress ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(14.dp),
                            tint = KyberTeal
                        )
                    }
                }
            }
        }
    }
}

/**
 * Network label hiển thị network hiện tại (read-only).
 * Không có click action - user phải vào Settings để đổi network.
 */
@Composable
private fun NetworkLabel(
    network: NetworkMode,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(network.iconColor))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = network.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BalanceCard(
    state: PortfolioState,
    onEvent: (PortfolioEvent) -> Unit
) {
    val priceChangeColor by animateColorAsState(
        targetValue = if (state.isPriceUp) PriceUp else PriceDown,
        animationSpec = tween(300),
        label = "priceColor"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        gradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = state.formattedTotalBalance,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = state.formattedPriceChange,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = priceChangeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Timeframe.entries.forEach { timeframe ->
                    TimeframeChip(
                        timeframe = timeframe,
                        isSelected = state.selectedTimeframe == timeframe,
                        onClick = { onEvent(PortfolioEvent.SelectTimeframe(timeframe)) }
                    )
                    if (timeframe != Timeframe.entries.last()) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeframeChip(
    timeframe: Timeframe,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = timeframe.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = KyberTeal.copy(alpha = 0.2f),
            selectedLabelColor = KyberTeal
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = KyberTeal
        )
    )
}

@Composable
private fun TokenHoldingItem(token: TokenHolding) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TokenIcon(symbol = token.symbol, iconUrl = token.iconUrl)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = token.formattedPrice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = token.formattedPriceChange,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (token.isPriceUp) PriceUp else PriceDown
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = token.formattedBalanceUsd,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${token.formattedBalance} ${token.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TokenIcon(
    symbol: String,
    iconUrl: String?,
    modifier: Modifier = Modifier.size(44.dp)
) {
    if (iconUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(iconUrl)
                .crossfade(true)
                .build(),
            contentDescription = symbol,
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val bgColor = when (symbol) {
            "ETH" -> Color(0xFF627EEA)
            "SOL" -> Color(0xFF9945FF)
            "BTC" -> Color(0xFFF7931A)
            else -> MaterialTheme.colorScheme.primaryContainer
        }
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol.take(1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    walletAddress: String,
    onClick: () -> Unit
) {
    val isOutgoing = transaction.from.equals(walletAddress, ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (transaction.txType) {
                            TransactionType.SWAP -> KyberTeal.copy(alpha = 0.15f)
                            else -> if (isOutgoing)
                                PriceDown.copy(alpha = 0.15f)
                            else
                                PriceUp.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.txType) {
                        TransactionType.SWAP -> Icons.Default.SwapHoriz
                        else -> if (isOutgoing)
                            Icons.AutoMirrored.Filled.CallMade
                        else
                            Icons.AutoMirrored.Filled.CallReceived
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when (transaction.txType) {
                        TransactionType.SWAP -> KyberTeal
                        else -> if (isOutgoing) PriceDown else PriceUp
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (transaction.txType) {
                            TransactionType.SWAP -> "Swap"
                            TransactionType.ERC20_TRANSFER -> transaction.tokenSymbol ?: "Token"
                            else -> if (isOutgoing) "Sent" else "Received"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = when (transaction.status) {
                            TransactionStatus.SUCCESS -> Icons.Default.CheckCircle
                            TransactionStatus.FAILED -> Icons.Default.Error
                            TransactionStatus.PENDING -> Icons.Default.Refresh
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = when (transaction.status) {
                            TransactionStatus.SUCCESS -> PriceUp
                            TransactionStatus.FAILED -> PriceDown
                            TransactionStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Text(
                    text = if (isOutgoing) "To: ${transaction.shortTo}" else "From: ${transaction.shortFrom}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isOutgoing) "-" else "+"}${transaction.formattedValue} ETH",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isOutgoing) PriceDown else PriceUp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "View on Explorer",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTokensPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Token,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tokens yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your tokens will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    gradient: Brush? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Box(
            modifier = if (gradient != null) {
                Modifier.background(gradient)
            } else {
                Modifier
            }
        ) {
            content()
        }
    }
}
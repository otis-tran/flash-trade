package com.otistran.flash_trade.presentation.feature.trading

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.otistran.flash_trade.core.ui.components.ErrorView
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenDisplayFilter
import com.otistran.flash_trade.presentation.feature.trading.components.TokenFilterSheet
import com.otistran.flash_trade.ui.theme.KyberTeal

private val SafeGreen = Color(0xFF00C853)
private val RiskRed = Color(0xFFFF5252)
private val WarningOrange = Color(0xFFFFB800)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingScreen(
    viewModel: TradingViewModel = hiltViewModel(),
    onNavigateToTradeDetails: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tokens = viewModel.pagingTokens.collectAsLazyPagingItems()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TradingEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is TradingEffect.NavigateToTradeDetails -> {
                    onNavigateToTradeDetails(effect.tokenAddress)
                }
            }
        }
    }

    if (state.showFilterSheet) {
        TokenFilterSheet(
            currentFilter = state.displayFilter,
            onFilterChange = { viewModel.onEvent(TradingEvent.UpdateFilter(it)) },
            onDismiss = { viewModel.onEvent(TradingEvent.ToggleFilterSheet) }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Trading") },
                actions = {
                    if (state.displayFilter != TokenDisplayFilter.DEFAULT) {
                        Badge(containerColor = KyberTeal) {
                            Text("!", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.onEvent(TradingEvent.ToggleFilterSheet) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            if (state.displayFilter != TokenDisplayFilter.DEFAULT) {
                ActiveFiltersRow(
                    filter = state.displayFilter,
                    onClearFilter = {
                        viewModel.onEvent(TradingEvent.UpdateFilter(TokenDisplayFilter.DEFAULT))
                    }
                )
            }

            GlassSearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(TradingEvent.Search(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            PagingTokenList(
                tokens = tokens,
                onTokenClick = { viewModel.onEvent(TradingEvent.SelectToken(it)) }
            )
        }
    }
}

@Composable
private fun ActiveFiltersRow(
    filter: TokenDisplayFilter,
    onClearFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (filter.safeOnly) {
            FilterChip(
                selected = true,
                onClick = { },
                label = { Text("Safe Only") },
                leadingIcon = {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SafeGreen.copy(alpha = 0.2f),
                    selectedLabelColor = SafeGreen
                )
            )
        }

        if (filter.verifiedOnly && !filter.safeOnly) {
            FilterChip(
                selected = true,
                onClick = { },
                label = { Text("Verified") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = KyberTeal.copy(alpha = 0.2f),
                    selectedLabelColor = KyberTeal
                )
            )
        }

        if (filter.hideHoneypots && !filter.safeOnly) {
            FilterChip(
                selected = true,
                onClick = { },
                label = { Text("No Honeypots") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WarningOrange.copy(alpha = 0.2f),
                    selectedLabelColor = WarningOrange
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onClearFilter) {
            Text("Clear", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search tokens...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(KyberTeal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PagingTokenList(
    tokens: LazyPagingItems<Token>,
    onTokenClick: (Token) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = tokens.loadState.refresh is LoadState.Loading,
        onRefresh = { tokens.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            tokens.loadState.refresh is LoadState.Loading && tokens.itemCount == 0 -> {
                LoadingContent()
            }
            tokens.loadState.refresh is LoadState.Error && tokens.itemCount == 0 -> {
                val error = (tokens.loadState.refresh as LoadState.Error).error
                ErrorView(
                    message = error.localizedMessage ?: "Failed to load tokens",
                    onRetry = { tokens.retry() }
                )
            }
            tokens.loadState.refresh is LoadState.NotLoading && tokens.itemCount == 0 -> {
                EmptyContent()
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = tokens.itemCount,
                        key = tokens.itemKey { it.address }
                    ) { index ->
                        tokens[index]?.let { token ->
                            TokenCard(
                                token = token,
                                onClick = { onTokenClick(token) }
                            )
                        }
                    }

                    if (tokens.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = KyberTeal
                                )
                            }
                        }
                    }

                    if (tokens.loadState.append is LoadState.Error) {
                        item {
                            val error = (tokens.loadState.append as LoadState.Error).error
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Error: ${error.localizedMessage}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = RiskRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenCard(
    token: Token,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TokenLogo(
                    logoUrl = token.logoUrl,
                    symbol = token.displaySymbol,
                    modifier = Modifier.size(48.dp)
                )
                if (token.isVerified) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier.size(14.dp),
                            tint = KyberTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.displaySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = token.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = token.formattedTvl,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TokenStatusBadge(token = token)
            }
        }
    }
}

@Composable
private fun TokenLogo(
    logoUrl: String?,
    symbol: String,
    modifier: Modifier = Modifier
) {
    if (logoUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = symbol,
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TokenStatusBadge(token: Token) {
    val (text, bgColor, textColor, icon) = when {
        token.isHoneypot -> StatusBadgeData(
            "Risk",
            RiskRed.copy(alpha = 0.15f),
            RiskRed,
            Icons.Default.Warning
        )
        token.isSafe -> StatusBadgeData(
            "Safe",
            SafeGreen.copy(alpha = 0.15f),
            SafeGreen,
            Icons.Default.VerifiedUser
        )
        token.isVerified -> StatusBadgeData(
            "Verified",
            KyberTeal.copy(alpha = 0.15f),
            KyberTeal,
            Icons.Default.Verified
        )
        else -> StatusBadgeData(
            "${token.poolCount} pools",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            null
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

private data class StatusBadgeData(
    val text: String,
    val bgColor: Color,
    val textColor: Color,
    val icon: ImageVector?
)

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = KyberTeal)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading tokens...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tokens found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
package com.otistran.flash_trade.presentation.feature.portfolio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.otistran.flash_trade.core.ui.components.ShimmerCircle
import com.otistran.flash_trade.core.ui.components.ShimmerLine
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding
import kotlinx.coroutines.delay

private val PriceUp = Color(0xFF00C853)
private val PriceDown = Color(0xFFFF5252)

/**
 * Token holding item with logo, name, balance, and price change.
 */
@Composable
fun TokenHoldingItem(
    token: TokenHolding,
    network: NetworkMode
) {
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
            // Left: Token logo with network badge
            Box {
                TokenIcon(symbol = token.symbol, iconUrl = token.iconUrl)
                // Network badge at bottom-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(network.iconColor))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center: Token name + Amount with symbol
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Auto-sell countdown display
                if (token.hasAutoSell) {
                    AutoSellCountdown(remainingMs = token.autoSellRemainingMs)
                }
                
                Text(
                    text = "${token.formattedBalance} ${token.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right: USD value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = token.formattedValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = token.formattedPrice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TokenIcon(
    symbol: String?,
    iconUrl: String?,
    modifier: Modifier = Modifier.size(44.dp)
) {
    if (iconUrl != null) {
        // Memoize context and ImageRequest to prevent recreation on every recomposition
        val context = LocalContext.current
        val imageRequest = androidx.compose.runtime.remember(iconUrl) {
            ImageRequest.Builder(context)
                .data(iconUrl)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = imageRequest,
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
                text = symbol?.take(1) ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Shimmer placeholder for token holding item during loading.
 */
@Composable
fun TokenHoldingItemShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token icon shimmer
            ShimmerCircle(size = 44.dp)

            Spacer(modifier = Modifier.width(12.dp))

            // Token name + amount shimmer
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLine(width = 80.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerLine(width = 60.dp, height = 12.dp)
            }

            // USD value shimmer
            Column(horizontalAlignment = Alignment.End) {
                ShimmerLine(width = 70.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerLine(width = 50.dp, height = 12.dp)
            }
        }
    }
}

/**
 * Auto-sell countdown timer that updates every second.
 */
@Composable
private fun AutoSellCountdown(remainingMs: Long) {
    var remaining by remember { mutableLongStateOf(remainingMs) }

    LaunchedEffect(remainingMs) {
        remaining = remainingMs
        while (remaining > 0) {
            delay(1000)
            remaining -= 1000
        }
    }

    val minutes = (remaining / 60000).toInt()
    val seconds = ((remaining % 60000) / 1000).toInt()
    val text = if (minutes > 0) {
        "Auto-sell in ${minutes}m ${seconds}s"
    } else if (remaining > 0) {
        "Auto-sell in ${seconds}s"
    } else {
        "Auto-sell processing..."
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

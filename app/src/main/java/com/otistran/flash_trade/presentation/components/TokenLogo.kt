package com.otistran.flash_trade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Shared token logo component with fallback to symbol initial.
 * Supports predefined colors for common tokens (ETH, USDC, USDT, SOL, BTC).
 */
@Composable
fun TokenLogo(
    symbol: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    if (logoUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = symbol,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val bgColor = getTokenColor(symbol)
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol.take(1),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Get predefined color for common tokens, fallback to primaryContainer.
 */
@Composable
private fun getTokenColor(symbol: String): Color {
    return when (symbol) {
        "ETH" -> Color(0xFF627EEA)
        "USDC" -> Color(0xFF2775CA)
        "USDT" -> Color(0xFF26A17B)
        "SOL" -> Color(0xFF9945FF)
        "BTC" -> Color(0xFFF7931A)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
}

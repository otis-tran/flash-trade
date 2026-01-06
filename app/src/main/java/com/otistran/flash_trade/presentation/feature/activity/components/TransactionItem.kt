package com.otistran.flash_trade.presentation.feature.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otistran.flash_trade.presentation.feature.activity.Transaction
import com.otistran.flash_trade.presentation.feature.activity.TransactionStatus
import com.otistran.flash_trade.presentation.feature.activity.TransactionType
import com.otistran.flash_trade.ui.theme.KyberTeal

private val PriceUp = Color(0xFF00C853)
private val PriceDown = Color(0xFFFF5252)

/**
 * Single transaction item for the Activity screen.
 */
@Composable
fun TransactionItem(
    transaction: Transaction,
    walletAddress: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutgoing = transaction.from.equals(walletAddress, ignoreCase = true)

    Card(
        modifier = modifier
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
            TransactionTypeIcon(
                txType = transaction.txType,
                isOutgoing = isOutgoing
            )

            Spacer(modifier = Modifier.width(12.dp))

            TransactionInfo(
                transaction = transaction,
                isOutgoing = isOutgoing,
                modifier = Modifier.weight(1f)
            )

            TransactionAmount(
                transaction = transaction,
                isOutgoing = isOutgoing
            )
        }
    }
}

@Composable
private fun TransactionTypeIcon(
    txType: TransactionType,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when (txType) {
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
            imageVector = when (txType) {
                TransactionType.SWAP -> Icons.Default.SwapHoriz
                else -> if (isOutgoing)
                    Icons.AutoMirrored.Filled.CallMade
                else
                    Icons.AutoMirrored.Filled.CallReceived
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = when (txType) {
                TransactionType.SWAP -> KyberTeal
                else -> if (isOutgoing) PriceDown else PriceUp
            }
        )
    }
}

@Composable
private fun TransactionInfo(
    transaction: Transaction,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
}

@Composable
private fun TransactionAmount(
    transaction: Transaction,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
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

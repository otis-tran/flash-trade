package com.otistran.flash_trade.presentation.feature.trading.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otistran.flash_trade.domain.model.TokenDisplayFilter
import com.otistran.flash_trade.ui.theme.KyberTeal

private val SafeGreen = Color(0xFF00C853)
private val RiskRed = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenFilterSheet(
    currentFilter: TokenDisplayFilter,
    onFilterChange: (TokenDisplayFilter) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Filter Tokens",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            FilterSwitch(
                title = "Safe Tokens Only",
                subtitle = "Verified, whitelisted, no honeypot",
                checked = currentFilter.safeOnly,
                onCheckedChange = {
                    onFilterChange(currentFilter.copy(safeOnly = it))
                },
                icon = Icons.Default.VerifiedUser,
                iconTint = SafeGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilterSwitch(
                title = "Verified Only",
                subtitle = "Show only verified tokens",
                checked = currentFilter.verifiedOnly,
                onCheckedChange = {
                    onFilterChange(currentFilter.copy(verifiedOnly = it))
                },
                icon = Icons.Default.Verified,
                iconTint = KyberTeal,
                enabled = !currentFilter.safeOnly
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilterSwitch(
                title = "Hide Honeypots",
                subtitle = "Exclude potential scam tokens",
                checked = currentFilter.hideHoneypots,
                onCheckedChange = {
                    onFilterChange(currentFilter.copy(hideHoneypots = it))
                },
                icon = Icons.Default.Warning,
                iconTint = RiskRed,
                enabled = !currentFilter.safeOnly
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { onFilterChange(TokenDisplayFilter.DEFAULT) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Filters")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FilterSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = KyberTeal,
                checkedTrackColor = KyberTeal.copy(alpha = 0.5f)
            )
        )
    }
}
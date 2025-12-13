package com.otistran.flash_trade.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.otistran.flash_trade.domain.model.NetworkMode

/**
 * Network mode toggle section.
 * Shows mainnet/testnet switch with indicator chip.
 */
@Composable
fun NetworkModeSection(
    networkMode: NetworkMode,
    onNetworkModeChange: (NetworkMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Network Mode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (networkMode.isProduction) {
                            "Real transactions on mainnet"
                        } else {
                            "Safe testing environment"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = networkMode == NetworkMode.MAINNET,
                    onCheckedChange = { isMainnet ->
                        val newMode = if (isMainnet) {
                            NetworkMode.MAINNET
                        } else {
                            NetworkMode.TESTNET
                        }
                        onNetworkModeChange(newMode)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            // Network indicator chip
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = networkMode.displayName.uppercase(),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (networkMode.isProduction) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    labelColor = if (networkMode.isProduction) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            )
        }
    }
}

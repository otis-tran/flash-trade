package com.otistran.flash_trade.presentation.feature.portfolio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.presentation.feature.portfolio.AssetTab
import com.otistran.flash_trade.presentation.feature.portfolio.PortfolioEvent
import com.otistran.flash_trade.presentation.feature.portfolio.PortfolioState
import com.otistran.flash_trade.ui.theme.KyberTeal

/**
 * Asset tabs section with Token/NFT tabs and network filter.
 */
@Composable
fun AssetTabsSection(
    state: PortfolioState,
    onEvent: (PortfolioEvent) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token / NFT Tabs
            TabRow(
                selectedTabIndex = state.selectedAssetTab.ordinal,
                modifier = Modifier.width(160.dp),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedAssetTab.ordinal]),
                        color = KyberTeal
                    )
                }
            ) {
                AssetTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedAssetTab == tab,
                        onClick = { onEvent(PortfolioEvent.SelectAssetTab(tab)) },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (state.selectedAssetTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Network filter chip
            NetworkFilterChip(network = state.currentNetwork)
        }
    }
}

@Composable
private fun NetworkFilterChip(
    network: NetworkMode
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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

package com.otistran.flash_trade.presentation.feature.portfolio.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otistran.flash_trade.core.ui.components.ShimmerBox
import com.otistran.flash_trade.presentation.feature.portfolio.PortfolioState

@Composable
fun BalanceSection(
    state: PortfolioState,
    isLoading: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Primary Balance - shimmer or value
        if (isLoading) {
            ShimmerBox(
                width = 180.dp,
                height = 40.dp,
                cornerRadius = 8.dp
            )
        } else {
            Text(
                text = state.formattedTotalBalance,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

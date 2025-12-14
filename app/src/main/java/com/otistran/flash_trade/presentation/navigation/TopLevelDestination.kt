package com.otistran.flash_trade.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations for bottom navigation.
 * Each entry represents a tab in the bottom navigation bar.
 */
enum class TopLevelDestination(
    val label: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val route: Any
) {
    TRADING(
        label = "Trading",
        iconOutlined = Icons.AutoMirrored.Outlined.TrendingUp,
        iconFilled = Icons.AutoMirrored.Filled.TrendingUp,
        route = TradingGraph
    ),
    PORTFOLIO(
        label = "Portfolio",
        iconOutlined = Icons.Outlined.AccountBalanceWallet,
        iconFilled = Icons.Filled.AccountBalanceWallet,
        route = PortfolioGraph
    ),
    SETTINGS(
        label = "Settings",
        iconOutlined = Icons.Outlined.Settings,
        iconFilled = Icons.Filled.Settings,
        route = SettingsGraph
    )
}

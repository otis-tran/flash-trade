package com.otistran.flash_trade.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations for bottom navigation.
 * 4-tab structure: Home (Portfolio), Swap, Activity, Settings
 */
enum class TopLevelDestination(
    val label: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val route: Any
) {
    HOME(
        label = "Home",
        iconOutlined = Icons.Outlined.AccountBalanceWallet,
        iconFilled = Icons.Filled.AccountBalanceWallet,
        route = HomeGraph
    ),
    SWAP(
        label = "Swap",
        iconOutlined = Icons.Outlined.SwapHoriz,
        iconFilled = Icons.Filled.SwapHoriz,
        route = SwapGraph
    ),
    ACTIVITY(
        label = "Activity",
        iconOutlined = Icons.Outlined.History,
        iconFilled = Icons.Filled.History,
        route = ActivityGraph
    ),
    SETTINGS(
        label = "Settings",
        iconOutlined = Icons.Outlined.Settings,
        iconFilled = Icons.Filled.Settings,
        route = SettingsGraph
    )
}

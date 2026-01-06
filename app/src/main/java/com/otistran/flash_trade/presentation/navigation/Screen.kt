package com.otistran.flash_trade.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Flash Trade app.
 * Uses Kotlin Serialization for Navigation Compose 2.8+ type safety.
 *
 * Navigation structure:
 * - 4-tab bottom nav: Home, Swap, Activity, Settings
 * - Each tab has its own graph with nested screens
 */

// =============================================================================
// Auth Flow (no bottom nav)
// =============================================================================
@Serializable
object Welcome

@Serializable
object Login

// =============================================================================
// Top-level Navigation Graphs (bottom nav tabs)
// =============================================================================
@Serializable
object HomeGraph

@Serializable
object SwapGraph

@Serializable
object ActivityGraph

@Serializable
object SettingsGraph

// =============================================================================
// Main Screens (nested in graphs)
// =============================================================================
@Serializable
object HomeScreen  // Previously PortfolioScreen - now Home tab

@Serializable
object SwapMainScreen  // Main swap screen in Swap tab

@Serializable
object ActivityScreen  // Transaction history in Activity tab

@Serializable
object SettingsScreen

// =============================================================================
// Detail Screens (nested, no bottom nav)
// =============================================================================
@Serializable
data class SwapScreen(val tokenAddress: String)  // Swap with specific token

@Serializable
data class TradeDetails(val tradeId: String)

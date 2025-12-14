package com.otistran.flash_trade.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Flash Trade app.
 * Uses Kotlin Serialization for Navigation Compose 2.8+ type safety.
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
object TradingGraph

@Serializable
object PortfolioGraph

@Serializable
object SettingsGraph

// =============================================================================
// Main Screens (nested in graphs)
// =============================================================================
@Serializable
object TradingScreen

@Serializable
object PortfolioScreen

@Serializable
object SettingsScreen

// =============================================================================
// Detail Screens (nested, no bottom nav)
// =============================================================================
@Serializable
data class TradeDetails(val tradeId: String)

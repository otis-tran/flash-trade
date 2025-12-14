package com.otistran.flash_trade.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Centralized app state holder for navigation management.
 * Uses @Stable to prevent unnecessary recompositions.
 */
@Stable
class AppState(val navController: NavHostController) {

    private val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    /**
     * Current route as type-safe navigation object.
     * Maps route string back to sealed class instance.
     */
    val currentRoute: Any?
        @Composable get() = currentDestination?.route?.let { route ->
            when {
                route.contains("TradingGraph") -> TradingGraph
                route.contains("TradingScreen") -> TradingScreen
                route.contains("PortfolioGraph") -> PortfolioGraph
                route.contains("PortfolioScreen") -> PortfolioScreen
                route.contains("SettingsGraph") -> SettingsGraph
                route.contains("SettingsScreen") -> SettingsScreen
                route.contains("Welcome") -> Welcome
                route.contains("Login") -> Login
                route.contains("TradeDetails") -> TradeDetails::class
                else -> null
            }
        }

    /**
     * Determines if bottom navigation bar should be visible.
     * Show on: Trading, Portfolio, Settings (graphs and screens)
     * Hide on: Welcome, Login, TradeDetails
     */
    val shouldShowBottomBar: Boolean
        @Composable get() {
            val route = currentRoute
            return route is TradingGraph ||
                    route is TradingScreen ||
                    route is PortfolioGraph ||
                    route is PortfolioScreen ||
                    route is SettingsGraph ||
                    route is SettingsScreen
        }

    /**
     * Current top-level destination for bottom nav selection state.
     */
    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() = when (currentRoute) {
            is TradingGraph, is TradingScreen -> TopLevelDestination.TRADING
            is PortfolioGraph, is PortfolioScreen -> TopLevelDestination.PORTFOLIO
            is SettingsGraph, is SettingsScreen -> TopLevelDestination.SETTINGS
            else -> null
        }

    /**
     * All available top-level destinations for bottom nav.
     */
    val topLevelDestinations = TopLevelDestination.entries

    /**
     * Navigate to a top-level destination with proper back stack handling.
     * - Pops up to start destination (saves state)
     * - Single top (prevents duplicate entries)
     * - Restores state (maintains tab state)
     */
    fun navigateToTopLevelDestination(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

/**
 * Remember AppState across recompositions, keyed by navController.
 */
@Composable
fun rememberAppState(
    navController: NavHostController
): AppState = remember(navController) {
    AppState(navController)
}

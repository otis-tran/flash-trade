package com.otistran.flash_trade.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Centralized app state holder for navigation management.
 */
@Stable
class AppState(val navController: NavHostController) {

    private val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    /**
     * Determines if bottom navigation bar should be visible.
     * Show on main tab screens, hide on auth and detail screens.
     */
    val shouldShowBottomBar: Boolean
        @Composable get() {
            val destination = currentDestination ?: return false
            return TOP_LEVEL_ROUTES.any { destination.hasRoute(it::class) }
        }

    /**
     * Current top-level destination for bottom nav selection state.
     */
    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() {
            val destination = currentDestination ?: return null
            return TopLevelDestination.entries.find { topLevel ->
                ROUTE_TO_DESTINATION[topLevel]?.any { route ->
                    destination.hasRoute(route::class)
                } == true
            }
        }

    /**
     * All available top-level destinations for bottom nav.
     */
    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries

    /**
     * Navigate to a top-level destination with proper back stack handling.
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

    companion object {
        /** Routes that show bottom bar */
        private val TOP_LEVEL_ROUTES = listOf(
            TradingGraph, TradingScreen,
            PortfolioGraph, PortfolioScreen,
            SettingsGraph, SettingsScreen
        )

        /** Map destinations to their routes */
        private val ROUTE_TO_DESTINATION = mapOf(
            TopLevelDestination.TRADING to listOf(TradingGraph, TradingScreen),
            TopLevelDestination.PORTFOLIO to listOf(PortfolioGraph, PortfolioScreen),
            TopLevelDestination.SETTINGS to listOf(SettingsGraph, SettingsScreen)
        )
    }
}

/**
 * Remember AppState across recompositions.
 */
@Composable
fun rememberAppState(
    navController: NavHostController
): AppState = remember(navController) {
    AppState(navController)
}
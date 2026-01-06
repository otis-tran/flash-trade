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
     * Show on main tab screens (except Swap), hide on auth, detail, and swap screens.
     */
    val shouldShowBottomBar: Boolean
        @Composable get() {
            val destination = currentDestination ?: return false
            // Hide bottom bar on swap screen to maximize space
            if (destination.hasRoute(SwapMainScreen::class)) return false
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
        /** Routes that show bottom bar (excludes SwapMainScreen for fullscreen swap) */
        private val TOP_LEVEL_ROUTES = listOf(
            HomeGraph, HomeScreen,
            SwapGraph, // SwapMainScreen excluded for full-screen experience
            ActivityGraph, ActivityScreen,
            SettingsGraph, SettingsScreen
        )

        /** Map destinations to their routes */
        private val ROUTE_TO_DESTINATION = mapOf(
            TopLevelDestination.HOME to listOf(HomeGraph, HomeScreen),
            TopLevelDestination.SWAP to listOf(SwapGraph, SwapMainScreen),
            TopLevelDestination.ACTIVITY to listOf(ActivityGraph, ActivityScreen),
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
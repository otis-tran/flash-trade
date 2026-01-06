package com.otistran.flash_trade.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.otistran.flash_trade.presentation.feature.activity.ActivityScreen
import com.otistran.flash_trade.presentation.feature.auth.LoginScreen
import com.otistran.flash_trade.presentation.feature.portfolio.PortfolioScreen
import com.otistran.flash_trade.presentation.feature.swap.SwapScreen
import com.otistran.flash_trade.presentation.feature.settings.SettingsScreen
import com.otistran.flash_trade.presentation.navigation.ActivityScreen as ActivityScreenRoute

/**
 * Main navigation graph for Flash Trade app.
 * 4-tab structure: Home, Swap, Activity, Settings
 * Start destination is determined by MainActivity during splash.
 */
@Composable
fun FlashTradeNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = Login,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ==================== Auth Flow ====================
        composable<Welcome> {
            // Welcome screen not implemented - redirect to Login
            androidx.compose.runtime.LaunchedEffect(Unit) {
                navController.navigate(Login) {
                    popUpTo<Welcome> { inclusive = true }
                }
            }
        }

        composable<Login> {
            LoginScreen(
                onNavigateToTrading = { navController.navigateToHome() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ==================== Home Tab (Portfolio) ====================
        navigation<HomeGraph>(startDestination = HomeScreen) {
            composable<HomeScreen> {
                PortfolioScreen(
                    onNavigateToSwap = { navController.navigateToSwap() }
                )
            }
        }

        // ==================== Swap Tab ====================
        navigation<SwapGraph>(startDestination = SwapMainScreen) {
            composable<SwapMainScreen> {
                // Swap screen with built-in token selector
                SwapScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHome = { navController.navigateToHomeTab() }
                )
            }
        }

        // ==================== Activity Tab ====================
        navigation<ActivityGraph>(startDestination = ActivityScreenRoute) {
            composable<ActivityScreenRoute> {
                ActivityScreen()
            }
        }

        // ==================== Settings Tab ====================
        navigation<SettingsGraph>(startDestination = SettingsScreen) {
            composable<SettingsScreen> {
                SettingsScreen(
                    onNavigateToLogin = { navController.navigateToLogin() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ==================== Navigation Extensions ====================

/**
 * Navigate to Home after login (clears login from backstack)
 */
private fun NavHostController.navigateToHome() {
    navigate(HomeGraph) {
        popUpTo<Login> { inclusive = true }
    }
}

/**
 * Navigate to Home tab (for in-app navigation)
 */
private fun NavHostController.navigateToHomeTab() {
    navigate(HomeGraph) {
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToLogin() {
    navigate(Login) {
        popUpTo(0) { inclusive = true }
    }
}

private fun NavHostController.navigateToSwap() {
    navigate(SwapGraph) {
        launchSingleTop = true
        restoreState = true
    }
}
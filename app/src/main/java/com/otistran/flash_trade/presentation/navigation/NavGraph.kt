package com.otistran.flash_trade.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.otistran.flash_trade.presentation.auth.LoginScreen
import com.otistran.flash_trade.presentation.settings.SettingsScreen

/**
 * Main navigation graph for Flash Trade app.
 * Uses type-safe navigation with nested graphs for bottom nav tabs.
 */
@Composable
fun FlashTradeNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = SettingsGraph
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // =================================================================
        // Auth Flow (no bottom nav)
        // =================================================================
        composable<Welcome> {
            // TODO: WelcomeScreen()
        }

        composable<Login> {
            LoginScreen(
                onNavigateToTrading = {
                    navController.navigate(TradingGraph) {
                        popUpTo<Login> { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // =================================================================
        // Trading Tab (nested graph)
        // =================================================================
        navigation<TradingGraph>(startDestination = TradingScreen) {
            composable<TradingScreen> {
                // TODO: TradingScreen()
            }

            composable<TradeDetails> { backStackEntry ->
                val args = backStackEntry.toRoute<TradeDetails>()
                // TODO: TradeDetailsScreen(tradeId = args.tradeId)
            }
        }

        // =================================================================
        // Portfolio Tab (nested graph)
        // =================================================================
        navigation<PortfolioGraph>(startDestination = PortfolioScreen) {
            composable<PortfolioScreen> {
                // TODO: PortfolioScreen()
            }
        }

        // =================================================================
        // Settings Tab (nested graph)
        // =================================================================
        navigation<SettingsGraph>(startDestination = SettingsScreen) {
            composable<SettingsScreen> {
                SettingsScreen(
                    onNavigateToLogin = {
                        navController.navigate(Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Navigation actions for type-safe navigation.
 */
class FlashTradeNavigationActions(private val navController: NavHostController) {
    fun navigateToLogin() = navController.navigate(Login)

    fun navigateToTrading() = navController.navigate(TradingGraph) {
        popUpTo<Login> { inclusive = true }
    }

    /**
     * Navigate to top-level destination with proper back stack management.
     * Uses saveState + restoreState for tab state preservation.
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

    fun navigateToTradeDetails(tradeId: String) =
        navController.navigate(TradeDetails(tradeId))

    fun navigateBack() = navController.popBackStack()
}

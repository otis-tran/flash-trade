package com.otistran.flash_trade.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.otistran.flash_trade.presentation.auth.LoginScreen
import com.otistran.flash_trade.presentation.settings.SettingsScreen

@Composable
fun FlashTradeNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.START.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding
        composable(Screen.Welcome.route) {
            // TODO: WelcomeScreen()
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToTrading = {
                    navController.navigate(Screen.Trading.route) {
                        // Clear back stack - can't go back to login after auth
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Main
        composable(Screen.Trading.route) {
            // TODO: TradingScreen()
        }

        composable(Screen.Portfolio.route) {
            // TODO: PortfolioScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Details
        composable(
            route = Screen.TradeDetails().route,
            arguments = listOf(
                navArgument(Screen.TradeDetails.TRADE_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val tradeId =
                backStackEntry.arguments?.getString(Screen.TradeDetails.TRADE_ID_ARG) ?: ""
            // TODO: TradeDetailsScreen(tradeId)
        }
    }
}

/**
 * Navigation actions for the app.
 */
class FlashTradeNavigationActions(private val navController: NavHostController) {
    fun navigateToLogin() = navController.navigate(Screen.Login.route)
    fun navigateToTrading() = navController.navigate(Screen.Trading.route) {
        popUpTo(Screen.Welcome.route) { inclusive = true }
    }

    fun navigateToPortfolio() = navController.navigate(Screen.Portfolio.route)
    fun navigateToSettings() = navController.navigate(Screen.Settings.route)
    fun navigateToTradeDetails(tradeId: String) =
        navController.navigate(Screen.TradeDetails.createRoute(tradeId))

    fun navigateBack() = navController.popBackStack()
}

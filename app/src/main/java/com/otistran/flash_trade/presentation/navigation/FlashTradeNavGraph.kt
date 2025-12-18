package com.otistran.flash_trade.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.otistran.flash_trade.presentation.feature.auth.LoginScreen
import com.otistran.flash_trade.presentation.feature.portfolio.PortfolioScreen
import com.otistran.flash_trade.presentation.feature.settings.SettingsScreen

/**
 * Main navigation graph for Flash Trade app.
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
            // TODO: WelcomeScreen()
        }

        composable<Login> {
            LoginScreen(
                onNavigateToTrading = { navController.navigateToHome() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ==================== Trading Tab ====================
        navigation<TradingGraph>(startDestination = TradingScreen) {
            composable<TradingScreen> {
                // TODO: TradingScreen()
            }

            composable<TradeDetails> { backStackEntry ->
                val args = backStackEntry.toRoute<TradeDetails>()
                // TODO: TradeDetailsScreen(tradeId = args.tradeId)
            }
        }

        // ==================== Portfolio Tab ====================
        navigation<PortfolioGraph>(startDestination = PortfolioScreen) {
            composable<PortfolioScreen> {
                PortfolioScreen()
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

private fun NavHostController.navigateToHome() {
    navigate(TradingGraph) {
        popUpTo<Login> { inclusive = true }
    }
}

private fun NavHostController.navigateToLogin() {
    navigate(Login) {
        popUpTo(0) { inclusive = true }
    }
}
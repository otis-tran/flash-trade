package com.otistran.flash_trade.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.otistran.flash_trade.presentation.feature.auth.LoginScreen
import com.otistran.flash_trade.presentation.feature.auth.LoginViewModel
import com.otistran.flash_trade.presentation.feature.portfolio.PortfolioScreen
import com.otistran.flash_trade.presentation.feature.settings.SettingsScreen

/**
 * Main navigation graph for Flash Trade app.
 */
@Composable
fun FlashTradeNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val loginViewModel: LoginViewModel = hiltViewModel()

    var startDestination by remember { mutableStateOf<Any?>(null) }
    var isChecking by remember { mutableStateOf(true) }

    // Check session on launch
    LaunchedEffect(Unit) {
        loginViewModel.state.collect { state ->
            if (!state.isCheckingSession) {
                startDestination = if (state.isAuthenticated) TradingGraph else Login
                isChecking = false
            }
        }
    }

    if (isChecking) {
        LoadingScreen()
    } else {
        startDestination?.let { destination ->
            NavHost(
                navController = navController,
                startDestination = destination,
                modifier = modifier
            ) {
                // ==================== Auth Flow ====================
                composable<Welcome> {
                    // TODO: WelcomeScreen()
                }

                composable<Login> {
                    LoginScreen(
                        onNavigateToTrading = {
                            navController.navigateToHome()
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
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
                            onNavigateToLogin = {
                                navController.navigateToLogin()
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ==================== Navigation Extensions ====================

/**
 * Navigate to home (Trading) after login, clearing auth backstack.
 */
private fun NavHostController.navigateToHome() {
    navigate(TradingGraph) {
        popUpTo<Login> { inclusive = true }
    }
}

/**
 * Navigate to login after logout, clearing entire backstack.
 */
private fun NavHostController.navigateToLogin() {
    navigate(Login) {
        popUpTo(0) { inclusive = true }
    }
}
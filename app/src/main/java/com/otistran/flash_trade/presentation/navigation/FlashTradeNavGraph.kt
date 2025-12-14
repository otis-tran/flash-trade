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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.otistran.flash_trade.domain.usecase.CheckLoginStatusUseCase
import com.otistran.flash_trade.presentation.auth.AuthCheckViewModel
import com.otistran.flash_trade.presentation.auth.LoginScreen
import com.otistran.flash_trade.presentation.settings.SettingsScreen
import com.otistran.flash_trade.util.Result

/**
 * Main navigation graph for Flash Trade app.
 * Uses type-safe navigation with nested graphs for bottom nav tabs.
 */
@Composable
fun FlashTradeNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    checkLoginStatusUseCase: CheckLoginStatusUseCase = hiltViewModel<AuthCheckViewModel>().checkLoginStatusUseCase
) {
    var startDestination by remember { mutableStateOf<Any?>(null) }
    var isChecking by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        when (val result = checkLoginStatusUseCase()) {
            is Result.Success -> {
                val authState = result.data
                startDestination = if (authState.isLoggedIn && authState.isSessionValid) {
                    TradingGraph
                } else {
                    Login
                }
                isChecking = false
            }

            is Result.Error -> {
                startDestination = Login
                isChecking = false
            }

            Result.Loading -> {
                // Continue checking
            }
        }
    }
    if (isChecking) {
        // Show loading or splash
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        startDestination?.let { destination ->
            NavHost(
                navController = navController,
                startDestination = destination,
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

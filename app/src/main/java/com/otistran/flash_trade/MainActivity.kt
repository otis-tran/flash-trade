package com.otistran.flash_trade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.otistran.flash_trade.core.datastore.UserPreferences
import com.otistran.flash_trade.di.PrivyProvider
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.domain.manager.PrefetchManager
import com.otistran.flash_trade.domain.usecase.auth.CheckLoginStatusUseCase
import com.otistran.flash_trade.presentation.navigation.BottomNavBar
import com.otistran.flash_trade.presentation.navigation.FlashTradeNavGraph
import com.otistran.flash_trade.presentation.navigation.Login
import com.otistran.flash_trade.presentation.navigation.TradingGraph
import com.otistran.flash_trade.presentation.navigation.rememberAppState
import com.otistran.flash_trade.ui.theme.FlashTradeTheme
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var checkLoginStatusUseCase: CheckLoginStatusUseCase

    @Inject
    lateinit var prefetchManager: PrefetchManager

    /** App ready state - splash stays until true */
    private var isAppReady by mutableStateOf(false)

    /** Start destination determined during splash */
    private var startDestination: Any by mutableStateOf(Login)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // Keep splash visible until app is ready
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        super.onCreate(savedInstanceState)

        PrivyProvider.setContext(this)
        enableEdgeToEdge()

        // Check auth during splash (non-blocking)
        checkAuthDuringSplash()

        setContent {
            val themeModeString by userPreferences.themeMode.collectAsState(initial = "DARK")
            val isDarkTheme = when (ThemeMode.valueOf(themeModeString)) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            FlashTradeTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val appState = rememberAppState(navController)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (appState.shouldShowBottomBar) {
                            BottomNavBar(
                                destinations = appState.topLevelDestinations,
                                currentDestination = appState.currentTopLevelDestination,
                                onNavigateToDestination = appState::navigateToTopLevelDestination
                            )
                        }
                    }
                ) { paddingValues ->
                    FlashTradeNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    private fun checkAuthDuringSplash() {
        lifecycleScope.launch {
            // Start prefetch in background (non-blocking)
            launch { prefetchManager.prefetch() }

            // Only wait for auth check (fast - reads from DataStore)
            val authResult = checkLoginStatusUseCase()

            startDestination = when (authResult) {
                is Result.Success -> {
                    val authState = authResult.data
                    if (authState.isLoggedIn && authState.isSessionValid) {
                        TradingGraph
                    } else {
                        Login
                    }
                }
                is Result.Error -> Login
                Result.Loading -> Login
            }
            isAppReady = true
        }
    }
}
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
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.otistran.flash_trade.core.datastore.UserPreferences
import com.otistran.flash_trade.di.PrivyProvider
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.presentation.navigation.BottomNavBar
import com.otistran.flash_trade.presentation.navigation.FlashTradeNavGraph
import com.otistran.flash_trade.presentation.navigation.rememberAppState
import com.otistran.flash_trade.ui.theme.FlashTradeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (handles API 28-36 automatically)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        // Set context for lazy Privy init (instant, NO blocking)
        PrivyProvider.setContext(this)
        enableEdgeToEdge()
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
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

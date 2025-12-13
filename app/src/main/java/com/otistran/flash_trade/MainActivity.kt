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
import com.otistran.flash_trade.data.local.datastore.UserPreferences
import com.otistran.flash_trade.di.PrivyProvider
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.presentation.navigation.FlashTradeNavGraph
import com.otistran.flash_trade.ui.theme.FlashTradeTheme
import dagger.hilt.android.AndroidEntryPoint
import io.privy.logging.PrivyLogLevel
import io.privy.sdk.Privy
import io.privy.sdk.PrivyConfig
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (handles API 28-36 automatically)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        // Initialize Privy SDK (MUST be on main thread)
        val privyInstance = initializePrivy()
        PrivyProvider.initialize(privyInstance)
        enableEdgeToEdge()
        setContent {
            val themeModeString by userPreferences.themeMode.collectAsState(initial = "DARK")
            val isDarkTheme = when (ThemeMode.valueOf(themeModeString)) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            FlashTradeTheme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FlashTradeNavGraph(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    /**
     * Initialize Privy SDK for OAuth authentication and wallet creation
     * Uses credentials from BuildConfig (loaded from local.properties)
     */
    private fun initializePrivy(): Privy {
        return Privy.init(
            context = this,
            config = PrivyConfig(
                appId = BuildConfig.PRIVY_APP_ID,
                appClientId = BuildConfig.PRIVY_APP_CLIENT_ID, // Same for basic setup
                logLevel = if (BuildConfig.DEBUG) PrivyLogLevel.VERBOSE else PrivyLogLevel.NONE
            )
        )
    }
}

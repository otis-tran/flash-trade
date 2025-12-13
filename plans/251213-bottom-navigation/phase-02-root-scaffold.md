# Phase 02: Root Scaffold & App State

**Priority:** High | **Status:** Pending | **Effort:** 2 hours

## Context

- [Research: Root Scaffold Pattern](./research/researcher-01-navigation-compose.md#21-scaffold-placement-mainactivity-level)
- [Research: AppState Management](./research/researcher-01-navigation-compose.md#22-appstate-management)
- [Current MainActivity](../../../app/src/main/java/com/otistran/flash_trade/MainActivity.kt)
- [Phase 01: Routes](./phase-01-route-restructure.md)

## Overview

Refactor MainActivity to use root Scaffold with conditional bottom bar. Create AppState holder for centralized navigation logic and route detection. Move Scaffold from individual screens to MainActivity level (single source of truth).

## Key Insights from Research

- Scaffold must be at MainActivity level, not per-screen (prevents bottom bar recreation)
- AppState pattern encapsulates navigation logic, prevents recomposition issues
- @Stable annotation on AppState critical for performance
- Bottom bar visibility controlled via route set membership check
- NavHost receives padding from Scaffold (not vice versa)

## Requirements

### AppState Holder
- Centralize nav controller management
- Expose current route reactively
- Determine bottom bar visibility based on route
- Provide navigation helper methods
- Preserve state across config changes (remember)

### Root Scaffold Architecture
- Single Scaffold at MainActivity setContent level
- Conditional bottom bar: visible on TradingGraph, PortfolioGraph, SettingsGraph
- NavHost as child, receives Scaffold paddingValues
- No nested Scaffolds in individual screens

### Bottom Bar Visibility Logic
Hide bottom bar on:
- Welcome
- Login
- TradeDetails (and any future detail screens)

Show bottom bar on:
- TradingGraph/TradingScreen
- PortfolioGraph/PortfolioScreen
- SettingsGraph/SettingsScreen

## Architecture Decisions

1. **Single Scaffold:** MainActivity level only (not per-screen)
2. **AppState Pattern:** Centralized navigation state holder (@Stable for performance)
3. **Route Detection:** Use currentBackStackEntry.destination.route matching
4. **State Preservation:** rememberAppState with navController as key
5. **Padding Strategy:** Scaffold provides padding → NavHost applies it

## Related Code Files

- `app/src/main/java/com/otistran/flash_trade/MainActivity.kt` (update)
- `app/src/main/java/com/otistran/flash_trade/presentation/navigation/AppState.kt` (create)
- `app/src/main/java/com/otistran/flash_trade/presentation/navigation/TopLevelDestination.kt` (reference)

## Implementation Steps

### Step 1: Create AppState.kt
Centralized navigation state holder:
```kotlin
package com.otistran.flash_trade.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Stable
class AppState(val navController: NavHostController) {

    private val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentRoute: Any?
        @Composable get() = currentDestination?.route?.let { route ->
            // Map route string back to sealed class instance
            when {
                route.contains("TradingGraph") -> TradingGraph
                route.contains("PortfolioGraph") -> PortfolioGraph
                route.contains("SettingsGraph") -> SettingsGraph
                route.contains("Welcome") -> Welcome
                route.contains("Login") -> Login
                route.contains("TradeDetails") -> TradeDetails::class
                else -> null
            }
        }

    val shouldShowBottomBar: Boolean
        @Composable get() {
            val route = currentRoute
            return route is TradingGraph ||
                   route is PortfolioGraph ||
                   route is SettingsGraph
        }

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() = when (currentRoute) {
            is TradingGraph -> TopLevelDestination.TRADING
            is PortfolioGraph -> TopLevelDestination.PORTFOLIO
            is SettingsGraph -> TopLevelDestination.SETTINGS
            else -> null
        }

    val topLevelDestinations = TopLevelDestination.entries

    fun navigateToTopLevelDestination(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            // Pop up to start destination, save state
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun rememberAppState(
    navController: NavHostController
): AppState = remember(navController) {
    AppState(navController)
}
```

### Step 2: Update MainActivity.kt
Refactor to use root Scaffold with conditional bottom bar:
```kotlin
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
import com.otistran.flash_trade.data.local.datastore.UserPreferences
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
        installSplashScreen()
        super.onCreate(savedInstanceState)
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
```

### Step 3: Remove Scaffold from Existing Screens
Verify no individual screens use Scaffold (should only be at MainActivity level):
- Remove any Scaffold usage in TradingScreen, PortfolioScreen, SettingsScreen
- Screens should only return Column/Box/etc. content directly

### Step 4: Test Route Detection Logic
Verify shouldShowBottomBar correctly detects routes:
```kotlin
// Testing scenarios:
// Welcome screen → shouldShowBottomBar = false
// Login screen → shouldShowBottomBar = false
// TradingGraph → shouldShowBottomBar = true
// TradeDetails → shouldShowBottomBar = false
```

## Success Criteria

- [ ] AppState.kt created with @Stable annotation
- [ ] MainActivity uses root Scaffold with conditional bottom bar
- [ ] NavHost receives paddingValues from Scaffold
- [ ] Bottom bar shows on Trading/Portfolio/Settings
- [ ] Bottom bar hidden on Welcome/Login/TradeDetails
- [ ] No nested Scaffolds in individual screens
- [ ] rememberAppState preserves state across config changes
- [ ] No layout jank or bottom bar flicker during navigation
- [ ] File sizes <200 lines

## Risk Assessment

**Medium Risk:**
- Moving Scaffold from screens to MainActivity requires careful testing
- Route string matching can be fragile if route format changes

**Mitigation:**
- Test all navigation flows after refactor
- Use sealed class route matching (not string contains)
- Add unit tests for AppState route detection logic
- Keep original MainActivity.kt as backup reference

## Testing Checklist

- App launches without crashes
- Navigate: Welcome → Login (no bottom bar)
- Navigate: Login → Trading (bottom bar appears)
- Navigate: Trading → Portfolio (bottom bar persists, selection changes)
- Navigate: Trading → TradeDetails (bottom bar disappears)
- Back press from TradeDetails → Trading (bottom bar reappears)
- Rotate device on Trading screen (bottom bar state preserved)
- Theme switch (dark ↔ light) - bottom bar colors update

## Follow-up Phase

Phase 03: Bottom Navigation Component - Implement BottomNavBar with Kyber brand styling

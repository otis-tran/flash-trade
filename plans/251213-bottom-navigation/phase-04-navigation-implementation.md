# Phase 04: Navigation Implementation

**Priority:** High | **Status:** Pending | **Effort:** 2 hours

## Context

- [Research: Nested Navigation](./research/researcher-01-navigation-compose.md#13-nested-navigation-with-separate-back-stacks)
- [Research: State Preservation](./research/researcher-01-navigation-compose.md#3-state-preservation--tab-switching)
- [Current NavGraph](../../../app/src/main/java/com/otistran/flash_trade/presentation/navigation/NavGraph.kt)
- [Phase 01: Routes](./phase-01-route-restructure.md)
- [Phase 02: AppState](./phase-02-root-scaffold.md)

## Overview

Update NavGraph.kt with nested navigation graphs for each bottom nav tab. Implement tab back stack management with state preservation. Configure navigation options for smooth tab switching (saveState, restoreState, launchSingleTop).

## Key Insights from Research

- Use navigation<T> (not composable<T>) for each tab to create independent back stacks
- Each graph preserves its own NavBackStackEntry when switching tabs
- Navigation options: saveState=true, restoreState=true, launchSingleTop=true
- popUpTo start destination prevents duplicate graphs in back stack
- Tab switching doesn't destroy previous tab's screens (memory efficient)

## Requirements

### Nested Graph Structure
```
NavHost (startDestination = Welcome)
├── Welcome (composable)
├── Login (composable)
├── TradingGraph (navigation)
│   ├── TradingScreen (composable, start)
│   └── TradeDetails (composable)
├── PortfolioGraph (navigation)
│   └── PortfolioScreen (composable, start)
└── SettingsGraph (navigation)
    └── SettingsScreen (composable, start)
```

### State Preservation Requirements
- Switch Trading → Portfolio → Trading: Trading screen state preserved
- Navigate Trading → TradeDetails → Switch to Portfolio → Back to Trading: TradeDetails still in back stack
- Configuration change (rotation): All tab states survive
- Process death: Restore to last active tab

### Navigation Options
```kotlin
navController.navigate(destination) {
    popUpTo(navController.graph.startDestinationId) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

## Architecture Decisions

1. **Graph Structure:** Nested navigation graphs (navigation<T>) for each tab
2. **Start Destination:** TradingGraph after login, Welcome for new users
3. **Back Stack Strategy:** Independent per tab, preserved on switch
4. **Navigation Options:** Always use saveState + restoreState for tabs
5. **Deep Links:** Support navigation to TradeDetails from notifications

## Related Code Files

- `app/src/main/java/com/otistran/flash_trade/presentation/navigation/NavGraph.kt` (update)
- `app/src/main/java/com/otistran/flash_trade/presentation/navigation/Screen.kt` (reference)
- `app/src/main/java/com/otistran/flash_trade/presentation/navigation/AppState.kt` (reference)

## Implementation Steps

### Step 1: Update NavGraph.kt with Nested Graphs
Replace flat structure with nested graphs:
```kotlin
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
// Import other screens as needed

@Composable
fun FlashTradeNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = Welcome
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth flow (no bottom nav)
        composable<Welcome> {
            // TODO: WelcomeScreen(onNavigateToLogin = { navController.navigate(Login) })
        }

        composable<Login> {
            LoginScreen(
                onNavigateToTrading = {
                    navController.navigate(TradingGraph) {
                        popUpTo(Welcome) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Trading tab (nested graph)
        navigation<TradingGraph>(startDestination = TradingScreen) {
            composable<TradingScreen> {
                // TODO: TradingScreen(onNavigateToDetails = { id ->
                //     navController.navigate(TradeDetails(id))
                // })
            }

            composable<TradeDetails> { backStackEntry ->
                val args = backStackEntry.toRoute<TradeDetails>()
                // TODO: TradeDetailsScreen(
                //     tradeId = args.tradeId,
                //     onNavigateBack = { navController.popBackStack() }
                // )
            }
        }

        // Portfolio tab (nested graph)
        navigation<PortfolioGraph>(startDestination = PortfolioScreen) {
            composable<PortfolioScreen> {
                // TODO: PortfolioScreen()
            }
        }

        // Settings tab (nested graph)
        navigation<SettingsGraph>(startDestination = SettingsScreen) {
            composable<SettingsScreen> {
                SettingsScreen(
                    onNavigateToLogin = {
                        navController.navigate(Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

### Step 2: Update FlashTradeNavigationActions
Adapt for nested graphs with proper navigation options:
```kotlin
class FlashTradeNavigationActions(private val navController: NavHostController) {

    fun navigateToLogin() = navController.navigate(Login)

    fun navigateToTrading() = navController.navigate(TradingGraph) {
        popUpTo(Welcome) { inclusive = true }
    }

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
```

### Step 3: Verify AppState Navigation Method
Ensure AppState.navigateToTopLevelDestination uses correct options:
```kotlin
// In AppState.kt
fun navigateToTopLevelDestination(destination: TopLevelDestination) {
    navController.navigate(destination.route) {
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
```

### Step 4: Handle Back Press Behavior
Ensure back press respects tab back stacks:
- Within tab graph: Pop within graph first
- At tab root: Switch to Trading tab (default) or exit app
- Not in tab: Normal back press behavior

```kotlin
// Optional: Add to MainActivity
BackHandler(enabled = appState.shouldShowBottomBar) {
    if (!navController.popBackStack()) {
        // At root of tab, navigate to Trading or exit
        if (appState.currentTopLevelDestination != TopLevelDestination.TRADING) {
            appState.navigateToTopLevelDestination(TopLevelDestination.TRADING)
        } else {
            finish()
        }
    }
}
```

### Step 5: Test State Preservation
Verify NavBackStackEntry preserved correctly:
```kotlin
// Add logging in development builds
navController.addOnDestinationChangedListener { _, destination, _ ->
    Log.d("Navigation", "Current destination: ${destination.route}")
    Log.d("Navigation", "Back stack size: ${navController.backQueue.size}")
}
```

## Success Criteria

- [ ] NavGraph.kt updated with nested navigation graphs
- [ ] Each tab has independent navigation<T> graph
- [ ] Tab switching preserves back stack state
- [ ] Navigation options correct (saveState, restoreState, launchSingleTop)
- [ ] Back press respects graph hierarchy
- [ ] Navigate Trading → TradeDetails → Portfolio → Trading: TradeDetails preserved
- [ ] Rotate device on any screen: State survives
- [ ] No duplicate destinations in back stack
- [ ] Start destination correct (Welcome or Trading based on auth)
- [ ] File size <200 lines (if exceeded, split graphs into separate files)

## Risk Assessment

**Medium Risk:**
- Nested navigation can be complex to debug
- State preservation requires careful navigation option configuration
- Back press behavior may conflict with system back gesture

**Mitigation:**
- Add extensive logging during development
- Test all navigation flows manually
- Use Navigation Compose Destination Inspector (Android Studio)
- Document expected back stack for each flow
- Handle edge cases (process death, low memory)

## Testing Checklist

### Basic Navigation
- [ ] App launches to Welcome screen
- [ ] Login → Trading (bottom bar appears)
- [ ] Trading → Portfolio (tab switches, Trading state preserved)
- [ ] Portfolio → Settings (tab switches)
- [ ] Settings → Trading (returns to Trading state)

### Back Stack Preservation
- [ ] Trading → TradeDetails → Portfolio → Trading → Back press: Returns to TradeDetails
- [ ] Trading → Portfolio → Trading: Trading screen in same scroll position
- [ ] Portfolio → Settings → Portfolio: Portfolio state preserved

### Deep Navigation
- [ ] Trading → TradeDetails → Back: Returns to Trading
- [ ] Trading → TradeDetails → Portfolio → Back press: Returns to Portfolio
- [ ] Login → Trading: Cannot navigate back to Login

### Edge Cases
- [ ] Rotate device on Trading: State survives
- [ ] Rotate on TradeDetails: Arguments preserved
- [ ] Process death (Dev Options > Don't keep activities): Restores to correct tab
- [ ] Low memory scenario: App doesn't crash
- [ ] Multiple rapid tab switches: No jank or crashes

### Back Press Behavior
- [ ] Back from TradeDetails: Returns to Trading
- [ ] Back from Trading (root): Exits app or navigates to default tab
- [ ] Back from Portfolio (root): Stays in Portfolio or exits
- [ ] System back gesture: Works correctly

### Performance
- [ ] Tab switches <300ms
- [ ] No memory leaks (use LeakCanary)
- [ ] Smooth animations, no frame drops

## Follow-up Phase

Phase 05: Testing & Polish - Comprehensive testing, performance optimization, accessibility

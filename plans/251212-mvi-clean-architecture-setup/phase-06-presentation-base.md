# Phase 06: Presentation Base

## Context
- **Parent Plan:** [plan.md](plan.md)
- **Dependencies:** Phase 01 (MVI base), Phase 05 (DI)
- **Docs:** [system-architecture.md](../../docs/system-architecture.md)

## Overview
| Field | Value |
|-------|-------|
| Date | 2024-12-12 |
| Priority | Medium |
| Implementation Status | Pending |
| Review Status | Pending |

**Description:** Setup navigation graph, screen routes, and common UI components.

## Key Insights
- Single Activity with Compose Navigation
- Screen routes as sealed class
- Common components for loading, error states

## Requirements
1. Navigation routes (sealed class)
2. NavGraph setup with Compose Navigation
3. Common UI components (LoadingIndicator, ErrorMessage)

## Architecture

```
presentation/
├── navigation/
│   ├── Screen.kt          # Route definitions
│   └── NavGraph.kt        # Navigation host
└── common/
    ├── LoadingIndicator.kt
    └── ErrorMessage.kt
```

## Implementation Steps

### Step 1: Create Screen.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/presentation/navigation/Screen.kt`

```kotlin
package com.otistran.flash_trade.presentation.navigation

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    // Onboarding flow
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")

    // Main flow
    data object Trading : Screen("trading")
    data object Portfolio : Screen("portfolio")
    data object Settings : Screen("settings")

    // Detail screens
    data class TradeDetails(val tradeId: String = "{tradeId}") : Screen("trade/$tradeId") {
        companion object {
            const val TRADE_ID_ARG = "tradeId"
            fun createRoute(tradeId: String) = "trade/$tradeId"
        }
    }

    companion object {
        // Start destination
        val START = Welcome
    }
}
```

### Step 2: Create NavGraph.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/presentation/navigation/NavGraph.kt`

```kotlin
package com.otistran.flash_trade.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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
            // TODO: LoginScreen()
        }

        // Main
        composable(Screen.Trading.route) {
            // TODO: TradingScreen()
        }

        composable(Screen.Portfolio.route) {
            // TODO: PortfolioScreen()
        }

        composable(Screen.Settings.route) {
            // TODO: SettingsScreen()
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
            val tradeId = backStackEntry.arguments?.getString(Screen.TradeDetails.TRADE_ID_ARG) ?: ""
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
    fun navigateToTradeDetails(tradeId: String) = navController.navigate(Screen.TradeDetails.createRoute(tradeId))
    fun navigateBack() = navController.popBackStack()
}
```

### Step 3: Create LoadingIndicator.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/presentation/common/LoadingIndicator.kt`

```kotlin
package com.otistran.flash_trade.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Full-screen loading indicator.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Inline loading indicator.
 */
@Composable
fun InlineLoadingIndicator(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(
        modifier = modifier.size(24.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 2.dp
    )
}
```

### Step 4: Create ErrorMessage.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/presentation/common/ErrorMessage.kt`

```kotlin
package com.otistran.flash_trade.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Full-screen error message with retry button.
 */
@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Inline error text.
 */
@Composable
fun InlineError(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
    )
}
```

### Step 5: Update MainActivity.kt with NavGraph
**Path:** `app/src/main/java/com/otistran/flash_trade/MainActivity.kt`

```kotlin
package com.otistran.flash_trade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.otistran.flash_trade.presentation.navigation.FlashTradeNavGraph
import com.otistran.flash_trade.ui.theme.FlashtradeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlashtradeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FlashTradeNavGraph(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
```

## Todo List
- [ ] Create presentation/navigation/ directory
- [ ] Create Screen.kt
- [ ] Create NavGraph.kt
- [ ] Create presentation/common/ directory
- [ ] Create LoadingIndicator.kt
- [ ] Create ErrorMessage.kt
- [ ] Update MainActivity.kt with NavGraph
- [ ] Verify compilation and navigation

## Success Criteria
- [ ] All files created
- [ ] Each file <200 lines
- [ ] Navigation compiles
- [ ] App launches without crash

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Navigation args issues | Low | Test with placeholder screens |

## Security Considerations
- No sensitive data in routes

## Next Steps
→ Implementation complete! Ready to build features.

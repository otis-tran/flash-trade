# Phase 01: Navigation Route Restructure

**Priority:** High | **Status:** ✅ Complete | **Effort:** 1.5 hours

## Context

- [Research: Navigation Architecture](./research/researcher-01-navigation-compose.md#12-type-safe-navigation-navigation-280)
- [Current Routes](../../../app/src/main/java/com/otistran/flash_trade/presentation/navigation/Screen.kt)
- [Code Standards](../../../docs/code-standards.md#jetpack-compose-standards)

## Overview

Convert existing string-based routes to type-safe @Serializable routes using Navigation 2.8+. Create TopLevelDestination enum for bottom nav tabs. Establish foundation for nested navigation graphs.

## Key Insights from Research

- Type-safe routes eliminate runtime errors, provide compile-time safety equivalent to Safe Args
- @Serializable annotation enables automatic argument passing without manual string parsing
- navigation<T> graphs (not composable<T>) required for independent back stacks per tab
- Route hierarchy: TopLevel (graphs) → Nested destinations (composables)

## Requirements

### Route Migration
- Convert Screen sealed class to @Serializable objects/data classes
- Replace string routes with Kotlin Serialization
- Maintain existing navigation arguments (e.g., tradeId)
- No breaking changes to existing screen implementations

### TopLevelDestination Enum
- Define Trading, Portfolio, Settings as top-level tabs
- Include icon references (outlined + filled variants)
- Add labels for NavigationBarItem
- Order: Trading (primary) → Portfolio → Settings

### Graph Structure
```kotlin
// Top-level graphs (bottom nav tabs)
@Serializable object TradingGraph
@Serializable object PortfolioGraph
@Serializable object SettingsGraph

// Nested destinations (screens within tabs)
@Serializable object TradingScreen
@Serializable object PortfolioScreen
@Serializable object SettingsScreen
@Serializable data class TradeDetails(val tradeId: String)

// Auth flow (no bottom nav)
@Serializable object Welcome
@Serializable object Login
```

## Architecture Decisions

1. **Type Safety:** Use @Serializable for all routes (Navigation 2.8+ standard)
2. **Graph Hierarchy:** Top-level graphs contain nested composable destinations
3. **Start Destination:** Welcome for new users, Trading for authenticated
4. **Argument Passing:** Use data class properties instead of string concatenation

## Related Code Files

- `app/src/main/java/com/otistran/flash_trade/presentation/navigation/Screen.kt` (update)
- `app/src/main/java/com/otistran/flash_trade/presentation/navigation/TopLevelDestination.kt` (create)
- `gradle/libs.versions.toml` (verify kotlinx-serialization plugin)

## Implementation Steps

### Step 1: Add Kotlin Serialization Plugin (if missing)
Check `build.gradle.kts` for plugin:
```kotlin
plugins {
    alias(libs.plugins.kotlin.serialization)
}
```
Verify `libs.versions.toml` includes:
```toml
[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### Step 2: Update Screen.kt with Type-Safe Routes
Replace existing sealed class with @Serializable objects:
```kotlin
package com.otistran.flash_trade.presentation.navigation

import kotlinx.serialization.Serializable

// Auth flow (no bottom nav)
@Serializable object Welcome
@Serializable object Login

// Top-level navigation graphs (bottom nav)
@Serializable object TradingGraph
@Serializable object PortfolioGraph
@Serializable object SettingsGraph

// Main screens (nested in graphs)
@Serializable object TradingScreen
@Serializable object PortfolioScreen
@Serializable object SettingsScreen

// Detail screens (nested, no bottom nav)
@Serializable data class TradeDetails(val tradeId: String)
```

### Step 3: Create TopLevelDestination.kt
Define bottom nav tab configuration:
```kotlin
package com.otistran.flash_trade.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val label: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val route: Any
) {
    TRADING(
        label = "Trading",
        iconOutlined = Icons.Outlined.TrendingUp,
        iconFilled = Icons.Filled.TrendingUp,
        route = TradingGraph
    ),
    PORTFOLIO(
        label = "Portfolio",
        iconOutlined = Icons.Outlined.AccountBalanceWallet,
        iconFilled = Icons.Filled.AccountBalanceWallet,
        route = PortfolioGraph
    ),
    SETTINGS(
        label = "Settings",
        iconOutlined = Icons.Outlined.Settings,
        iconFilled = Icons.Filled.Settings,
        route = SettingsGraph
    )
}
```

### Step 4: Verify Navigation Dependency
Check `libs.versions.toml` has Navigation 2.8.0+:
```toml
[versions]
navigation-compose = "2.8.4"
```

### Step 5: Update FlashTradeNavigationActions
Adapt navigation helper class for type-safe routes:
```kotlin
class FlashTradeNavigationActions(private val navController: NavHostController) {
    fun navigateToLogin() = navController.navigate(Login)
    fun navigateToTrading() = navController.navigate(TradingGraph) {
        popUpTo(Welcome) { inclusive = true }
    }
    fun navigateToPortfolio() = navController.navigate(PortfolioGraph)
    fun navigateToSettings() = navController.navigate(SettingsGraph)
    fun navigateToTradeDetails(tradeId: String) =
        navController.navigate(TradeDetails(tradeId))
    fun navigateBack() = navController.popBackStack()
}
```

## Success Criteria

- [ ] Kotlin Serialization plugin added to build config
- [ ] All routes converted to @Serializable (no string routes remain)
- [ ] TopLevelDestination enum created with 3 tabs (Trading, Portfolio, Settings)
- [ ] Graph hierarchy clear: TopLevel graphs → Nested screens
- [ ] No compilation errors
- [ ] Existing navigation logic still works (no breaking changes)
- [ ] File size <200 lines (split Screen.kt + TopLevelDestination.kt)

## Risk Assessment

**Low Risk:**
- Navigation 2.8+ is stable, well-documented
- Type-safe routes are backwards compatible with existing composables
- Non-breaking change (internal refactor only)

**Mitigation:**
- Test all navigation paths after migration
- Keep original Screen.kt as reference during transition
- Verify no hardcoded string routes remain in codebase

## Testing Checklist

- Compile project successfully
- Navigate: Welcome → Login → Trading
- Navigate: Trading → TradeDetails(id="test123")
- Verify arguments passed correctly to TradeDetails screen
- No runtime crashes or navigation failures

## Follow-up Phase

Phase 02: Root Scaffold & App State - Use TopLevelDestination for bottom bar rendering

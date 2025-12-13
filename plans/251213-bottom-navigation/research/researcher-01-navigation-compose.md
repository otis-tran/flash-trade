# Jetpack Compose Navigation Best Practices
## Research Report: Bottom Navigation Architecture (2024-2025)

**Date:** 2025-12-13 | **Focus:** Navigation Compose 2.8+ | **Target:** Web3 Trading App

---

## 1. Bottom Navigation Architecture Patterns

### 1.1 Single Unified Navigation Graph (Recommended)
- Use a single `NavHost` with dynamic bottom bar visibility
- Control visibility via centralized route set: `bottomBarRoutes = setOf(Home, Portfolio, Settings)`
- Condition display in `Scaffold`: `if (currentRoute in bottomBarRoutes) { NavigationBar(...) }`
- **Benefit:** Simplified state management, single source of truth
- **Trade-off:** Requires route awareness for conditional rendering

### 1.2 Type-Safe Navigation (Navigation 2.8.0+)
**Standard Pattern:**
```kotlin
@Serializable
object Home

@Serializable
data class Profile(val id: String)

NavHost(navController, startDestination = Home) {
    composable<Home> { HomeScreen() }
    composable<Profile> { backStackEntry ->
        val args: Profile = backStackEntry.toRoute()
        ProfileScreen(args.id)
    }
}

// Navigate with compile-time safety
navController.navigate(Profile(id = "123"))
```
- **Benefits:** Compile-time type safety, no string-based routes, equivalent to Safe Args
- **Requirements:** Navigation 2.8.0+, Kotlin Serialization plugin

### 1.3 Nested Navigation with Separate Back Stacks
Each tab maintains independent navigation stack:
```kotlin
NavHost(navController, startDestination = Home) {
    navigation<HomeGraph>(startDestination = HomeScreen) {
        composable<HomeScreen> { /* Home content */ }
        composable<HomeDetails> { /* Nested screen */ }
    }
    navigation<PortfolioGraph>(startDestination = PortfolioScreen) {
        composable<PortfolioScreen> { /* Portfolio content */ }
        composable<PortfolioDetails> { /* Nested screen */ }
    }
}
```
- Each graph maintains own back stack without affecting sibling tabs
- Pop within graph doesn't affect other tabs
- Back press handled per-graph

---

## 2. Root Scaffold Pattern

### 2.1 Scaffold Placement: MainActivity Level
**Recommended structure:**
```kotlin
// MainActivity.kt
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val appState = rememberAppState(navController)

    Scaffold(
        bottomBar = {
            if (appState.shouldShowBottomBar) {
                NavigationBar {
                    appState.topLevelDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = appState.currentTopLevelDestination == dest,
                            onClick = { appState.navigate(dest) },
                            label = { Text(dest.label) },
                            icon = { Icon(...) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Navigation graph definitions
        }
    }
}
```

### 2.2 AppState Management
Create centralized navigation state holder:
```kotlin
@Stable
class AppState(val navController: NavController) {
    val currentRoute: String?
        get() = navController.currentBackStackEntry?.destination?.route

    val shouldShowBottomBar: Boolean
        get() = currentRoute in bottomBarRoutes

    fun navigate(destination: Any) {
        navController.navigate(destination)
    }
}

@Composable
fun rememberAppState(navController: NavController) =
    remember(navController) { AppState(navController) }
```

### 2.3 Conditional Bar Display
Routes where bottom bar should hide:
```kotlin
val bottomBarRoutes = setOf(
    Login::class.qualifiedName,
    Splash::class.qualifiedName,
    DetailsScreen::class.qualifiedName
)

val shouldShowBottomBar = currentRoute?.let {
    it !in bottomBarRoutes
} ?: false
```

---

## 3. State Preservation & Tab Switching

### 3.1 Per-Tab Back Stack Preservation
Bottom navigation tabs must each preserve their own navigation stack:
- When switching to Tab A → nested flow → Tab B → back to Tab A, the stack should be at the same nested state
- **Implementation:** Use `navigation<T>` for each tab's root graph instead of `composable<T>`
- Compose automatically preserves `NavBackStackEntry` for each tab's graph

### 3.2 Avoid Recomposition Issues
- Use `@Stable` annotation on `AppState` to prevent unnecessary recomposition
- Leverage `remember()` with navController as key
- Keep bottom bar composable outside NavHost padding scope to preserve its state

### 3.3 DeepLink & State Restoration
```kotlin
composable<DeepLinkDestination>(
    deepLinks = listOf(navDeepLink<DeepLinkDestination>(
        uriPattern = "https://app.com/route/{id}"
    ))
) { backStackEntry ->
    val args = backStackEntry.toRoute<DeepLinkDestination>()
    // Content
}
```

---

## 4. Performance Best Practices

### 4.1 Navigation State Restoration
- Never recreate `NavController` on recomposition
- Use `rememberNavController()` to preserve controller instance
- Leverage `SavedStateHandle` for ViewModel state

### 4.2 Memory-Efficient Tab Switching
- Bottom navigation doesn't destroy screens when switching tabs (good)
- Each tab's NavBackStackEntry is cached by Navigation system
- Monitor: Avoid retaining large objects in navigation graph scope
- Use `navGraphViewModel()` for tab-scoped data, not screen-scoped

### 4.3 Adaptive Navigation (NavigationSuiteScaffold)
For apps targeting multiple device types:
```kotlin
NavigationSuiteScaffold(
    navigationSuiteItems = {
        appState.topLevelDestinations.forEach { destination ->
            item(
                selected = appState.currentTopLevelDestination == destination,
                onClick = { appState.navigate(destination) },
                icon = { Icon(...) },
                label = { Text(destination.label) }
            )
        }
    }
) {
    NavHost(...)
}
```
- Automatically switches between `NavigationBar` (compact), `NavigationRail` (medium), and `PermanentNavigationDrawer` (expanded)
- Handles window size changes seamlessly

---

## 5. Web3 Trading App Specific Recommendations

### 5.1 Route Structure for Trading Features
```kotlin
// Top-level destinations (show bottom bar)
@Serializable object Home
@Serializable object Portfolio
@Serializable object Markets
@Serializable object Settings

// Nested destinations (hide bottom bar in some cases)
@Serializable data class TradeDetails(val tokenId: String)
@Serializable data class OrderHistory(val walletAddress: String)
@Serializable data class ConnectWallet(val source: String = "settings")
```

### 5.2 Tab-Specific Back Stacks
Each trading view (Home, Portfolio, Markets) needs independent history:
- Home: Browse → Token Details → Trade → Order Confirmation
- Portfolio: Holdings → Position Details → Trade History
- Markets: Watchlist → Chart → Trade → Order Details

Use nested navigation graphs to naturally manage this separation.

### 5.3 Full-Screen Dialogs & Modals
For wallet connection or order confirmation, consider:
- Separate non-bottom-bar routes instead of dialogs
- Or use route detection: `if (currentRoute is ConnectWallet) hideBottomBar()`

---

## Key Implementation Checklist

- [x] Use Navigation Compose 2.8.0+ with type-safe routes (@Serializable)
- [x] Create AppState holder for centralized navigation logic
- [x] Place Scaffold at MainActivity level, NavHost as child
- [x] Implement dynamic bottom bar visibility based on current route
- [x] Use `navigation<T>` graphs for each bottom tab (not `composable<T>`)
- [x] Leverage `@Stable` annotation on AppState
- [x] Test back stack preservation across tab switches
- [x] Consider NavigationSuiteScaffold for adaptive layouts
- [x] Handle deep links at composable definition level

---

## Unresolved Questions

1. **Trading-specific:** Should real-time price updates use shared navigation state or ViewModel layer?
2. **Wallet integration:** How to handle multi-wallet switching within existing bottom nav structure?
3. **Order flows:** Should order confirmation be a separate route or modal overlay?

---

## Sources

- [Navigation bar | Jetpack Compose](https://developer.android.com/develop/ui/compose/components/navigation-bar)
- [Navigation with Compose | Jetpack Compose](https://developer.android.com/develop/ui/compose/navigation)
- [Type safety in Kotlin DSL and Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety)
- [Jetpack Navigation 3 Announcement (2025)](https://android-developers.googleblog.com/2025/05/announcing-jetpack-navigation-3-for-compose.html)
- [Type-Safe Navigation for Compose (Navigation 2.8.0)](https://medium.com/androiddevelopers/type-safe-navigation-for-compose-105325a97657)
- [Bottom Navigation + Nested Navigation (Saurabh Jadhav Blog)](https://saurabhjadhavblogs.com/jetpack-compose-bottom-navigation-nested-navigation-solved)
- [Advanced Navigation Patterns (Medium)](https://medium.com/@rushabhprajapati20/advanced-navigation-in-jetpack-compose-deep-dive-into-navigation-patterns-and-best-practices-5c3b4b15ddb0)
- [UI Layer Architecture for Persistent UI Elements](https://www.tunjid.com/articles/ui-layer-architecture-for-persistent-ui-elements-68248e8ecc8e85f53ce1aa46)
- [Conditional Navigation Bar Display](https://www.valueof.io/blog/should-show-bottombar-conditionally-in-jetpack-compose)
- [Adaptive Navigation in Compose (2024)](https://android-developers.googleblog.com/2024/09/jetpack-compose-apis-for-building-adaptive-layouts-material-guidance-now-stable.html)

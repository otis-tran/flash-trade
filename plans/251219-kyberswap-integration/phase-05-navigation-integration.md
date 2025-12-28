# Phase 5: Navigation & Integration

**Duration:** 15 minutes
**Dependencies:** Phase 4 (Presentation)
**Risk Level:** Low

---

## Context Links

- **Navigation:** `app/src/main/java/com/otistran/flash_trade/presentation/navigation/FlashTradeNavGraph.kt`
- **Screen:** `app/src/main/java/com/otistran/flash_trade/presentation/navigation/Screen.kt`
- **TradingScreen:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingScreen.kt`
- **DI Modules:** `app/src/main/java/com/otistran/flash_trade/di/`

---

## Overview

Add SwapScreen to navigation graph with token parameters. Connect TradingScreen token click to navigate to SwapScreen. Create SwapModule for Hilt dependency injection. Wire up all components.

---

## Key Insights

1. **Navigation Pattern:**
   - Single Activity with Compose Navigation
   - Screen sealed class defines routes
   - Arguments passed via route parameters
   - TradingScreen already has token selection

2. **Hilt DI:**
   - Existing modules: NetworkModule, AuthModule, TokenModule
   - Need SwapModule for repository binding
   - ViewModel automatically provided by @HiltViewModel

3. **Token Flow:**
   - TradingScreen → token click → navigate with tokenAddress
   - SwapScreen receives tokenAddress param
   - Load token from param or use default

---

## Requirements

### Functional
- Navigate from TradingScreen to SwapScreen
- Pass token address as route parameter
- Support deep linking (optional)
- Handle back navigation

### Non-Functional
- Type-safe navigation arguments
- Smooth screen transitions
- No memory leaks

---

## Architecture

```
Navigation Flow:
TradingScreen
    ├── Token click → NavigateToSwap(tokenAddress)
    └── NavController.navigate("swap/{tokenAddress}")
            ↓
        SwapScreen
            ├── Get tokenAddress from navArgs
            └── Load token data

Hilt Modules:
SwapModule
    ├── @Binds SwapRepository
    ├── @Provides SwapMapper
    └── @Provides QuoteCacheManager
```

---

## Related Code Files

**Existing (to modify):**
1. `app/src/main/java/com/otistran/flash_trade/presentation/navigation/Screen.kt`
   - Add Swap route

2. `app/src/main/java/com/otistran/flash_trade/presentation/navigation/FlashTradeNavGraph.kt`
   - Add swap composable

3. `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingScreen.kt`
   - Add navigation callback

4. `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingViewModel.kt`
   - Update NavigateToTradeDetails effect

**New files:**
5. `app/src/main/java/com/otistran/flash_trade/di/SwapModule.kt`

---

## Implementation Steps

### Step 1: Add Swap Route to Screen (3min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/navigation/Screen.kt`

Add this sealed class member:

```kotlin
object Swap : Screen("swap/{tokenAddress}") {
    fun createRoute(tokenAddress: String) = "swap/$tokenAddress"
}
```

### Step 2: Add SwapScreen to NavGraph (5min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/navigation/FlashTradeNavGraph.kt`

Add this composable inside NavHost:

```kotlin
import com.otistran.flash_trade.presentation.feature.swap.SwapScreen

// Inside NavHost
composable(
    route = Screen.Swap.route,
    arguments = listOf(
        navArgument("tokenAddress") {
            type = NavType.StringType
        }
    )
) { backStackEntry ->
    val tokenAddress = backStackEntry.arguments?.getString("tokenAddress") ?: ""
    SwapScreen(
        tokenAddress = tokenAddress,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### Step 3: Update SwapScreen to Accept Token Address (2min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapScreen.kt`

Update function signature:

```kotlin
@Composable
fun SwapScreen(
    tokenAddress: String,
    viewModel: SwapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    // Load token on init
    LaunchedEffect(tokenAddress) {
        viewModel.onEvent(SwapEvent.LoadToken(tokenAddress))
    }

    // ... rest of screen
}
```

Update SwapEvent to include LoadToken:

```kotlin
sealed class SwapEvent : UiEvent {
    data class LoadToken(val address: String) : SwapEvent()
    // ... existing events
}
```

### Step 4: Connect TradingScreen Navigation (3min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingScreen.kt`

Update screen to handle navigation effect:

```kotlin
@Composable
fun TradingScreen(
    viewModel: TradingViewModel = hiltViewModel(),
    onNavigateToSwap: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TradingEffect.NavigateToTradeDetails -> {
                    onNavigateToSwap(effect.tokenAddress)
                }
                // ... other effects
            }
        }
    }

    // ... rest of screen
}
```

Update NavGraph caller:

```kotlin
// In FlashTradeNavGraph.kt
composable(Screen.Trading.route) {
    TradingScreen(
        onNavigateToSwap = { tokenAddress ->
            navController.navigate(Screen.Swap.createRoute(tokenAddress))
        }
    )
}
```

### Step 5: Create SwapModule for DI (2min)

**File:** `app/src/main/java/com/otistran/flash_trade/di/SwapModule.kt`

```kotlin
package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.SwapRepositoryImpl
import com.otistran.flash_trade.domain.repository.SwapRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SwapModule {

    @Binds
    @Singleton
    abstract fun bindSwapRepository(
        impl: SwapRepositoryImpl
    ): SwapRepository
}
```

---

## Todo List

- [ ] Add Swap route to Screen sealed class
- [ ] Add SwapScreen composable to NavGraph
- [ ] Add tokenAddress navigation argument
- [ ] Update SwapScreen to accept tokenAddress parameter
- [ ] Add LoadToken event to SwapEvent
- [ ] Update TradingScreen to handle navigation callback
- [ ] Update NavGraph to pass onNavigateToSwap to TradingScreen
- [ ] Create SwapModule for Hilt DI
- [ ] Test navigation flow: TradingScreen → SwapScreen
- [ ] Test back navigation

---

## Success Criteria

- [ ] Token click navigates to SwapScreen
- [ ] Token address passed correctly via route
- [ ] SwapScreen loads token data from address
- [ ] Back button returns to TradingScreen
- [ ] No navigation memory leaks
- [ ] Hilt provides all dependencies

---

## Risk Assessment

**Low Risk:**
- Standard Compose Navigation patterns
- Simple Hilt DI setup

**Potential Issues:**
1. **Token loading:** May need to fetch token details if not in cache
2. **Navigation args:** Ensure tokenAddress is URL-encoded if needed
3. **Back stack:** Verify proper back navigation behavior

---

## Security Considerations

1. **Token address validation:** Verify address format before loading
2. **Navigation deep links:** Sanitize external navigation intents
3. **State preservation:** Handle process death gracefully

---

## Next Steps

After completion:
1. Test navigation flow end-to-end
2. Proceed to Phase 6: Optimization
3. Add analytics tracking for navigation events

# Phase 04: Presentation Layer

**Status:** ⏳ Pending
**Priority:** P0 (Blocking)
**Estimated Time:** 2 hours
**Dependencies:** Phase 03 (Data Layer)

---

## Context

- [Phase 03: Data Layer](./phase-03-data-layer.md)
- [Main Plan](./plan.md)
- [Existing MviContainer](D:/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/presentation/base/MviContainer.kt)
- [Existing Screen.kt](D:/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/presentation/navigation/Screen.kt)

---

## Overview

Build MVI presentation layer: LoginIntent, LoginState, LoginViewModel (extending MviContainer), LoginScreen composable, and navigation integration. Handles user interactions, displays auth states, and navigates to Trading screen on success.

**Goal:** Clean, minimal UI that completes auth in 3-5 seconds.

---

## Key Insights

1. **MviContainer base** provides state/side effect infrastructure
2. **LoginIntent** represents user actions (tap Passkey, tap Google)
3. **LoginState** is immutable UI state (loading, success, error)
4. **LoginSideEffect** triggers one-time events (navigation, toasts)
5. **LoginScreen** observes state, emits intents
6. **Navigation** uses existing NavGraph pattern

---

## Requirements

### Functional Requirements
- Two primary buttons: Passkey (priority), Google OAuth
- Loading indicator during auth operations
- Error display with retry option
- Navigate to Trading screen on success
- Back button returns to Welcome screen

### Non-Functional Requirements
- Smooth animations (no jank)
- Accessible (content descriptions, contrast)
- Keyboard-friendly (tab navigation)
- <200 LOC per file
- Material3 design

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│          PRESENTATION LAYER                     │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │   LoginScreen (Composable)                │ │
│  │   - Observes LoginState                   │ │
│  │   - Emits LoginIntent on user action      │ │
│  └────────────┬──────────────────────────────┘ │
│               │                                 │
│               ▼                                 │
│  ┌───────────────────────────────────────────┐ │
│  │   LoginViewModel : MviContainer           │ │
│  │   - onIntent(LoginIntent)                 │ │
│  │   - state: StateFlow<LoginState>          │ │
│  │   - sideEffect: Flow<LoginSideEffect>     │ │
│  └────────────┬──────────────────────────────┘ │
│               │                                 │
│               ▼                                 │
│  ┌───────────────────────────────────────────┐ │
│  │   LoginUseCase                            │ │
│  │   (from Domain Layer)                     │ │
│  └───────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

**Data Flow:**
```
User Tap Button → LoginIntent.PasskeyLogin
→ LoginViewModel.onIntent()
→ reduce { copy(isLoading = true) }
→ LoginUseCase.invoke()
→ reduce { copy(isLoading = false, user = result) }
→ emitSideEffect(NavigateToTrading)
→ LoginScreen observes → navigate()
```

---

## Related Code Files

### Files to Create
- `presentation/auth/login-intent.kt` - User intent sealed class
- `presentation/auth/login-state.kt` - UI state data class
- `presentation/auth/login-side-effect.kt` - One-time events
- `presentation/auth/login-view-model.kt` - ViewModel with MviContainer
- `presentation/auth/login-screen.kt` - Composable UI

### Files to Modify
- `presentation/navigation/NavGraph.kt` - Add Login route

### Files to Reference
- `presentation/base/MviContainer.kt` - Base ViewModel
- `presentation/navigation/Screen.kt` - Navigation routes
- `ui/theme/` - Material3 theme

---

## Implementation Steps

### Step 1: Define LoginIntent

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/auth/login-intent.kt`

```kotlin
package com.otistran.flash_trade.presentation.auth

import com.otistran.flash_trade.presentation.base.MviIntent

/**
 * User intents for login screen.
 */
sealed class LoginIntent : MviIntent {
    /**
     * User tapped "Continue with Passkey".
     */
    data object PasskeyLogin : LoginIntent()

    /**
     * User tapped "Continue with Passkey" for first-time signup.
     */
    data object PasskeySignup : LoginIntent()

    /**
     * User tapped "Continue with Google".
     */
    data object GoogleLogin : LoginIntent()

    /**
     * User tapped retry after error.
     */
    data object Retry : LoginIntent()

    /**
     * User navigated back.
     */
    data object NavigateBack : LoginIntent()
}
```

**Lines:** ~30

---

### Step 2: Define LoginState

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/auth/login-state.kt`

```kotlin
package com.otistran.flash_trade.presentation.auth

import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.presentation.base.MviState

/**
 * UI state for login screen.
 */
data class LoginState(
    val isLoading: Boolean = false,
    val isPasskeyLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null
) : MviState {
    /**
     * True if authentication succeeded.
     */
    val isAuthenticated: Boolean get() = user != null

    /**
     * True if any auth operation in progress.
     */
    val isAnyLoading: Boolean get() = isPasskeyLoading || isGoogleLoading
}
```

**Lines:** ~25

**Note:** Separate loading flags allow showing spinner on specific button.

---

### Step 3: Define LoginSideEffect

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/auth/login-side-effect.kt`

```kotlin
package com.otistran.flash_trade.presentation.auth

import com.otistran.flash_trade.presentation.base.MviSideEffect

/**
 * One-time side effects for login screen.
 */
sealed class LoginSideEffect : MviSideEffect {
    /**
     * Navigate to Trading screen after successful auth.
     */
    data object NavigateToTrading : LoginSideEffect()

    /**
     * Navigate back to Welcome screen.
     */
    data object NavigateBack : LoginSideEffect()

    /**
     * Show error toast (for non-critical errors).
     */
    data class ShowToast(val message: String) : LoginSideEffect()
}
```

**Lines:** ~25

---

### Step 4: Implement LoginViewModel

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/auth/login-view-model.kt`

```kotlin
package com.otistran.flash_trade.presentation.auth

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.BuildConfig
import com.otistran.flash_trade.domain.model.AuthMethod
import com.otistran.flash_trade.domain.model.OAuthProvider
import com.otistran.flash_trade.domain.usecase.LoginUseCase
import com.otistran.flash_trade.presentation.base.MviContainer
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RELYING_PARTY = "https://flashtrade.app" // TODO: Replace with actual domain
private const val OAUTH_SCHEME = "com.otistran.flashtrade.privy"

/**
 * ViewModel for login screen.
 * Handles passkey and OAuth authentication.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : MviContainer<LoginState, LoginIntent, LoginSideEffect>(
    initialState = LoginState()
) {

    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            LoginIntent.PasskeyLogin -> handlePasskeyLogin(isSignup = false)
            LoginIntent.PasskeySignup -> handlePasskeyLogin(isSignup = true)
            LoginIntent.GoogleLogin -> handleGoogleLogin()
            LoginIntent.Retry -> handleRetry()
            LoginIntent.NavigateBack -> emitSideEffect(LoginSideEffect.NavigateBack)
        }
    }

    private fun handlePasskeyLogin(isSignup: Boolean) {
        viewModelScope.launch {
            reduce { copy(isPasskeyLoading = true, error = null) }

            val method = AuthMethod.Passkey(relyingParty = RELYING_PARTY)
            when (val result = loginUseCase(method, isSignup)) {
                is Result.Success -> {
                    reduce {
                        copy(
                            isPasskeyLoading = false,
                            user = result.data
                        )
                    }
                    emitSideEffect(LoginSideEffect.NavigateToTrading)
                }
                is Result.Error -> {
                    reduce {
                        copy(
                            isPasskeyLoading = false,
                            error = result.message
                        )
                    }
                }
                Result.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    private fun handleGoogleLogin() {
        viewModelScope.launch {
            reduce { copy(isGoogleLoading = true, error = null) }

            val method = AuthMethod.OAuth(
                provider = OAuthProvider.GOOGLE,
                scheme = OAUTH_SCHEME
            )
            when (val result = loginUseCase(method, isSignup = false)) {
                is Result.Success -> {
                    reduce {
                        copy(
                            isGoogleLoading = false,
                            user = result.data
                        )
                    }
                    emitSideEffect(LoginSideEffect.NavigateToTrading)
                }
                is Result.Error -> {
                    reduce {
                        copy(
                            isGoogleLoading = false,
                            error = result.message
                        )
                    }
                }
                Result.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    private fun handleRetry() {
        reduce { copy(error = null) }
    }
}
```

**Lines:** ~110

**Key Features:**
- Extends MviContainer for state/side effect management
- Separate loading states for each button
- Clear error on retry
- Navigation via side effects (not direct)

---

### Step 5: Create LoginScreen Composable

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/auth/login-screen.kt`

```kotlin
package com.otistran.flash_trade.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otistran.flash_trade.R

/**
 * Login screen with passkey and OAuth options.
 */
@Composable
fun LoginScreen(
    onNavigateToTrading: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                LoginSideEffect.NavigateToTrading -> onNavigateToTrading()
                LoginSideEffect.NavigateBack -> onNavigateBack()
                is LoginSideEffect.ShowToast -> {
                    // TODO: Show toast (requires SnackbarHost)
                }
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Branding
            Text(
                text = "Flash Trade",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "One-tap crypto trading",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Error display
            if (state.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = state.error ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Passkey button (primary)
            Button(
                onClick = { viewModel.onIntent(LoginIntent.PasskeyLogin) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isAnyLoading
            ) {
                if (state.isPasskeyLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue with Passkey")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google OAuth button (secondary)
            OutlinedButton(
                onClick = { viewModel.onIntent(LoginIntent.GoogleLogin) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isAnyLoading
            ) {
                if (state.isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text("Continue with Google")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy/Terms (optional)
            Text(
                text = "By continuing, you agree to our Terms and Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

**Lines:** ~135

**Design Notes:**
- Minimal, clean layout
- Primary button for passkey (branding)
- Loading indicator replaces button text
- Error shown above buttons (dismissable via retry)
- Material3 components for consistency

---

### Step 6: Update NavGraph

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/navigation/NavGraph.kt`

**Modify existing file to add Login route:**

```kotlin
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route,
        modifier = modifier
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        // NEW: Login screen
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToTrading = {
                    navController.navigate(Screen.Trading.route) {
                        // Clear back stack (can't go back to login)
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Trading.route) {
            TradingScreen()
        }

        // ... other routes
    }
}
```

**Key Navigation:**
- Login → Trading clears back stack
- Back from Login → Welcome
- Can't navigate back to Login after Trading

---

## Todo List

- [ ] Create `presentation/auth/login-intent.kt`
- [ ] Define all LoginIntent cases
- [ ] Create `presentation/auth/login-state.kt`
- [ ] Add isAuthenticated computed property
- [ ] Create `presentation/auth/login-side-effect.kt`
- [ ] Define navigation and toast effects
- [ ] Create `presentation/auth/login-view-model.kt`
- [ ] Extend MviContainer base class
- [ ] Implement onIntent for all cases
- [ ] Add separate loading states per button
- [ ] Create `presentation/auth/login-screen.kt`
- [ ] Build Scaffold with Column layout
- [ ] Add Passkey button (primary)
- [ ] Add Google button (secondary)
- [ ] Add error display card
- [ ] Add loading indicators
- [ ] Observe side effects for navigation
- [ ] Update NavGraph.kt with Login route
- [ ] Configure navigation back stack clearing
- [ ] Test navigation flow
- [ ] Build project and verify compilation

---

## Success Criteria

- [ ] LoginScreen displays two auth buttons
- [ ] Tapping Passkey triggers CredentialManager
- [ ] Tapping Google opens OAuth browser flow
- [ ] Loading spinner shows on active button
- [ ] Error message displays on failure
- [ ] Success navigates to Trading screen
- [ ] Back navigation returns to Welcome
- [ ] No state leaks on configuration change
- [ ] Smooth animations (no jank)
- [ ] Accessible (TalkBack friendly)

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Navigation back stack issues | Medium | Medium | Test with back button, verify popUpTo |
| State not preserved on rotation | Low | Low | collectAsStateWithLifecycle handles this |
| Side effect missed | Low | Medium | LaunchedEffect in LoginScreen observes all |
| Button states out of sync | Low | Low | Separate loading flags per button |
| UI thread blocking | Low | High | All auth calls in viewModelScope (IO) |

---

## Security Considerations

- **No password fields** - passkey/OAuth only
- **No credential storage in UI** - handled by SDK
- **Error messages sanitized** - no stack traces shown
- **Back stack cleared** - prevent auth screen revisit

---

## UI/UX Best Practices

1. **Primary action clear:** Passkey button prominent
2. **Loading feedback:** Spinner on specific button
3. **Error recovery:** Retry clears error, re-enables buttons
4. **Accessibility:** Content descriptions for icons
5. **Responsive:** Works on small/large screens

---

## Testing Recommendations

**Manual testing:**
1. Tap Passkey → verify CredentialManager dialog
2. Cancel dialog → verify error message
3. Tap Google → verify browser opens
4. Complete OAuth → verify navigation to Trading
5. Rotate device during auth → verify state preserved
6. Test back navigation at each step

---

## Next Steps After Completion

1. Add wallet creation flow (auto-triggered after auth)
2. Implement session persistence (skip login on relaunch)
3. Add biometric re-authentication for trades
4. Add "Forgot passkey" recovery flow
5. Implement logout functionality

---

## Unresolved Questions

1. Should we auto-detect if user has existing passkey and default to login vs signup?
2. Add Apple Sign-In as third option?
3. Should error messages differ between passkey/OAuth failures?
4. Add analytics events for auth attempts/success/failures?

# Phase 05: Navigation Integration

**Duration:** 10 minutes
**Dependencies:** Phase 04 (UI components)

## Objectives

Integrate settings screen into navigation graph and handle theme changes.

## Files to Modify

### 1. NavGraph.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/navigation/NavGraph.kt`

**Changes:**
Add Settings destination to existing NavGraph:

```kotlin
// ADD import:
import com.otistran.flash_trade.presentation.settings.SettingsScreen

// ADD to NavHost composable destinations:
composable(Screen.Settings.route) {
    SettingsScreen(
        onNavigateToLogin = {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true } // Clear back stack
            }
        }
    )
}
```

**Rationale:**
- Settings route already defined in Screen.kt
- `popUpTo(0)` ensures clean logout (no back stack)
- Follows existing pattern in NavGraph

### 2. MainActivity.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/MainActivity.kt`

**Changes:**
Connect theme mode to FlashTradeTheme:

```kotlin
// ADD imports:
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.otistran.flash_trade.data.local.datastore.UserPreferences
import com.otistran.flash_trade.domain.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = "DARK")
            val isDarkTheme = when (ThemeMode.valueOf(themeMode)) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            FlashTradeTheme(darkTheme = isDarkTheme) {
                // Existing content
            }
        }
    }
}
```

**Rationale:**
- Observes theme preference from DataStore
- Applies theme dynamically (instant apply)
- Supports system theme mode
- Uses collectAsState for compose integration

### 3. Add Navigation to Settings (Optional)

**From Trading/Portfolio screens:**

```kotlin
// In TopAppBar or FloatingActionButton:
IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
    Icon(Icons.Default.Settings, contentDescription = "Settings")
}
```

**From Bottom Navigation Bar:**
```kotlin
// If using bottom nav:
NavigationBarItem(
    selected = currentRoute == Screen.Settings.route,
    onClick = { navController.navigate(Screen.Settings.route) },
    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
    label = { Text("Settings") }
)
```

## Testing Navigation Flow

### Manual Test Checklist

1. **Navigate to Settings:**
   - [ ] Tap settings icon/button
   - [ ] Settings screen loads

2. **Network Mode Toggle:**
   - [ ] Toggle to Mainnet → shows confirmation dialog
   - [ ] Cancel → network stays Testnet
   - [ ] Confirm → network changes to Mainnet
   - [ ] Toggle back to Testnet → instant switch (no dialog)
   - [ ] Chip color changes (green → red)

3. **Theme Mode Toggle:**
   - [ ] Toggle to Light → theme changes instantly
   - [ ] Icon crossfades (Dark → Light)
   - [ ] Toggle back to Dark → theme changes
   - [ ] Preference persists (restart app → theme retained)

4. **Logout Flow:**
   - [ ] Tap Logout → shows bottom sheet
   - [ ] Cancel → sheet dismisses, stays logged in
   - [ ] Confirm → navigates to Login screen
   - [ ] Back button disabled (back stack cleared)
   - [ ] UserPreferences cleared (re-login required)

5. **Theme Persistence:**
   - [ ] Change theme to Light
   - [ ] Close app (kill process)
   - [ ] Reopen app → Light theme applied

## Integration with Privy Logout

**Future Enhancement (not in this phase):**

When Privy SDK is integrated, update logout handler:

```kotlin
// In SettingsViewModel:
private fun handleLogoutConfirm() {
    viewModelScope.launch {
        reduce { copy(isLoggingOut = true, showLogoutConfirmSheet = false) }
        try {
            // ADD: Call Privy logout
            // privyClient.logout()

            userPreferences.clear()
            settingsRepository.clearSettings()

            emitSideEffect(SettingsSideEffect.NavigateToLogin)
        } catch (e: Exception) {
            reduce { copy(isLoggingOut = false, error = e.message) }
        }
    }
}
```

## Acceptance Criteria

- [ ] Settings route added to NavGraph
- [ ] MainActivity observes theme preference
- [ ] Theme changes apply instantly
- [ ] Logout clears back stack
- [ ] Navigation to/from Settings works
- [ ] Theme persists across app restarts
- [ ] All navigation flows tested

## Deployment Notes

1. **Gradle Sync:** No new dependencies needed
2. **Build:** Clean build recommended (`./gradlew clean assembleDebug`)
3. **Test:** Run on emulator + physical device
4. **Verify:** Theme persistence, logout flow, network confirmation

## Post-Implementation Tasks

- [ ] Add unit tests for SettingsViewModel
- [ ] Add instrumentation tests for navigation
- [ ] Update docs/codebase-summary.md
- [ ] Create PR with screenshots
- [ ] Test on Android 10+ devices

## Success Metrics

- Settings screen loads in < 200ms
- Theme toggle has < 50ms latency
- Network mode persists across restarts
- Logout completes in < 500ms
- Zero crashes in manual testing

## Unresolved Questions (Carry Forward)

1. Should Settings be in bottom nav or hamburger menu?
   - **Recommendation:** Bottom nav for quick access
2. Add biometric toggle in future iteration?
   - **Recommendation:** Separate phase after Privy integration
3. Privy SDK logout method signature?
   - **Action:** Check Privy docs when SDK is integrated

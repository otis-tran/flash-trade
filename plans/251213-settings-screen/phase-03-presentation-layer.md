# Phase 03: Presentation Layer

**Duration:** 25 minutes
**Dependencies:** Phase 02 (data layer)

## Objectives

Create MVI presentation layer (ViewModel, Intent, State, SideEffect).

## Files to Create

### 1. SettingsIntent.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/SettingsIntent.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.presentation.base.MviIntent

/**
 * User intents for settings screen.
 */
sealed class SettingsIntent : MviIntent {
    /** User toggled network mode switch. */
    data class ToggleNetworkMode(val newMode: NetworkMode) : SettingsIntent()

    /** User confirmed mainnet switch in dialog. */
    data object ConfirmMainnetSwitch : SettingsIntent()

    /** User canceled mainnet switch. */
    data object CancelMainnetSwitch : SettingsIntent()

    /** User toggled theme mode. */
    data class ToggleThemeMode(val newMode: ThemeMode) : SettingsIntent()

    /** User tapped logout button. */
    data object RequestLogout : SettingsIntent()

    /** User confirmed logout in bottom sheet. */
    data object ConfirmLogout : SettingsIntent()

    /** User canceled logout. */
    data object CancelLogout : SettingsIntent()

    /** Dismiss error message. */
    data object DismissError : SettingsIntent()
}
```

**Rationale:**
- Separate intents for request/confirm (two-step flows)
- Explicit cancel intents for dialog dismissal
- Follows LoginIntent pattern

### 2. SettingsState.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/SettingsState.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.presentation.base.MviState

/**
 * UI state for settings screen.
 */
data class SettingsState(
    val networkMode: NetworkMode = NetworkMode.TESTNET,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isAutoSellEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val showMainnetConfirmDialog: Boolean = false,
    val showLogoutConfirmSheet: Boolean = false,
    val pendingNetworkMode: NetworkMode? = null,
    val isLoggingOut: Boolean = false,
    val error: String? = null
) : MviState {
    /** True if network is production (show warning). */
    val isMainnet: Boolean get() = networkMode == NetworkMode.MAINNET

    /** True if any operation in progress. */
    val hasActiveOperation: Boolean get() = isLoading || isLoggingOut
}
```

**Rationale:**
- Immutable state with default values
- Dialog/sheet visibility flags
- `pendingNetworkMode` for confirmation flow
- Computed properties for UI logic
- Follows LoginState pattern

### 3. SettingsSideEffect.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/SettingsSideEffect.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings

import com.otistran.flash_trade.presentation.base.MviSideEffect

/**
 * One-time side effects for settings screen.
 */
sealed class SettingsSideEffect : MviSideEffect {
    /** Navigate back to login after logout. */
    data object NavigateToLogin : SettingsSideEffect()

    /** Show toast message. */
    data class ShowToast(val message: String) : SettingsSideEffect()
}
```

**Rationale:**
- Side effects for one-time events
- Navigation handled via side effect
- Toast for success feedback

### 4. SettingsViewModel.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/SettingsViewModel.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.data.local.datastore.UserPreferences
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.domain.repository.SettingsRepository
import com.otistran.flash_trade.presentation.base.MviContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for settings screen.
 * Manages network mode, theme mode, and logout flow.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userPreferences: UserPreferences
) : MviContainer<SettingsState, SettingsIntent, SettingsSideEffect>(
    initialState = SettingsState()
) {

    init {
        loadSettings()
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ToggleNetworkMode -> handleNetworkModeToggle(intent.newMode)
            SettingsIntent.ConfirmMainnetSwitch -> handleMainnetConfirm()
            SettingsIntent.CancelMainnetSwitch -> handleMainnetCancel()
            is SettingsIntent.ToggleThemeMode -> handleThemeModeToggle(intent.newMode)
            SettingsIntent.RequestLogout -> handleLogoutRequest()
            SettingsIntent.ConfirmLogout -> handleLogoutConfirm()
            SettingsIntent.CancelLogout -> handleLogoutCancel()
            SettingsIntent.DismissError -> reduce { copy(error = null) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .catch { e ->
                    reduce { copy(isLoading = false, error = e.message) }
                }
                .collect { settings ->
                    reduce {
                        copy(
                            networkMode = settings.networkMode,
                            themeMode = settings.themeMode,
                            isAutoSellEnabled = settings.isAutoSellEnabled,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun handleNetworkModeToggle(newMode: NetworkMode) {
        // Show confirmation only when switching TO mainnet
        if (newMode == NetworkMode.MAINNET && state.value.networkMode != NetworkMode.MAINNET) {
            reduce {
                copy(
                    showMainnetConfirmDialog = true,
                    pendingNetworkMode = newMode
                )
            }
        } else {
            // Instant switch for testnet
            applyNetworkMode(newMode)
        }
    }

    private fun handleMainnetConfirm() {
        state.value.pendingNetworkMode?.let { mode ->
            applyNetworkMode(mode)
        }
        reduce {
            copy(
                showMainnetConfirmDialog = false,
                pendingNetworkMode = null
            )
        }
    }

    private fun handleMainnetCancel() {
        reduce {
            copy(
                showMainnetConfirmDialog = false,
                pendingNetworkMode = null
            )
        }
    }

    private fun applyNetworkMode(mode: NetworkMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setNetworkMode(mode)
                emitSideEffect(
                    SettingsSideEffect.ShowToast("Switched to ${mode.displayName}")
                )
            } catch (e: Exception) {
                reduce { copy(error = e.message) }
            }
        }
    }

    private fun handleThemeModeToggle(newMode: ThemeMode) {
        viewModelScope.launch {
            try {
                settingsRepository.setThemeMode(newMode)
            } catch (e: Exception) {
                reduce { copy(error = e.message) }
            }
        }
    }

    private fun handleLogoutRequest() {
        reduce { copy(showLogoutConfirmSheet = true) }
    }

    private fun handleLogoutConfirm() {
        viewModelScope.launch {
            reduce { copy(isLoggingOut = true, showLogoutConfirmSheet = false) }
            try {
                // Clear all user data
                userPreferences.clear()
                settingsRepository.clearSettings()

                // Navigate to login
                emitSideEffect(SettingsSideEffect.NavigateToLogin)
            } catch (e: Exception) {
                reduce { copy(isLoggingOut = false, error = e.message) }
            }
        }
    }

    private fun handleLogoutCancel() {
        reduce { copy(showLogoutConfirmSheet = false) }
    }
}
```

**Rationale:**
- Extends MviContainer (follows existing pattern)
- Observes settings in init (reactive updates)
- Two-step confirmation for mainnet/logout
- Error handling with try-catch
- Side effects for navigation and toasts

## Acceptance Criteria

- [ ] SettingsIntent covers all user actions
- [ ] SettingsState is immutable with defaults
- [ ] SettingsSideEffect handles one-time events
- [ ] SettingsViewModel extends MviContainer
- [ ] Mainnet confirmation flow works
- [ ] Logout clears all data
- [ ] All files < 200 lines
- [ ] Compiles without errors

## Next Phase

Phase 04 will implement UI components.

# Settings Screen Implementation Plan

**Date:** 2025-12-13
**Feature:** Settings screen with Network Mode, Theme Mode, Logout
**Architecture:** MVI + Clean Architecture
**Status:** Planning

## Overview

Implement settings screen following existing MVI patterns with:
- Network mode toggle (Mainnet/Testnet) with confirmation dialog
- Theme mode toggle (Dark/Light) with instant apply
- Logout section (danger zone) with bottom sheet confirmation
- Kyber brand styling with Material3 components

## Architecture

```
features/settings/
├── domain/
│   ├── model/SettingsModel.kt           # Domain model for settings
│   └── repository/SettingsRepository.kt # Repository interface
├── data/
│   ├── SettingsDataStore.kt            # DataStore preferences
│   └── SettingsRepositoryImpl.kt        # Repository implementation
└── presentation/
    ├── SettingsScreen.kt                # Main composable
    ├── SettingsViewModel.kt             # MVI Container
    ├── SettingsIntent.kt                # User intents
    ├── SettingsState.kt                 # UI state
    ├── SettingsSideEffect.kt            # One-time events
    └── components/
        ├── NetworkModeSection.kt        # Network toggle + chip
        ├── ThemeModeSection.kt          # Theme toggle
        ├── LogoutSection.kt             # Danger zone
        ├── NetworkConfirmDialog.kt      # Mainnet confirm dialog
        └── LogoutConfirmSheet.kt        # Bottom sheet
```

## Implementation Phases

### Phase 01: Domain Layer (15 min)
- SettingsModel domain entity
- NetworkMode enum (Mainnet/Testnet)
- ThemeMode enum (Dark/Light/System)
- SettingsRepository interface

### Phase 02: Data Layer (20 min)
- Extend UserPreferences with theme/network keys
- SettingsDataStore wrapper
- SettingsRepositoryImpl with Flow-based reactive data
- Hilt DI module

### Phase 03: Presentation Layer (25 min)
- SettingsIntent (ToggleTheme, ToggleNetwork, Logout, etc.)
- SettingsState with network/theme/loading states
- SettingsSideEffect (ShowDialog, NavigateToLogin, etc.)
- SettingsViewModel extending MviContainer

### Phase 04: UI Components (30 min)
- SettingsScreen scaffold with sections
- NetworkModeSection (toggle + indicator chip)
- ThemeModeSection (toggle with crossfade)
- LogoutSection (danger zone styling)
- NetworkConfirmDialog (Material3 AlertDialog)
- LogoutConfirmSheet (ModalBottomSheet)

### Phase 05: Navigation Integration (10 min)
- Add Settings route to NavGraph
- Navigate from main flow
- Handle logout navigation back to Login
- Handle theme changes with rememberSaveable

## Key Design Decisions

### 1. Network Mode Confirmation
- Only show confirmation when switching TO Mainnet
- Testnet switch is instant (no confirm needed)
- Dialog emphasizes real money risk

### 2. Theme Instant Apply
- Use rememberSaveable to preserve selection
- Crossfade animation between states
- Persist immediately to DataStore

### 3. Logout Flow
- Bottom sheet (not dialog) for mobile UX
- Clear UserPreferences on confirm
- Navigate back to Login screen
- Integrate with Privy logout

### 4. Visual Design
- Settings sections with dividers
- 48dp min touch targets
- Rounded cards with KyberNavyLight surface
- Danger zone uses Error color

## Dependencies

**Existing:**
- MviContainer base class
- UserPreferences DataStore
- Navigation Screen sealed class
- FlashTradeTheme with dark/light schemes

**New:**
- androidx.compose.material3:material3 (ModalBottomSheet)
- Navigation integration for Settings route

## Testing Strategy

- ViewModel unit tests (intent handling, state updates)
- Repository tests (DataStore read/write)
- UI tests (toggle switches, dialogs, navigation)

## Success Criteria

- [ ] Network toggle persists across app restarts
- [ ] Theme change applies instantly with animation
- [ ] Mainnet confirmation shows warning
- [ ] Logout clears all user data
- [ ] All components < 200 lines
- [ ] Follows MVI pattern
- [ ] Passes unit tests

## Unresolved Questions

1. Should we add biometric toggle in this phase? (Out of scope for now)
2. Need Privy SDK logout method signature?
3. Should Settings be in bottom nav or hamburger menu?

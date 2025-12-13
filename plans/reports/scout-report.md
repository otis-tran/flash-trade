# Flash Trade Android Codebase Exploration Report

## Project Overview
- **Name**: Flash Trade - One-Click Cryptocurrency Trading for Android
- **Purpose**: Mobile app for rapid meme token trading (15-30 seconds from download to first trade)
- **Architecture**: MVI + Clean Architecture
- **UI Framework**: Jetpack Compose + Material3
- **Min SDK**: 28, Target SDK: 36
- **Language**: Kotlin 2.2.21

## Key Directory Structure
```
app/src/main/java/com/otistran/flash_trade/
├── data/                    # Data layer implementation
│   ├── local/              # Local database (Room) & DataStore
│   ├── remote/             # Remote APIs (Kyber)
│   ├── repository/         # Repository implementations
│   └── service/            # External services (Privy)
├── domain/                 # Business logic
│   ├── model/              # Domain models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Use cases
├── presentation/           # UI layer
│   ├── auth/               # Login feature
│   ├── base/               # MVI base classes
│   ├── navigation/         # Navigation Compose
│   ├── settings/           # Settings feature
│   └── common/             # Common UI components
├── di/                     # Dependency Injection (Hilt)
├── ui/theme/               # Compose theming
└── util/                   # Utilities
```

## MVI Architecture Implementation

### 1. Base MVI Components
Located in `presentation/base/`:
- **MviContainer.kt**: Abstract base ViewModel extending `ViewModel`
  - Manages `StateFlow<S>` for state
  - Uses `Channel<E>` for side effects
  - Provides `reduce()` function for state updates
  - Provides `emitSideEffect()` for one-time events
  
- **MviIntent.kt**: Marker interface for user intents
- **MviState.kt**: Marker interface for UI states
- **MviSideEffect.kt**: Marker interface for side effects

### 2. Feature Implementations

#### Login Feature (`presentation/auth/`)
- **LoginViewModel**: Extends `MviContainer<LoginState, LoginIntent, LoginSideEffect>`
  - Handles passkey and OAuth authentication
  - Uses `LoginUseCase` for business logic
  - Proper state management with loading indicators
  
- **LoginScreen**: Jetpack Compose UI
  - Collects state with `collectAsStateWithLifecycle()`
  - Handles side effects in `LaunchedEffect`
  - Sends intents via `viewModel.onIntent()`
  
- **MVI Components**:
  - `LoginIntent`: Sealed class (PasskeyLogin, GoogleLogin, Retry, NavigateBack)
  - `LoginState`: Data class with loading states, error, and user
  - `LoginSideEffect`: Sealed class (NavigateToTrading, NavigateBack, ShowToast)

#### Settings Feature (`presentation/settings/`)
- **SettingsViewModel**: Extends `MviContainer<SettingsState, SettingsIntent, SettingsSideEffect>`
  - Manages network mode, theme mode, and logout
  - Observes settings from repository
  - Handles confirmation dialogs
  
- **SettingsScreen**: Jetpack Compose UI with components
  - Component-based architecture in `components/` subdirectory
  - Proper state collection and intent handling
  
- **MVI Components**:
  - `SettingsIntent`: Sealed class for various settings actions
  - `SettingsState`: Data class with settings values
  - `SettingsSideEffect`: Navigation and toast events

### 3. Navigation
- **Navigation Compose** implementation in `presentation/navigation/`
- Single-activity architecture with `FlashTradeNavGraph`
- Screen definitions in `Screen.kt` sealed class
- Proper back stack management

## Architecture Compliance

### ✅ MVI Pattern Correctly Implemented
1. **Unidirectional Data Flow**: Intent → ViewModel → State → UI
2. **State Management**: Immutable state objects with `reduce()`
3. **Side Effects**: Separated from state using Channel
4. **Intent Handling**: Clear separation of user actions

### ✅ Clean Architecture Layers
1. **Presentation**: Compose UI + ViewModels (MVI)
2. **Domain**: Models, repository interfaces, use cases
3. **Data**: Repository implementations, data sources

### ✅ Modern Android Practices
1. **Dependency Injection**: Hilt for compile-time DI
2. **Coroutines**: Proper use of viewModelScope
3. **State Flow**: Reactive state management
4. **Compose**: Modern declarative UI

## Key Files for MVI Review

### Core MVI Files
1. `/app/src/main/java/com/otistran/flash_trade/presentation/base/MviContainer.kt`
2. `/app/src/main/java/com/otistran/flash_trade/presentation/base/MviIntent.kt`
3. `/app/src/main/java/com/otistran/flash_trade/presentation/base/MviState.kt`
4. `/app/src/main/java/com/otistran/flash_trade/presentation/base/MviSideEffect.kt`

### Implementation Examples
1. `/app/src/main/java/com/otistran/flash_trade/presentation/auth/LoginViewModel.kt`
2. `/app/src/main/java/com/otistran/flash_trade/presentation/auth/LoginScreen.kt`
3. `/app/src/main/java/com/otistran/flash_trade/presentation/settings/SettingsViewModel.kt`
4. `/app/src/main/java/com/otistran/flash_trade/presentation/settings/SettingsScreen.kt`

## Unresolved Questions
1. What specific MVI compliance aspects need to be reviewed?
2. Are there any specific features or components that need special attention?
3. Should the review focus on architectural correctness or performance optimization?
4. Are there any existing MVI violations or code smells to look for?

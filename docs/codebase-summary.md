# Flash Trade - Codebase Summary

## Project Overview

**Platform:** Android
**Language:** Kotlin 2.2.21
**UI Framework:** Jetpack Compose
**Build System:** Gradle 8.13 + AGP 8.11.2
**Completion:** ~25% (Core Utils, Base MVI, Domain Layer complete)

## Project Structure

```
flash-trade/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/otistran/flash_trade/
│   │   │   │   ├── MainActivity.kt              # Main entry point
│   │   │   │   ├── util/
│   │   │   │   │   └── Result.kt                # Result wrapper (Success/Error/Loading)
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/                   # Domain entities
│   │   │   │   │   │   ├── Token.kt             # Token entity
│   │   │   │   │   │   ├── Trade.kt             # Trade entity + TradeStatus enum
│   │   │   │   │   │   ├── Wallet.kt            # Wallet entity
│   │   │   │   │   │   └── User.kt              # User entity
│   │   │   │   │   ├── repository/              # Repository interfaces
│   │   │   │   │   │   ├── TradeRepository.kt   # Trade operations
│   │   │   │   │   │   ├── WalletRepository.kt  # Wallet operations
│   │   │   │   │   │   └── UserRepository.kt    # User operations
│   │   │   │   │   └── usecase/                 # Use case base interfaces
│   │   │   │   │       └── UseCase.kt           # UseCase, NoParamsUseCase, FlowUseCase
│   │   │   │   ├── presentation/
│   │   │   │   │   └── base/                    # MVI base classes
│   │   │   │   │       ├── MviIntent.kt         # Intent marker interface
│   │   │   │   │       ├── MviState.kt          # State marker interface
│   │   │   │   │       ├── MviSideEffect.kt     # Side effect marker interface
│   │   │   │   │       └── MviContainer.kt      # Base ViewModel for MVI
│   │   │   │   └── ui/
│   │   │   │       └── theme/                   # Compose theme setup
│   │   │   │           ├── Color.kt             # Color palette
│   │   │   │           ├── Theme.kt             # Theme configuration
│   │   │   │           └── Type.kt              # Typography
│   │   │   ├── res/                             # Resources
│   │   │   └── AndroidManifest.xml              # App manifest
│   │   ├── androidTest/                         # Instrumentation tests
│   │   └── test/                                # Unit tests
│   ├── build.gradle.kts                         # App-level build config
│   └── proguard-rules.pro                       # ProGuard rules
├── gradle/
│   └── libs.versions.toml                       # Version catalog
├── build.gradle.kts                             # Project-level build
├── settings.gradle.kts                          # Project settings
├── docs/                                        # Documentation
└── plans/                                       # Implementation plans
```

## Key Directories

### `/app/src/main/java/com/otistran/flash_trade/`
Root package for all application code. Currently contains:
- `MainActivity.kt` - Single activity architecture entry point
- `util/` - Core utilities (Result wrapper)
- `domain/` - Domain layer (models, repositories, use cases)
- `presentation/base/` - MVI foundation (Intent, State, SideEffect, Container)
- `ui/theme/` - Compose theming system

### `/docs/`
Project documentation including:
- Architecture decisions
- API documentation
- Development guidelines
- Project requirements

### `/plans/`
Implementation plans and reports for feature development.

## Current Implementation Status

### ✅ Completed (25%)

#### Project Setup
- Gradle 8.13 + AGP 8.11.2 configured
- Kotlin 2.2.21 with KSP 2.2.10-2.0.2
- Min SDK 28, Target SDK 36
- Version catalog (libs.versions.toml)

#### Dependencies Configured
- **UI:** Jetpack Compose 1.10.0 + Material3 1.4.0
- **DI:** Hilt 2.57.2
- **Networking:** Retrofit 3.0.0 + Moshi 1.15.2
- **Database:** Room 2.8.4 + DataStore 1.2.0
- **Web3:** Ethers.kt 1.5.1 + Privy 0.8.0
- **Background:** WorkManager 2.11.0
- **QR:** ZXing 3.5.4
- **Security:** Biometric 1.4.0-alpha04
- **Testing:** JUnit 4.13.2, Coroutines Test 1.10.2

#### Core Utilities (Phase 01)
- `Result.kt` - Type-safe result wrapper with Success/Error/Loading states
- Extension functions: `map()`, `onSuccess()`, `onError()`

#### MVI Foundation (Phase 01)
- `MviIntent` - Marker interface for user intents
- `MviState` - Marker interface for UI states
- `MviSideEffect` - Marker interface for one-time events
- `MviContainer` - Base ViewModel with StateFlow + Channel
  - Unidirectional data flow
  - State management via `reduce()`
  - Side effect emission via `emitSideEffect()`

#### Basic UI
- MainActivity with Compose setup
- Material3 theme (Color, Typography, Theme)
- Single activity architecture skeleton

#### Domain Layer (Phase 02)
- **Models:** Token, Trade (TradeStatus enum), Wallet, User entities
- **Repositories:** TradeRepository, WalletRepository, UserRepository interfaces
- **Use Cases:** UseCase, NoParamsUseCase, FlowUseCase base interfaces
- All domain entities follow clean architecture principles
- Repository interfaces use Result<T> wrapper and Flow for reactive data

### 🚧 In Progress (0%)
Nothing currently in progress.

### ⏳ Planned (75%)

#### Architecture Layer (Week 1)
- Data layer (repositories impl, data sources, DTOs)
- Concrete use case implementations
- Presentation layer features (ViewModels extending MviContainer, UI states)
- Dependency injection modules (Hilt)

#### Core Features (Week 2)
- Privy wallet integration
- Kyber Aggregator API client
- Token trading flow
- Auto-sell scheduling
- Wallet funding options

#### UI/UX (Week 2-3)
- Onboarding screens
- Trading interface
- Portfolio dashboard
- Transaction history
- Settings & profile

#### Testing & Polish (Week 3-4)
- Unit tests (>80% coverage)
- Integration tests
- Performance optimization
- Security hardening
- Error handling

## Main Entry Points

### `MainActivity.kt`
```kotlin
Location: app/src/main/java/com/otistran/flash_trade/MainActivity.kt
Purpose: Single activity host for Compose navigation
Dependencies: Jetpack Compose, Material3
```

Single activity that hosts the entire Compose UI hierarchy. Uses `setContent` to launch the app theme and navigation graph.

### `util/Result.kt`
```kotlin
Location: app/src/main/java/com/otistran/flash_trade/util/Result.kt
Purpose: Type-safe result wrapper for domain operations
Dependencies: None (pure Kotlin)
```

Sealed class hierarchy for Success/Error/Loading states with helper methods and extension functions.

### `presentation/base/MviContainer.kt`
```kotlin
Location: app/src/main/java/com/otistran/flash_trade/presentation/base/MviContainer.kt
Purpose: Base ViewModel for MVI pattern implementation
Dependencies: AndroidX ViewModel, Coroutines
```

Abstract base class for feature ViewModels. Manages StateFlow for UI state and Channel for side effects.

### `ui/theme/Theme.kt`
```kotlin
Location: app/src/main/java/com/otistran/flash_trade/ui/theme/Theme.kt
Purpose: Material3 theming for the app
Dependencies: Material3, Compose
```

Defines light/dark themes, dynamic color support, and theme composition.

## Architecture Overview

### Architecture: MVI + Clean Architecture (In Progress)

```
┌─────────────────────────────────────┐
│      Presentation Layer             │
│  (Composables, Intents, States)     │
│  ✅ MviContainer, MviIntent,        │
│     MviState, MviSideEffect         │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│        Domain Layer                 │
│  (Use Cases, Entities, Repositories)│
│  ✅ Models: Token, Trade, Wallet,   │
│     User                            │
│  ✅ Repositories: Trade, Wallet,    │
│     User interfaces                 │
│  ✅ UseCase base interfaces          │
│  ✅ Result<T> wrapper                │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│         Data Layer                  │
│  (Repositories, APIs, Database)     │
│  ⏳ Planned                          │
└─────────────────────────────────────┘
```

**Layers:**
- **Presentation:** UI (Compose) + MviContainer base + unidirectional data flow
- **Domain:** Business logic, use cases, domain entities wrapped in Result<T>
- **Data:** API clients, database, repositories implementation

### Technical Stack

| Component | Technology |
|-----------|------------|
| **Wallet** | Privy TEE (social login) - [privy.io](https://www.privy.io/) |
| **Swap** | Kyber Aggregator (multi-chain, MEV-safe) |
| **Auto-sell** | WorkManager (background executor) |

### Current Package Structure

```
com.otistran.flash_trade/
├── MainActivity.kt
├── util/
│   └── Result.kt                      # ✅ Result wrapper
├── domain/                            # ✅ Domain layer
│   ├── model/
│   │   ├── Token.kt                   # Token entity
│   │   ├── Trade.kt                   # Trade entity + TradeStatus
│   │   ├── Wallet.kt                  # Wallet entity
│   │   └── User.kt                    # User entity
│   ├── repository/
│   │   ├── TradeRepository.kt         # Trade operations interface
│   │   ├── WalletRepository.kt        # Wallet operations interface
│   │   └── UserRepository.kt          # User operations interface
│   └── usecase/
│       └── UseCase.kt                 # Base use case interfaces
├── presentation/
│   └── base/                          # ✅ MVI foundation
│       ├── MviIntent.kt
│       ├── MviState.kt
│       ├── MviSideEffect.kt
│       └── MviContainer.kt
└── ui/
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

### Target Package Structure

```
com.otistran.flash_trade/
├── FlashTradeApplication.kt          # Application class
├── MainActivity.kt                    # Main activity
│
├── domain/                            # ✅ Business logic
│   ├── model/                         # ✅ Domain entities (Token, Trade, Wallet, User)
│   ├── repository/                    # ✅ Repository interfaces (Trade, Wallet, User)
│   └── usecase/                       # ✅ Use case base interfaces
│
├── data/                              # Data layer
│   ├── remote/                        # API clients
│   │   ├── kyber/                     # Kyber API
│   │   └── privy/                     # Privy integration
│   ├── local/                         # Database
│   │   ├── dao/                       # Room DAOs
│   │   ├── entity/                    # Room entities
│   │   └── datastore/                 # DataStore
│   └── repository/                    # Repository implementations
│
├── di/                                # Dependency injection
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   └── DatabaseModule.kt
│
├── presentation/                      # UI layer (MVI)
│   ├── base/                          # ✅ MVI foundation (MviContainer, etc.)
│   ├── onboarding/                    # Onboarding feature
│   ├── trading/                       # Trading feature
│   ├── portfolio/                     # Portfolio feature
│   ├── settings/                      # Settings feature
│   ├── navigation/                    # Navigation graph
│   └── common/                        # Shared UI components
│
├── ui/
│   └── theme/                         # Theming
│
└── util/                              # Utilities
    ├── Result.kt                      # ✅ Result wrapper
    ├── extension/                     # Extension functions
    └── constants/                     # Constants
```

## Build Configuration

### Gradle Version Catalog (`gradle/libs.versions.toml`)

Centralized dependency management with version catalog. Key versions:
- Compose: 1.8.4
- Kotlin: 2.2.21
- Hilt: 2.57.2
- Room: 2.8.4
- Retrofit: 3.0.0
- Ethers.kt: 1.5.1

### Build Performance
- Gradle: 8.13 (fast configuration)
- KSP: 2.2.10-2.0.2 (faster than kapt)
- Parallel execution enabled
- Configuration cache enabled
- Target build time: <30 seconds

## Testing Structure

### Unit Tests (`app/src/test/`)
- ViewModel tests
- Use case tests
- Repository tests
- Utility tests
- Target: >80% coverage

### Instrumentation Tests (`app/src/androidTest/`)
- UI tests (Compose testing)
- Integration tests
- Database tests
- End-to-end flow tests

### Test Dependencies
- JUnit 4.13.2
- Coroutines Test 1.10.2 (Flow/StateFlow testing)

## External Integrations

### Kyber Aggregator API (Mandatory)
- Base URL: TBD from Kyber docs
- Purpose: Trade execution, price quotes, MEV protection
- Authentication: API key
- Status: Not yet integrated

### Privy SDK
- Version: 0.8.0
- Purpose: Wallet management, social auth
- Status: Dependency added, not implemented

### Ethers.kt
- Version: 1.5.1
- Purpose: Blockchain interactions, signing
- Status: Dependency added, not implemented

## Key Files & Their Purpose

| File | Purpose | Status |
|------|---------|--------|
| `MainActivity.kt` | App entry point | ✅ Basic setup |
| `util/Result.kt` | Result wrapper (Success/Error/Loading) | ✅ Complete |
| `presentation/base/MviContainer.kt` | Base ViewModel for MVI | ✅ Complete |
| `presentation/base/MviIntent.kt` | Intent marker interface | ✅ Complete |
| `presentation/base/MviState.kt` | State marker interface | ✅ Complete |
| `presentation/base/MviSideEffect.kt` | Side effect marker interface | ✅ Complete |
| `domain/model/Token.kt` | Token domain entity | ✅ Complete |
| `domain/model/Trade.kt` | Trade entity + TradeStatus enum | ✅ Complete |
| `domain/model/Wallet.kt` | Wallet domain entity | ✅ Complete |
| `domain/model/User.kt` | User domain entity | ✅ Complete |
| `domain/repository/TradeRepository.kt` | Trade operations interface | ✅ Complete |
| `domain/repository/WalletRepository.kt` | Wallet operations interface | ✅ Complete |
| `domain/repository/UserRepository.kt` | User operations interface | ✅ Complete |
| `domain/usecase/UseCase.kt` | Base use case interfaces | ✅ Complete |
| `FlashTradeApplication.kt` | Application class for Hilt | ⏳ Planned |
| `build.gradle.kts` | Build configuration | ✅ Complete |
| `libs.versions.toml` | Dependency versions | ✅ Complete |
| `proguard-rules.pro` | Code obfuscation rules | ⏳ To be configured |
| `AndroidManifest.xml` | App manifest | ✅ Basic setup |

## Development Workflow

### Current Phase: Foundation (Week 1)
1. ✅ MVI base classes (Result, MviContainer, etc.)
2. ✅ Domain layer (models, repository interfaces, use case bases)
3. ⏳ Dependency injection setup (Hilt modules)
4. ⏳ Navigation graph structure
5. ⏳ Data layer implementation (repositories, API clients, database)
6. ⏳ Privy integration

### Next Phase: Core Features (Week 2)
1. Kyber API integration
2. Trading flow implementation
3. Auto-sell mechanism
4. Wallet funding options

## Code Quality Standards

- File size: <200 lines per file
- Function complexity: <10 cyclomatic complexity
- No God objects
- Clear separation of concerns
- Comprehensive error handling

## Performance Benchmarks

### Current Metrics
- Cold start: ~1.2s (baseline)
- App size: TBD
- Build time: TBD

### Target Metrics
- Cold start: <800ms (excellent), <500ms (blazing)
- App size: <20MB
- Build time: <30s
- 60fps UI minimum

## Next Steps

1. ✅ MVI foundation (Result wrapper, MviContainer base)
2. ✅ Domain layer (entities, repository interfaces, use case bases)
3. Set up Hilt dependency injection modules
4. Implement data layer with Kyber API client
5. Create navigation graph and feature screens
6. Integrate Privy SDK for wallet management
7. Implement core trading flow

## Resources

- Build configuration: `build.gradle.kts`
- Dependencies: `gradle/libs.versions.toml`
- Manifest: `app/src/main/AndroidManifest.xml`
- Main code: `app/src/main/java/com/otistran/flash_trade/`

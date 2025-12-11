# Flash Trade - Codebase Summary

## Project Overview

**Platform:** Android
**Language:** Kotlin 2.2.21
**UI Framework:** Jetpack Compose
**Build System:** Gradle 8.13 + AGP 8.11.2
**Completion:** ~10% (Foundation phase)

## Project Structure

```
flash-trade/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/otistran/flash_trade/
│   │   │   │   ├── MainActivity.kt              # Main entry point
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

### ✅ Completed (10%)

#### Project Setup
- Gradle 8.13 + AGP 8.11.2 configured
- Kotlin 2.2.21 with KSP 2.2.10-2.0.2
- Min SDK 28, Target SDK 36
- Version catalog (libs.versions.toml)

#### Dependencies Configured
- **UI:** Jetpack Compose 1.8.4 + Material3 1.4.0
- **DI:** Hilt 2.57.2
- **Networking:** Retrofit 3.0.0 + Moshi 1.16.0
- **Database:** Room 2.8.4 + DataStore 1.2.0
- **Web3:** Ethers.kt 1.5.1 + Privy 0.8.0
- **Background:** WorkManager 3.0.1
- **QR:** ZXing 4.3.0
- **Security:** Biometric 1.2.0
- **Testing:** JUnit 4.13.2, Espresso 3.6.1, MockK

#### Basic UI
- MainActivity with Compose setup
- Material3 theme (Color, Typography, Theme)
- Single activity architecture skeleton

### 🚧 In Progress (0%)
Nothing currently in progress.

### ⏳ Planned (90%)

#### Architecture Layer (Week 1)
- Domain layer (use cases, entities, repositories)
- Data layer (repositories impl, data sources, DTOs)
- Presentation layer (ViewModels, UI states)
- Dependency injection modules

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

### `ui/theme/Theme.kt`
```kotlin
Location: app/src/main/java/com/otistran/flash_trade/ui/theme/Theme.kt
Purpose: Material3 theming for the app
Dependencies: Material3, Compose
```

Defines light/dark themes, dynamic color support, and theme composition.

## Architecture Overview

### Planned Architecture: MVI + Clean Architecture

```
┌─────────────────────────────────────┐
│      Presentation Layer             │
│  (Composables, Intents, States)     │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│        Domain Layer                 │
│  (Use Cases, Entities, Repositories)│
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│         Data Layer                  │
│  (Repositories, APIs, Database)     │
└─────────────────────────────────────┘
```

**Layers:**
- **Presentation:** UI (Compose) + Intents + States (unidirectional data flow)
- **Domain:** Business logic, use cases, domain entities
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
├── domain/                            # Business logic
│   ├── model/                         # Domain entities
│   ├── repository/                    # Repository interfaces
│   └── usecase/                       # Use cases
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
│   ├── onboarding/                    # Onboarding (Screen, Intent, State, Reducer)
│   ├── trading/                       # Trading (Screen, Intent, State, Reducer)
│   ├── portfolio/                     # Portfolio (Screen, Intent, State, Reducer)
│   ├── settings/                      # Settings (Screen, Intent, State, Reducer)
│   ├── navigation/                    # Navigation graph
│   └── common/                        # Shared UI components
│
├── ui/
│   └── theme/                         # Theming
│
└── util/                              # Utilities
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
- MockK (mocking)
- Turbine (Flow testing)
- Compose Testing
- Espresso 3.6.1

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
| `FlashTradeApplication.kt` | Application class for Hilt | ⏳ Planned |
| `build.gradle.kts` | Build configuration | ✅ Complete |
| `libs.versions.toml` | Dependency versions | ✅ Complete |
| `proguard-rules.pro` | Code obfuscation rules | ⏳ To be configured |
| `AndroidManifest.xml` | App manifest | ✅ Basic setup |

## Development Workflow

### Current Phase: Foundation (Week 1)
1. Architecture layer implementation
2. Dependency injection setup
3. Navigation graph structure
4. Basic UI scaffolding
5. Privy integration

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

1. Implement Clean Architecture layers
2. Set up Hilt dependency injection
3. Create navigation graph
4. Integrate Privy SDK
5. Build Kyber API client
6. Implement core trading flow

## Resources

- Build configuration: `build.gradle.kts`
- Dependencies: `gradle/libs.versions.toml`
- Manifest: `app/src/main/AndroidManifest.xml`
- Main code: `app/src/main/java/com/otistran/flash_trade/`

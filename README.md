# Flash Trade

**One-Click Cryptocurrency Trading for Android**

A mobile-first application for rapid meme token trading. Go from app download to first trade in under 15 seconds.

## Features

- **Instant Onboarding** - Social login (Google, Apple) with auto-generated embedded wallet
- **Zero-Wait Trading** - Pre-fetched token data displays immediately on launch
- **One-Tap Buy** - Select token and execute trade with single tap
- **Auto-Sell** - Automatic token sales after 24 hours
- **MEV Protection** - Protected swaps via Kyber Aggregator API
- **Real-Time Portfolio** - Live balance and transaction tracking

## The 3-Tap Flow

```
1. Sign Up (~3s)     → Social login with auto wallet creation
2. Add Funds (3-10s) → QR deposit or fiat on-ramp
3. Buy Token (~5s)   → One-tap purchase with auto-sell scheduled
```

**Total: 15-30 seconds from download to first trade**

## Tech Stack

| Category | Technology |
|----------|------------|
| Platform | Android (Min SDK 28, Target SDK 36) |
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3 |
| Architecture | MVI + Clean Architecture |
| DI | Hilt 2.54 |
| Networking | Retrofit 2.11 + Moshi |
| Wallet | Privy SDK (embedded wallet, social auth) |
| Swap | Kyber Aggregator API |
| Background | WorkManager |
| Storage | DataStore + Room |

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                 Presentation Layer                  │
│         (Compose UI + ViewModel + MVI)              │
├─────────────────────────────────────────────────────┤
│                   Domain Layer                      │
│           (Use Cases + Models + Repository)         │
├─────────────────────────────────────────────────────┤
│                    Data Layer                       │
│      (Repository Impl + API + Cache + DataStore)    │
└─────────────────────────────────────────────────────┘
```

### Key Patterns

- **MVI** - Unidirectional data flow (Event → State → Effect)
- **Repository** - Abstract data sources behind interfaces
- **Use Case** - Single-purpose business logic
- **Cache-First** - Pre-fetch tokens during splash for instant display

## Project Structure

```
app/src/main/java/com/otistran/flash_trade/
├── core/                    # Shared utilities
│   ├── base/               # BaseViewModel, UiState, UiEvent, UiEffect
│   ├── datastore/          # UserPreferences
│   ├── network/            # API client, interceptors
│   └── ui/components/      # Reusable composables
├── data/                    # Data layer
│   ├── mapper/             # DTO ↔ Domain mappers
│   ├── remote/             # API services, DTOs
│   ├── repository/         # Repository implementations
│   └── service/            # Privy auth service
├── di/                      # Hilt modules
├── domain/                  # Business logic
│   ├── manager/            # TokenCacheManager, AppStartupManager
│   ├── model/              # Domain models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Use cases
├── presentation/            # UI layer
│   ├── feature/            # Feature screens (auth, trading, portfolio, settings)
│   └── navigation/         # Navigation graph, bottom nav
└── ui/theme/                # Material3 theme
```

## Getting Started

### Prerequisites

- Android Studio Koala+
- JDK 17+
- Android SDK 28+

### Setup

```bash
# Clone repository
git clone https://github.com/anthropics/flash-trade.git
cd flash-trade

# Create local.properties with API keys
echo "PRIVY_APP_ID=your_privy_app_id" >> local.properties

# Build and run
./gradlew assembleDebug
```

### Run Tests

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumentation tests
```

## Implementation Status

| Feature | Status |
|---------|--------|
| Project Setup | ✅ Complete |
| MVI Architecture | ✅ Complete |
| Privy Auth (Social Login) | ✅ Complete |
| Kyber Token List API | ✅ Complete |
| Token Prefetch (Splash) | ⏳ Planned |
| Trading Screen | 🚧 In Progress |
| Portfolio Screen | 🚧 In Progress |
| Swap Execution | ⏳ Planned |
| Auto-Sell Worker | ⏳ Planned |
| Fiat On-Ramp | ⏳ Planned |

## Performance

| Metric | Target | Current |
|--------|--------|---------|
| Cold Start | <800ms | ~600ms |
| Token List Display | 0ms (cached) | ✅ Instant |
| Trade Execution | <5s | TBD |

## License

MIT License - See [LICENSE](LICENSE) for details.

## Acknowledgments

- [Kyber Network](https://kyberswap.com/) - Aggregator API
- [Privy](https://privy.io/) - Embedded wallet SDK
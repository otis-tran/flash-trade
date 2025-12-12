# Flash Trade

**One-Click Cryptocurrency Trading for Android**

Flash Trade is a mobile-first application designed for rapid meme token trading. Built for the Kyber Flash Trade Challenge, it enables users to go from app download to first trade execution in 15-30 seconds.

## Overview

### The 3-Tap Flow

1. **Sign Up** (~3s) - Social login or passkey with auto-generated wallet
2. **Add Funds** (3-10s) - QR code, Stripe, P2P, or Bridge
3. **Buy Token** (~5s) - One-tap buy with automatic sell after 24 hours

**Total Time: 15-30 seconds from download to first trade**

## Features

- **Instant Onboarding** - Social login (Google, Apple) with embedded wallet creation
- **One-Tap Trading** - Pre-fetched token data for zero-wait trading
- **Auto-Sell** - Automatic token sales after 24 hours
- **MEV Protection** - Protected swaps via Kyber Aggregator API
- **Biometric Security** - Device biometric authentication
- **Real-Time Portfolio** - Live balance and transaction tracking

## Tech Stack

### Core Technologies
- **Platform:** Android (Min SDK 28, Target SDK 36)
- **Language:** Kotlin 2.2.21
- **UI:** Jetpack Compose + Material3
- **Architecture:** MVI + Clean Architecture

### Key Libraries
- **Wallet:** Privy TEE (social login, embedded wallet) - [privy.io](https://www.privy.io/)
- **Swap:** Kyber Aggregator (multi-chain, efficient routing, MEV-safe)
- **Auto-sell:** WorkManager (background executor)
- **DI:** Hilt 2.57.2
- **Networking:** Retrofit 3.0.0 + Moshi
- **Database:** Room 2.8.4 + DataStore
- **Web3:** Ethers.kt 1.5.1
- **QR Codes:** ZXing
- **Security:** Biometric

### Development Tools
- **Build:** Gradle 8.13 + AGP 8.11.2
- **Code Gen:** KSP 2.2.10-2.0.2
- **Testing:** JUnit, Espresso, MockK, Turbine

## Project Structure

```
flash-trade/
├── app/
│   └── src/
│       ├── main/
│       │   └── java/com/otistran/flash_trade/
│       │       ├── MainActivity.kt
│       │       ├── domain/              # Business logic (planned)
│       │       ├── data/                # Data sources (planned)
│       │       ├── presentation/        # UI + ViewModels (planned)
│       │       ├── di/                  # Dependency injection (planned)
│       │       └── ui/theme/            # Compose theme
│       ├── test/                        # Unit tests
│       └── androidTest/                 # Instrumentation tests
├── docs/                                # Documentation
│   ├── project-overview-pdr.md
│   ├── codebase-summary.md
│   ├── code-standards.md
│   └── system-architecture.md
├── plans/                               # Implementation plans
├── gradle/
│   └── libs.versions.toml              # Dependency versions
└── README.md
```

## Getting Started

### Prerequisites

- **Android Studio:** Koala or later
- **JDK:** 17 or later
- **Android SDK:** 28+ (Target 36)
- **Gradle:** 8.13+

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/otis-tran/flash-trade.git
   cd flash-trade
   ```

2. **Open in Android Studio**
   - File → Open → Select `flash-trade` directory
   - Wait for Gradle sync to complete

3. **Configure API Keys** (when available)
   - Create `local.properties` in project root
   - Add Kyber API key: `KYBER_API_KEY=your_key_here`
   - Add Privy app ID: `PRIVY_APP_ID=your_app_id`

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or click ▶️ Run in Android Studio

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest

# All tests with coverage
./gradlew testDebugUnitTest connectedDebugAndroidTest
```

## Architecture

Flash Trade follows **MVI + Clean Architecture** principles:

```
Presentation Layer (UI + Intent + State)
        ↓
Domain Layer (Use Cases + Entities)
        ↓
Data Layer (Repositories + APIs + Database)
```

### Key Architectural Decisions

- **Single Activity** - Compose navigation for all screens
- **Hilt DI** - Compile-time safe dependency injection
- **MVI Pattern** - Unidirectional data flow (Intent → State → UI)
- **Repository Pattern** - Abstract data sources
- **Use Case Pattern** - Single-purpose business logic

See [system-architecture.md](docs/system-architecture.md) for detailed diagrams.

## Development Workflow

### Code Standards

- **File Size:** Max 200 lines per file
- **Function Length:** Prefer <30 lines
- **Naming:** PascalCase for classes, camelCase for functions
- **Architecture:** Follow MVI + Clean Architecture layers
- **Testing:** >80% test coverage target

See [code-standards.md](docs/code-standards.md) for complete guidelines.

### Git Workflow

```bash
# Feature branch
git checkout -b feature/trading-flow

# Commit with conventional format
git commit -m "[feat] Add token trading screen"

# Push and create PR
git push origin feature/trading-flow
```

**Commit Types:** `[feat]`, `[fix]`, `[refactor]`, `[docs]`, `[test]`, `[chore]`

## Key Features Implementation Status

| Feature | Status | Notes |
|---------|--------|-------|
| Project Setup | ✅ Complete | All dependencies configured |
| UI Theme | ✅ Complete | Material3 theme ready |
| Architecture | 🚧 Planned | MVVM + Clean layers |
| Privy Integration | ⏳ Planned | Wallet + social auth |
| Kyber API | ⏳ Planned | Trade execution |
| Auto-Sell | ⏳ Planned | WorkManager scheduled tasks |
| Biometric Auth | ⏳ Planned | Device authentication |
| QR Funding | ⏳ Planned | Deposit via QR codes |

**Legend:** ✅ Complete | 🚧 In Progress | ⏳ Planned

## Performance Targets

| Metric | Target | Stretch Goal | Current |
|--------|--------|--------------|---------|
| Cold Start | <800ms | <500ms | ~1.2s |
| Wallet Init | 2-3s | <2s | TBD |
| Trade Execution | 3-5s | <3s | TBD |
| **Total (Download → Trade)** | **<30s** | **<15s** | **TBD** |

## Challenge Requirements

Built for the **Kyber Flash Trade Challenge**:

- ✅ Mobile-first Android app
- ⏳ All trades via Kyber Aggregator API
- ⏳ Auto-sell after ~24 hours
- ⏳ MEV-protected swaps
- ⏳ Download to trade in 15-30 seconds

**Rewards:**
- $1,000 base for MVC completion
- $5,000 bonus for top builder
- **Total: $6,000 potential**

## Contributing

This is a challenge submission project. Contributions are not currently accepted, but feedback is welcome!

## Documentation

- [Project Overview & PDR](docs/project-overview-pdr.md) - Goals, requirements, timeline
- [Codebase Summary](docs/codebase-summary.md) - Project structure and status
- [Code Standards](docs/code-standards.md) - Coding guidelines
- [System Architecture](docs/system-architecture.md) - Architecture diagrams and patterns

## Resources

- **Kyber Aggregator API:** [Documentation](https://docs.kyberswap.com/)
- **Privy SDK:** [Documentation](https://docs.privy.io/)
- **Ethers.kt:** [GitHub](https://github.com/Kr1ptal/ethers-kt)
- **Jetpack Compose:** [Guide](https://developer.android.com/jetpack/compose)

## License

This project is built for the Kyber Flash Trade Challenge. License TBD.

## Contact

**Developer:** Otis Tran
**Project:** Flash Trade Challenge Submission
**Timeline:** 4 weeks (January 2025)

---

**Status:** 🚧 In Development (Week 1 of 4)
**Completion:** ~10% (Foundation phase)

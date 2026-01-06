# Flash Trade

**One-Click Cryptocurrency Trading for Android**

A mobile-first application for rapid meme token trading. Go from app download to first trade in under 15 seconds.

## Features

- **Instant Onboarding** - Google OAuth via Privy embedded wallet
- **Portfolio View** - Token holdings with QR deposit address
- **Token Discovery** - Paginated token list with search and Room-backed cache
- **DEX Swap** - Kyber Aggregator integration with EIP-2612 permits, simulation, and MEV protection
- **Auto-Sell** - Scheduled token sales via WorkManager background jobs
- **Transaction History** - Etherscan V2 API integration for full activity view

## The 3-Tap Flow

```
1. Sign Up (~3s)     → Social login with auto wallet creation
2. Add Funds (3-10s) → QR deposit or fiat on-ramp
3. Buy Token (~5s)   → One-tap purchase with auto-sell scheduled
```

**Total: 15-30 seconds from download to first trade**

## Tech Stack

**Core**
- Kotlin `2.2.21` · Android SDK `28-36`

**UI**
- Jetpack Compose `1.10.0` · Material3 `1.4.0` · Coil `3.3.0`

**Architecture**
- MVI + Clean Architecture · Hilt `2.57.2`

**Networking**
- Retrofit `3.0.0` · Moshi `1.15.2` · OkHttp `5.3.2`

**Web3**
- Privy SDK `0.9.0` · Web3j `4.12.3`

**Storage**
- Room `2.8.4` · DataStore `1.2.0` · Paging 3 `3.3.6`

**Background**
- WorkManager `2.11.0`

**Camera & QR**
- CameraX `1.5.2` · ML Kit `17.3.0` · ZXing `3.5.4`

**Utilities**
- Timber `5.0.1` · Kotlinx Serialization `1.9.0`

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                 Presentation Layer                  │
│         (Compose UI + ViewModel + MVI)              │
├─────────────────────────────────────────────────────┤
│                   Domain Layer                      │
│       (Use Cases + Models + Repository Interfaces)  │
├─────────────────────────────────────────────────────┤
│                    Data Layer                       │
│      (Repository Impl + API + Room + DataStore)     │
└─────────────────────────────────────────────────────┘
```

### Key Patterns

- **MVI** - Unidirectional data flow (Event → State → Effect)
- **Repository** - Abstract data sources behind interfaces
- **Use Case** - Single-purpose business logic with step composition
- **Cache-First** - Room-backed token cache with RemoteMediator
- **EIP-2612 Gasless Approvals** - Permit signatures for supported tokens

## APIs

| API | Purpose |
|-----|---------|
| Kyber Aggregator | Swap routes, quote building, optimal routing |
| Etherscan V2 | Transaction history, token balances, account data |
| Alchemy Prices | Real-time token prices |
| Privy | Wallet creation, social auth, transaction signing |

## License

MIT License - See [LICENSE](LICENSE) for details.

## Acknowledgments

- [Kyber Network](https://kyberswap.com/) - Aggregator API
- [Privy](https://privy.io/) - Embedded wallet SDK
- [Etherscan](https://etherscan.io/) - Blockchain explorer API
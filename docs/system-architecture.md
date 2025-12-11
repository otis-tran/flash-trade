# Flash Trade - System Architecture

## Overview

Flash Trade uses **MVI (Model-View-Intent)** pattern combined with Clean Architecture principles to ensure unidirectional data flow, separation of concerns, testability, and maintainability.

## Technical Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Platform** | Android (Kotlin) | Mobile-first trading app |
| **Architecture** | MVI + Clean Architecture | Unidirectional data flow |
| **Wallet** | Privy TEE | Social login, embedded wallet ([privy.io](https://www.privy.io/)) |
| **Swap** | Kyber Aggregator | Multi-chain, efficient routing, MEV-safe |
| **Auto-sell** | WorkManager | Background executor for scheduled sells |

## Architecture Layers

```
┌─────────────────────────────────────────────────────┐
│              PRESENTATION LAYER                     │
│  (UI Components, Intents, States, Reducers)         │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  Composables│  │   Reducers   │  │  States   │ │
│  └─────────────┘  └──────────────┘  └───────────┘ │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│               DOMAIN LAYER                          │
│  (Business Logic, Use Cases, Entities)              │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  Use Cases  │  │  Entities    │  │Repository │ │
│  │             │  │              │  │Interfaces │ │
│  └─────────────┘  └──────────────┘  └───────────┘ │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│                DATA LAYER                           │
│  (Repositories, Data Sources, APIs)                 │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │Repositories │  │  Remote APIs │  │  Local DB │ │
│  │(Impl)       │  │              │  │           │ │
│  └─────────────┘  └──────────────┘  └───────────┘ │
└─────────────────────────────────────────────────────┘
```

## Layer Details

### Presentation Layer

**Responsibility:** User interface, intent handling, state management

**MVI Components:**
- **Composables:** UI components built with Jetpack Compose
- **Intent:** User actions/events (sealed class)
- **State:** Immutable UI state (data class)
- **Reducer:** Pure function (State + Intent) → State
- **Side Effects:** One-time events (navigation, toasts)

**Dependencies:**
- Depends on: Domain layer (use cases, entities)
- Depends on: None (pure UI logic)

**Package Structure:**
```
presentation/
├── onboarding/
│   ├── OnboardingScreen.kt
│   ├── OnboardingIntent.kt
│   ├── OnboardingState.kt
│   └── OnboardingReducer.kt
├── trading/
│   ├── TradingScreen.kt
│   ├── TradingIntent.kt
│   ├── TradingState.kt
│   └── TradingReducer.kt
├── portfolio/
│   ├── PortfolioScreen.kt
│   ├── PortfolioIntent.kt
│   ├── PortfolioState.kt
│   └── PortfolioReducer.kt
├── navigation/
│   └── NavGraph.kt
└── common/
    ├── LoadingIndicator.kt
    └── ErrorMessage.kt
```

### Domain Layer

**Responsibility:** Core business logic (platform-independent)

**Components:**
- **Entities:** Core business models (Token, Trade, Wallet)
- **Use Cases:** Single-purpose business operations
- **Repository Interfaces:** Contracts for data access

**Dependencies:**
- Depends on: Nothing (pure Kotlin)
- No Android/framework dependencies

**Package Structure:**
```
domain/
├── model/
│   ├── Token.kt
│   ├── Trade.kt
│   ├── Wallet.kt
│   └── User.kt
├── repository/
│   ├── TradeRepository.kt
│   ├── WalletRepository.kt
│   └── UserRepository.kt
└── usecase/
    ├── ExecuteTradeUseCase.kt
    ├── ScheduleAutoSellUseCase.kt
    ├── GetTokenPriceUseCase.kt
    └── CreateWalletUseCase.kt
```

### Data Layer

**Responsibility:** Data management and external communication

**Components:**
- **Repository Implementations:** Implement domain interfaces
- **Remote Data Sources:** API clients (Kyber, Privy)
- **Local Data Sources:** Database (Room) and preferences (DataStore)
- **DTOs:** Data transfer objects for API/DB

**Dependencies:**
- Depends on: Domain layer (implements interfaces)
- Uses: Retrofit, Room, DataStore

**Package Structure:**
```
data/
├── remote/
│   ├── kyber/
│   │   ├── KyberApiService.kt
│   │   ├── dto/
│   │   │   ├── TradeRequestDto.kt
│   │   │   └── TradeResponseDto.kt
│   │   └── mapper/
│   │       └── KyberMapper.kt
│   └── privy/
│       └── PrivyClient.kt
├── local/
│   ├── dao/
│   │   ├── TradeDao.kt
│   │   └── WalletDao.kt
│   ├── entity/
│   │   ├── TradeEntity.kt
│   │   └── WalletEntity.kt
│   ├── database/
│   │   └── FlashTradeDatabase.kt
│   └── datastore/
│       └── UserPreferences.kt
└── repository/
    ├── TradeRepositoryImpl.kt
    ├── WalletRepositoryImpl.kt
    └── UserRepositoryImpl.kt
```

## Component Flow Diagram

### Trade Execution Flow

```
User Tap "Buy"
      │
      ▼
[TradingScreen] ──────────┐
      │                   │
      │ onClick           │ Observes state
      ▼                   │
[TradingViewModel] ◄──────┘
      │
      │ calls
      ▼
[ExecuteTradeUseCase]
      │
      │ calls
      ▼
[TradeRepository Interface]
      │
      │ implements
      ▼
[TradeRepositoryImpl]
      │
      ├─────────────┬───────────────┐
      │             │               │
      ▼             ▼               ▼
[KyberApiService] [TradeDao]  [WorkManager]
      │             │               │
      ▼             ▼               ▼
  Kyber API    Local DB      Schedule Auto-Sell
```

### MVI Data Flow Example

```kotlin
// 1. User taps buy button → Intent
TradingScreen.kt:
  Button(onClick = { onIntent(TradingIntent.ExecuteTrade(token, amount)) })

// 2. Intent sealed class
TradingIntent.kt:
  sealed class TradingIntent {
    data class ExecuteTrade(val token: Token, val amount: Double) : TradingIntent()
    data class SelectToken(val token: Token) : TradingIntent()
    object RefreshPrices : TradingIntent()
  }

// 3. State data class (immutable)
TradingState.kt:
  data class TradingState(
    val isLoading: Boolean = false,
    val selectedToken: Token? = null,
    val error: String? = null,
    val recentTrades: List<Trade> = emptyList()
  )

// 4. Reducer processes intent → new state
TradingReducer.kt:
  fun reduce(state: TradingState, intent: TradingIntent): TradingState {
    return when (intent) {
      is TradingIntent.ExecuteTrade -> state.copy(isLoading = true)
      is TradingIntent.SelectToken -> state.copy(selectedToken = intent.token)
      // ...
    }
  }

// 5. Use case executes business logic
ExecuteTradeUseCase.kt:
  suspend operator fun invoke(token: Token, amount: Double): Result<Trade> {
    return tradeRepository.executeTrade(token, amount)
  }

// 6. Repository coordinates data sources (Kyber Aggregator)
TradeRepositoryImpl.kt:
  override suspend fun executeTrade(token: Token, amount: Double): Result<Trade> {
    return try {
      // Call Kyber Aggregator API (MEV-safe, efficient routing)
      val response = kyberApi.executeTrade(request)
      val trade = mapper.toDomain(response)

      // Save to database
      tradeDao.insert(trade.toEntity())

      // Schedule auto-sell via WorkManager
      scheduleAutoSell(trade)

      Result.Success(trade)
    } catch (e: Exception) {
      Result.Error(e)
    }
  }
```

## Dependency Injection Architecture

### Hilt Component Hierarchy

```
SingletonComponent (Application scope)
    │
    ├── NetworkModule (Retrofit, APIs)
    ├── DatabaseModule (Room, DataStore)
    └── RepositoryModule (Repository implementations)

ActivityRetainedComponent (ViewModel scope)
    │
    └── UseCaseModule (Use cases)

ActivityComponent (Activity scope)
    │
    └── MainActivity
```

### Module Structure

```kotlin
// NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(KYBER_API_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideKyberApiService(retrofit: Retrofit): KyberApiService =
        retrofit.create(KyberApiService::class.java)
}

// DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlashTradeDatabase =
        Room.databaseBuilder(
            context,
            FlashTradeDatabase::class.java,
            "flash_trade_db"
        ).build()

    @Provides
    fun provideTradeDao(database: FlashTradeDatabase): TradeDao =
        database.tradeDao()
}

// RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTradeRepository(
        impl: TradeRepositoryImpl
    ): TradeRepository
}
```

## External Integration Architecture

### Kyber Aggregator API

Kyber Aggregator provides multi-chain swaps with efficient routing and MEV protection.
Documentation: [docs.kyberswap.com](https://docs.kyberswap.com/)

```
[App] ──────► [KyberApiService] ──────► [Kyber Aggregator]
                     │
                     ├─ GET /route - Get optimal swap route
                     ├─ POST /swap - Execute swap (MEV-safe)
                     ├─ GET /tokens - Token list
                     └─ GET /prices - Real-time prices

Key Features:
- Multi-chain support (any Kyber-supported chain)
- Efficient routing (best price across DEXs)
- MEV protection (built-in)
- Pre-compiled ABI for instant execution

Request Flow:
1. Fetch optimal route via Kyber Aggregator
2. Sign transaction with Ethers.kt + Privy TEE
3. Submit MEV-protected swap
4. Poll for transaction confirmation
5. Update local database
```

**API Endpoints:**
- `GET /ethereum/api/v1/tokens` - Token list with filters
- `GET /ethereum/route` - Get swap route
- `POST /ethereum/swap` - Execute swap

### Privy TEE Integration

Privy TEE (Trusted Execution Environment) provides secure embedded wallets with social login.
Documentation: [privy.io](https://www.privy.io/)

```
[App] ──────► [PrivyClient] ──────► [Privy TEE]
                     │
                     ├─ Initialize wallet (zero cold start)
                     ├─ Social login (Google, Apple, Email)
                     ├─ Create embedded wallet (auto-generated)
                     └─ Sign transactions (TEE-secured)

Integration Points:
1. App startup: Initialize Privy SDK (parallel with UI for zero cold start)
2. Onboarding: Social login + auto wallet creation (~3s)
3. Trading: Transaction signing via TEE
4. Settings: Wallet management
```

### Ethers.kt Integration

```
[App] ──────► [WalletManager] ──────► [Ethers.kt]
                     │
                     ├─ Create wallet
                     ├─ Sign transactions
                     ├─ Estimate gas
                     └─ Send transactions

Usage:
- Wallet creation during onboarding
- Transaction signing before API calls
- Gas estimation for fee display
```

## Navigation Architecture

### Single Activity Architecture

```
MainActivity
    │
    └── NavHost (Compose Navigation)
            │
            ├── OnboardingGraph
            │     ├── WelcomeScreen
            │     ├── LoginScreen
            │     └── WalletSetupScreen
            │
            ├── MainGraph
            │     ├── TradingScreen (Start destination)
            │     ├── PortfolioScreen
            │     └── SettingsScreen
            │
            └── TradeDetailsScreen
```

**Navigation Routes:**
```kotlin
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Trading : Screen("trading")
    object Portfolio : Screen("portfolio")
    object Settings : Screen("settings")
    object TradeDetails : Screen("trade/{tradeId}") {
        fun createRoute(tradeId: String) = "trade/$tradeId"
    }
}
```

## Background Processing Architecture

### WorkManager for Auto-Sell

```
[ScheduleAutoSellUseCase]
        │
        ▼
[WorkManager] ──────► [AutoSellWorker]
        │                     │
        │                     ├─ Check time elapsed
        │                     ├─ Execute sell via repository
        │                     ├─ Update database
        │                     └─ Send notification
        │
        └── Constraints:
            ├─ Network connected
            └─ Battery not low
```

**Worker Implementation:**
```kotlin
class AutoSellWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tradeId = inputData.getString("tradeId") ?: return Result.failure()

        return try {
            repository.executeSell(tradeId)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry()
            else Result.failure()
        }
    }
}
```

## Database Architecture

### Room Database Schema

```
┌─────────────────┐    ┌─────────────────┐
│  TradeEntity    │    │  WalletEntity   │
├─────────────────┤    ├─────────────────┤
│ id: String (PK) │    │ address: String │
│ tokenSymbol     │    │ balance: Double │
│ amount: Double  │    │ chainId: Int    │
│ buyPrice        │    │ createdAt: Long │
│ sellPrice?      │    └─────────────────┘
│ status: String  │              │
│ autoSellTime    │              │ 1
│ createdAt: Long │              │
│ walletId: (FK)──┼──────────────┘
└─────────────────┘        N

┌─────────────────┐
│  UserEntity     │
├─────────────────┤
│ id: String (PK) │
│ email: String   │
│ displayName     │
│ avatarUrl       │
│ createdAt: Long │
└─────────────────┘
```

**Database Relations:**
- One User → Many Wallets
- One Wallet → Many Trades

### DataStore for Preferences

```kotlin
data class UserPreferences(
    val isOnboardingComplete: Boolean = false,
    val biometricEnabled: Boolean = false,
    val selectedChainId: Int = 1, // Ethereum mainnet
    val autoSellEnabled: Boolean = true,
    val autoSellDelayHours: Int = 24
)
```

## State Management Architecture

### ViewModel State Pattern

```kotlin
// UI State
data class TradingUiState(
    val isLoading: Boolean = false,
    val selectedToken: Token? = null,
    val tokenPrice: Double = 0.0,
    val userBalance: Double = 0.0,
    val recentTrades: List<Trade> = emptyList(),
    val error: String? = null
)

// ViewModel
@HiltViewModel
class TradingViewModel @Inject constructor(
    private val executeTradeUseCase: ExecuteTradeUseCase,
    private val getTokenPriceUseCase: GetTokenPriceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()

    fun executeTrade(token: Token, amount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = executeTradeUseCase(token, amount)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recentTrades = listOf(result.data) + it.recentTrades
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }
}
```

## Security Architecture

### Key Storage Strategy

```
Private Keys ──────► Android Keystore
                            │
                            ├─ Hardware-backed (if available)
                            ├─ Encrypted at rest
                            └─ Biometric-protected

API Keys ──────► BuildConfig (obfuscated)
                     │
                     └─ ProGuard obfuscation

User Tokens ──────► Encrypted DataStore
                            │
                            └─ EncryptedSharedPreferences fallback
```

### Authentication Flow

```
App Launch
    │
    ├─ Has valid session? ──No──► Show login
    │                              │
    └─ Yes                         ▼
        │                    Social login / Passkey
        ▼                          │
    Biometric prompt               ▼
        │                    Create wallet (Privy)
        ├─ Success ───►             │
        │                          ▼
        └─ Fail ──────►      Save session token
                             │
                             ▼
                       Navigate to trading
```

## Performance Optimization Architecture

### Parallel Initialization

```
App Start
    │
    ├────────────┬────────────┬────────────┐
    │            │            │            │
    ▼            ▼            ▼            ▼
Compose UI   Privy SDK   Database   Pre-fetch Tokens
    │            │            │            │
    └────────────┴────────────┴────────────┘
                 │
                 ▼
            App Ready
         (Cold start <800ms)
```

### Data Caching Strategy

```
Token Prices ──────► Cache (5 min TTL)
                            │
                            ├─ In-memory cache
                            └─ Disk cache (Room)

User Balance ──────► Cache (30 sec TTL)

Trade History ──────► Local DB (primary)
                            │
                            └─ Sync with server on demand
```

## Error Handling Architecture

### Error Hierarchy

```kotlin
sealed class AppError {
    data class NetworkError(val message: String) : AppError()
    data class ApiError(val code: Int, val message: String) : AppError()
    data class DatabaseError(val cause: Throwable) : AppError()
    data class WalletError(val reason: String) : AppError()
    object UnknownError : AppError()
}

// Repository layer catches and maps exceptions
// ViewModel exposes user-friendly error messages
// UI displays appropriate error states
```

## Scalability Considerations

### Horizontal Scalability
- Stateless API clients (can add more servers)
- Local-first data strategy (reduces server load)
- Efficient pagination for large lists

### Vertical Scalability
- Lazy loading for heavy screens
- Efficient database queries with indexes
- Image caching and compression

## Future Architecture Enhancements

1. **Multi-module structure** - Split into feature modules
2. **KMP (Kotlin Multiplatform)** - Share domain layer with iOS
3. **GraphQL** - Replace REST if API supports it
4. **Offline-first sync** - Robust offline support
5. **Analytics integration** - Firebase/Mixpanel for insights

## Architecture Diagrams Legend

```
─────►  Data flow
┌────┐  Component
│    │
└────┘
═════►  Dependency
(PK)    Primary Key
(FK)    Foreign Key
```

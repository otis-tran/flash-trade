# Flash Trade - Code Standards

## Overview

This document defines coding standards and best practices for the Flash Trade project. All code must adhere to these guidelines to ensure consistency, maintainability, and quality.

## General Principles

### YAGNI (You Aren't Gonna Need It)
- Implement only what's required now
- Avoid speculative features
- Keep scope focused on challenge requirements

### KISS (Keep It Simple, Stupid)
- Prefer simple solutions over complex ones
- Avoid over-engineering
- Clear code > clever code

### DRY (Don't Repeat Yourself)
- Extract common logic into utilities
- Use extension functions for repeated operations
- Create reusable components

## Kotlin Standards

### Naming Conventions

**Classes & Interfaces** - PascalCase
```kotlin
class TradeViewModel
interface KyberRepository
data class TokenEntity
sealed class TradeState
```

**Functions & Variables** - camelCase
```kotlin
fun executeTrade()
val currentPrice: Double
var isLoading: Boolean
private val repository: KyberRepository
```

**Constants** - SCREAMING_SNAKE_CASE
```kotlin
const val MAX_RETRY_ATTEMPTS = 3
const val API_TIMEOUT_SECONDS = 30L
private const val TAG = "TradeViewModel"
```

**Package Names** - snake_case (per Android convention)
```kotlin
package com.otistran.flash_trade
package com.otistran.flash_trade.domain.usecase
```

### File Naming

**Source Files** - PascalCase matching class name
```
TradeViewModel.kt
KyberApiService.kt
TokenEntity.kt
```

**Composable Files** - PascalCase + "Screen" or "Component" suffix
```
OnboardingScreen.kt
TradingScreen.kt
TokenCardComponent.kt
```

**Test Files** - Class name + "Test" suffix
```
TradeViewModelTest.kt
KyberRepositoryTest.kt
```

### File Size Limits

**Hard Limit:** 200 lines per file
- Includes imports, comments, blank lines
- If exceeded, split into logical components
- Extract nested classes into separate files

**Function Length:** <30 lines preferred
- Complex functions should be split
- Use helper functions for clarity

### Code Organization

**File Structure Order:**
1. Package declaration
2. Imports (grouped and sorted)
3. File-level constants
4. Class declaration
5. Companion object (if any)
6. Properties
7. Init blocks
8. Public functions
9. Private functions

**Example:**
```kotlin
package com.otistran.flash_trade.presentation.trading

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val TAG = "TradeViewModel"

@HiltViewModel
class TradeViewModel @Inject constructor(
    private val repository: TradeRepository
) : ViewModel() {

    companion object {
        private const val AUTO_SELL_DELAY_MS = 24 * 60 * 60 * 1000L
    }

    // Properties
    private val _state = MutableStateFlow<TradeState>(TradeState.Idle)
    val state: StateFlow<TradeState> = _state.asStateFlow()

    // Public functions
    fun executeTrade(token: Token, amount: Double) {
        // Implementation
    }

    // Private functions
    private fun scheduleAutoSell(tradeId: String) {
        // Implementation
    }
}
```

## Architecture Standards

### MVI + Clean Architecture

**Layer Separation:**
```
Presentation → Domain → Data
(UI/Intents/States) → (Use Cases) → (Repositories)
```

**MVI Pattern (Unidirectional Data Flow):**
```
User Action → Intent → Reducer → State → UI
```

**Rules:**
- Presentation depends on Domain
- Domain depends on nothing (pure Kotlin)
- Data depends on Domain (implements interfaces)
- No circular dependencies

### Package Structure Per Feature

```
feature_name/
├── domain/
│   ├── model/           # Domain entities
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Use cases
├── data/
│   ├── remote/          # API DTOs & services
│   ├── local/           # Database entities & DAOs
│   └── repository/      # Repository implementations
└── presentation/
    ├── FeatureScreen.kt   # Composable screen
    ├── FeatureIntent.kt   # User intents (sealed class)
    ├── FeatureState.kt    # UI state (data class)
    └── FeatureReducer.kt  # State reducer
```

### Dependency Injection with Hilt

**Annotations:**
- `@HiltAndroidApp` on Application class
- `@AndroidEntryPoint` on Activity/Fragment
- `@HiltViewModel` on ViewModels
- `@Inject` for constructor injection
- `@Module` + `@InstallIn` for modules

**Example Module:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideKyberApiService(
        retrofit: Retrofit
    ): KyberApiService = retrofit.create(KyberApiService::class.java)
}
```

## Jetpack Compose Standards

### Composable Naming

**Screens** - `[Feature]Screen`
```kotlin
@Composable
fun TradingScreen(
    viewModel: TradeViewModel = hiltViewModel()
) { ... }
```

**Components** - Descriptive noun
```kotlin
@Composable
fun TokenCard(
    token: Token,
    onTokenClick: (Token) -> Unit
) { ... }
```

**Modifiers** - As parameter (not receiver)
```kotlin
@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // Parameter, not receiver
) {
    Button(
        onClick = onClick,
        modifier = modifier // Apply here
    ) { ... }
}
```

### State Management (MVI Pattern)

**Intent (User Actions):**
```kotlin
sealed class TradingIntent {
    data class SelectToken(val token: Token) : TradingIntent()
    data class ExecuteTrade(val amount: Double) : TradingIntent()
    object RefreshPrices : TradingIntent()
}
```

**State (Immutable):**
```kotlin
data class TradingState(
    val isLoading: Boolean = false,
    val token: Token? = null,
    val price: Double = 0.0,
    val error: String? = null
)
```

**Reducer (Pure Function):**
```kotlin
fun reduce(state: TradingState, intent: TradingIntent): TradingState {
    return when (intent) {
        is TradingIntent.SelectToken -> state.copy(token = intent.token)
        is TradingIntent.ExecuteTrade -> state.copy(isLoading = true)
        TradingIntent.RefreshPrices -> state.copy(isLoading = true)
    }
}
```

**Composable (Observes State, Emits Intents):**
```kotlin
@Composable
fun TradingScreen(
    state: TradingState,
    onIntent: (TradingIntent) -> Unit
) {
    when {
        state.isLoading -> LoadingIndicator()
        state.error != null -> ErrorMessage(state.error)
        else -> TradingContent(state.token, state.price, onIntent)
    }
}
```

### Composition Best Practices

**Single Responsibility:**
```kotlin
// ❌ Bad - too much in one composable
@Composable
fun TradeScreen() {
    // 200 lines of UI logic
}

// ✅ Good - split into components
@Composable
fun TradeScreen() {
    Column {
        TradeHeader()
        TokenSelector()
        AmountInput()
        TradeButton()
    }
}
```

**Avoid Remember with Complex Logic:**
```kotlin
// ❌ Bad
@Composable
fun Screen() {
    val data = remember { complexCalculation() }
}

// ✅ Good - move to state/reducer
@Composable
fun Screen(state: ScreenState, onIntent: (ScreenIntent) -> Unit) {
    // State already computed, just render
}
```

## Testing Standards

### Test Coverage Target
- **Overall:** >80% coverage
- **Reducers:** 100% coverage (pure functions, easy to test)
- **Use Cases:** 100% coverage
- **Repositories:** >90% coverage
- **UI:** Critical flows tested

### Test Naming Convention
```kotlin
// Pattern: should[ExpectedBehavior]When[StateUnderTest]
@Test
fun shouldReturnSuccessWhenTradeExecutes() { }

@Test
fun shouldShowErrorWhenNetworkFails() { }
```

### Test Structure (Given-When-Then)
```kotlin
@Test
fun shouldExecuteTradeSuccessfully() {
    // Given
    val token = Token(id = "eth", symbol = "ETH")
    val amount = 1.0
    coEvery { repository.executeTrade(any(), any()) } returns Result.success(trade)

    // When
    viewModel.executeTrade(token, amount)

    // Then
    val state = viewModel.state.value
    assertTrue(state is TradeState.Success)
    assertEquals(trade, state.trade)
}
```

### Mocking with MockK
```kotlin
class TradeViewModelTest {
    @MockK
    private lateinit var repository: TradeRepository

    private lateinit var viewModel: TradeViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = TradeViewModel(repository)
    }
}
```

## Error Handling

### Result Pattern
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

// Usage
suspend fun executeTrade(token: Token): Result<Trade> {
    return try {
        val trade = api.executeTrade(token)
        Result.Success(trade)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

### Error States in UI
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### Logging Standards
```kotlin
// Use Timber or similar
Timber.d("Trade executed: $tradeId")
Timber.e(exception, "Failed to execute trade")

// Never log sensitive data
// ❌ Bad
Timber.d("Private key: $privateKey")

// ✅ Good
Timber.d("Wallet address: ${wallet.address}")
```

## Security Standards

### Sensitive Data Handling

**Never:**
- Log private keys or mnemonics
- Store keys in SharedPreferences
- Hardcode API keys
- Commit secrets to version control

**Always:**
- Use Android Keystore for keys
- Encrypt sensitive local data
- Use HTTPS for all network calls
- Implement certificate pinning

### Biometric Authentication
```kotlin
// Use BiometricPrompt API
val promptInfo = BiometricPrompt.PromptInfo.Builder()
    .setTitle("Authenticate")
    .setSubtitle("Confirm transaction")
    .setNegativeButtonText("Cancel")
    .build()

biometricPrompt.authenticate(promptInfo)
```

## Performance Standards

### Coroutines Best Practices
```kotlin
// ✅ Use appropriate dispatchers
viewModelScope.launch(Dispatchers.IO) {
    val result = repository.fetchData()
    withContext(Dispatchers.Main) {
        updateUi(result)
    }
}

// ✅ Cancel when not needed
private val job = Job()
private val scope = CoroutineScope(Dispatchers.Main + job)

override fun onDestroy() {
    job.cancel()
    super.onDestroy()
}
```

### Database Optimization
```kotlin
// ✅ Use suspend functions
@Query("SELECT * FROM trades")
suspend fun getAllTrades(): List<TradeEntity>

// ✅ Use Flow for reactive queries
@Query("SELECT * FROM trades")
fun observeTrades(): Flow<List<TradeEntity>>

// ✅ Use transactions for multiple writes
@Transaction
suspend fun updateTradesAndBalance(trades: List<Trade>, balance: Balance) {
    insertTrades(trades)
    updateBalance(balance)
}
```

### Image Loading
```kotlin
// Use Coil for efficient image loading
AsyncImage(
    model = imageUrl,
    contentDescription = null,
    modifier = Modifier.size(48.dp)
)
```

## Documentation Standards

### KDoc for Public APIs
```kotlin
/**
 * Executes a token trade through the Kyber Aggregator API.
 *
 * @param token The token to trade
 * @param amount The amount to trade in USD
 * @return Result containing the executed trade or error
 * @throws NetworkException if network request fails
 */
suspend fun executeTrade(token: Token, amount: Double): Result<Trade>
```

### Inline Comments
```kotlin
// Only when logic is non-obvious
// Calculate MEV protection threshold based on gas price
val mevThreshold = gasPrice * MEV_MULTIPLIER
```

## Git Commit Standards

### Commit Message Format
```
[type] Brief description

Detailed explanation (if needed)

- Bullet points for multiple changes
```

**Types:**
- `[feat]` - New feature
- `[fix]` - Bug fix
- `[refactor]` - Code refactoring
- `[docs]` - Documentation
- `[test]` - Tests
- `[chore]` - Build/config changes

**Example:**
```
[feat] Add Kyber API integration

Implement KyberApiService with:
- Trade execution endpoint
- Price quote endpoint
- MEV protection
```

## Code Review Checklist

Before submitting code for review:
- [ ] Follows naming conventions
- [ ] Files under 200 lines
- [ ] Functions under 30 lines
- [ ] No hardcoded strings (use resources)
- [ ] Error handling implemented
- [ ] Tests written and passing
- [ ] No TODO comments without tickets
- [ ] Documentation updated
- [ ] No sensitive data exposed
- [ ] Follows MVI architecture (Intent → State → UI)

## IDE Configuration

### Android Studio Settings
- Code style: Kotlin official
- Formatter: Enable on save
- Optimize imports: On save
- Line length: 120 characters

### Useful Plugins
- Kotlin Fill Class (auto-generate properties)
- Rainbow Brackets
- SonarLint (code quality)

## Additional Resources

- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Jetpack Compose Guidelines](https://developer.android.com/jetpack/compose/guidelines)

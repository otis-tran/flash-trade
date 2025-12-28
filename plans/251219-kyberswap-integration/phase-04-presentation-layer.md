# Phase 4: Presentation Layer Implementation

**Duration:** 60 minutes
**Dependencies:** Phase 2 (Domain), Phase 3 (Data)
**Risk Level:** Medium

---

## Context Links

- **Existing Screen:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingScreen.kt`
- **Existing ViewModel:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingViewModel.kt`
- **Base ViewModel:** `app/src/main/java/com/otistran/flash_trade/core/base/BaseViewModel.kt`
- **MVI Pattern:** `app/src/main/java/com/otistran/flash_trade/core/base/`

---

## Overview

Create SwapScreen with token input, amount selector, quote display, and swap confirmation. Implement SwapViewModel with MVI pattern for state management. Add auto-refresh for quotes (5s interval). Handle all user interactions and error states.

---

## Key Insights

1. **Existing MVI Pattern:**
   - BaseViewModel handles state/event/effect
   - State = immutable data class
   - Event = sealed class for user actions
   - Effect = one-time events (navigation, toasts)

2. **UI Components:**
   - Token selector (from/to)
   - Amount input with balance display
   - Quote card (rate, gas, slippage)
   - Swap button with loading state
   - Error banner

3. **Auto-Refresh Logic:**
   - Launch coroutine on quote received
   - Delay 5s, fetch new quote
   - Cancel on screen exit or new input
   - Show subtle refresh indicator

4. **Performance:**
   - Debounce amount input (500ms)
   - Cache last quote while typing
   - Show stale quote with warning

---

## Requirements

### Functional
- Display token pair (from → to)
- Amount input with max button
- Real-time quote display
- Auto-refresh every 5s
- Swap confirmation dialog
- Transaction status tracking

### Non-Functional
- Responsive UI (60fps)
- Accessibility labels
- Error recovery
- Loading states
- Input validation

---

## Architecture

```
SwapScreen (Composable)
    ├── TokenSelector (from/to)
    ├── AmountInput (with balance)
    ├── QuoteCard (rate, gas, slippage)
    ├── SwapButton (loading state)
    └── ErrorBanner

SwapViewModel (MVI)
    ├── SwapState (tokens, amount, quote, loading)
    ├── SwapEvent (SelectToken, SetAmount, FetchQuote, ExecuteSwap)
    └── SwapEffect (ShowToast, NavigateBack, ShowTxHash)

Auto-refresh:
    ├── Launch coroutine on quote received
    ├── Delay 5s
    └── Fetch new quote (if screen active)
```

---

## Related Code Files

**Existing (for reference):**
1. `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingScreen.kt`
2. `app/src/main/java/com/otistran/flash_trade/presentation/feature/trading/TradingViewModel.kt`
3. `app/src/main/java/com/otistran/flash_trade/core/base/BaseViewModel.kt`

**New files:**
4. `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapScreen.kt`
5. `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapViewModel.kt`
6. `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapState.kt`
7. `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapEvent.kt`
8. `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapEffect.kt`

---

## Implementation Steps

### Step 1: Create SwapState (8min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapState.kt`

```kotlin
package com.otistran.flash_trade.presentation.feature.swap

import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.model.Token

data class SwapState(
    val tokenFrom: Token? = null,
    val tokenTo: Token? = null,
    val amount: String = "",
    val quote: Quote? = null,
    val isLoadingQuote: Boolean = false,
    val isExecuting: Boolean = false,
    val error: String? = null,
    val txHash: String? = null,
    val quoteExpired: Boolean = false,
    val userBalance: String = "0.0" // In tokenFrom
) {
    val isValid: Boolean
        get() = tokenFrom != null && tokenTo != null && amount.isNotEmpty() && quote != null

    val displayRate: String
        get() {
            if (quote == null || tokenFrom == null || tokenTo == null) return "-"
            val rate = quote.amountOut.toDouble() / quote.amountIn.toDouble()
            return "1 ${tokenFrom.symbol} = $rate ${tokenTo.symbol}"
        }
}
```

### Step 2: Create SwapEvent (6min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapEvent.kt`

```kotlin
package com.otistran.flash_trade.presentation.feature.swap

import com.otistran.flash_trade.core.base.UiEvent
import com.otistran.flash_trade.domain.model.Token

sealed class SwapEvent : UiEvent {
    data class SelectTokenFrom(val token: Token) : SwapEvent()
    data class SelectTokenTo(val token: Token) : SwapEvent()
    data class SetAmount(val amount: String) : SwapEvent()
    object FetchQuote : SwapEvent()
    object ExecuteSwap : SwapEvent()
    object SwapTokens : SwapEvent() // Flip from/to
    object SetMaxAmount : SwapEvent()
    object DismissError : SwapEvent()
    object RefreshQuote : SwapEvent()
}
```

### Step 3: Create SwapEffect (5min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapEffect.kt`

```kotlin
package com.otistran.flash_trade.presentation.feature.swap

import com.otistran.flash_trade.core.base.UiEffect

sealed class SwapEffect : UiEffect {
    data class ShowToast(val message: String) : SwapEffect()
    data class NavigateToTxDetails(val txHash: String) : SwapEffect()
    object NavigateBack : SwapEffect()
    data class OpenTokenSelector(val isFrom: Boolean) : SwapEffect()
}
```

### Step 4: Create SwapViewModel (20min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapViewModel.kt`

```kotlin
package com.otistran.flash_trade.presentation.feature.swap

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.usecase.swap.ExecuteSwapUseCase
import com.otistran.flash_trade.domain.usecase.swap.GetSwapQuoteUseCase
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigInteger
import javax.inject.Inject

@HiltViewModel
class SwapViewModel @Inject constructor(
    private val getSwapQuoteUseCase: GetSwapQuoteUseCase,
    private val executeSwapUseCase: ExecuteSwapUseCase,
    private val privyAuthService: PrivyAuthService
) : BaseViewModel<SwapState, SwapEvent, SwapEffect>(
    initialState = SwapState()
) {

    private var quoteRefreshJob: Job? = null
    private var debounceJob: Job? = null

    override fun onEvent(event: SwapEvent) {
        when (event) {
            is SwapEvent.SelectTokenFrom -> selectTokenFrom(event.token)
            is SwapEvent.SelectTokenTo -> selectTokenTo(event.token)
            is SwapEvent.SetAmount -> setAmount(event.amount)
            SwapEvent.FetchQuote -> fetchQuote()
            SwapEvent.ExecuteSwap -> executeSwap()
            SwapEvent.SwapTokens -> swapTokens()
            SwapEvent.SetMaxAmount -> setMaxAmount()
            SwapEvent.DismissError -> setState { copy(error = null) }
            SwapEvent.RefreshQuote -> fetchQuote(forceRefresh = true)
        }
    }

    private fun selectTokenFrom(token: Token) {
        setState { copy(tokenFrom = token) }
        fetchQuoteDebounced()
    }

    private fun selectTokenTo(token: Token) {
        setState { copy(tokenTo = token) }
        fetchQuoteDebounced()
    }

    private fun setAmount(amount: String) {
        setState { copy(amount = amount) }
        fetchQuoteDebounced()
    }

    private fun swapTokens() {
        val currentState = state.value
        setState {
            copy(
                tokenFrom = currentState.tokenTo,
                tokenTo = currentState.tokenFrom,
                quote = null
            )
        }
        fetchQuoteDebounced()
    }

    private fun setMaxAmount() {
        val balance = state.value.userBalance
        setState { copy(amount = balance) }
        fetchQuoteDebounced()
    }

    private fun fetchQuoteDebounced() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(500) // Debounce 500ms
            fetchQuote()
        }
    }

    private fun fetchQuote(forceRefresh: Boolean = false) {
        val currentState = state.value
        if (currentState.tokenFrom == null || currentState.tokenTo == null) return
        if (currentState.amount.isEmpty() || currentState.amount.toDoubleOrNull() == null) return

        viewModelScope.launch {
            setState { copy(isLoadingQuote = true, error = null) }

            val amountWei = parseAmountToWei(
                currentState.amount,
                currentState.tokenFrom!!.decimals
            )

            val user = privyAuthService.getUser()
            val walletAddress = user?.embeddedEthereumWallets?.firstOrNull()?.address

            when (val result = getSwapQuoteUseCase(
                chain = "base",
                tokenIn = currentState.tokenFrom!!.address,
                tokenOut = currentState.tokenTo!!.address,
                amountIn = amountWei,
                userAddress = walletAddress
            )) {
                is Result.Success -> {
                    setState {
                        copy(
                            isLoadingQuote = false,
                            quote = result.data,
                            quoteExpired = false
                        )
                    }
                    startAutoRefresh()
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoadingQuote = false,
                            error = result.message
                        )
                    }
                }
                Result.Loading -> { /* Already loading */ }
            }
        }
    }

    private fun startAutoRefresh() {
        quoteRefreshJob?.cancel()
        quoteRefreshJob = viewModelScope.launch {
            delay(5000) // 5s
            setState { copy(quoteExpired = true) }
            fetchQuote(forceRefresh = true)
        }
    }

    private fun executeSwap() {
        val currentState = state.value
        if (!currentState.isValid) return

        viewModelScope.launch {
            setState { copy(isExecuting = true, error = null) }

            val user = privyAuthService.getUser()
            val walletAddress = user?.embeddedEthereumWallets?.firstOrNull()?.address
            if (walletAddress == null) {
                setState {
                    copy(
                        isExecuting = false,
                        error = "Wallet not found"
                    )
                }
                return@launch
            }

            when (val result = executeSwapUseCase(
                chain = "base",
                quote = currentState.quote!!,
                senderAddress = walletAddress
            )) {
                is Result.Success -> {
                    setState {
                        copy(
                            isExecuting = false,
                            txHash = result.data.txHash
                        )
                    }
                    setEffect(SwapEffect.ShowToast("Swap submitted!"))
                    setEffect(SwapEffect.NavigateToTxDetails(result.data.txHash))
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isExecuting = false,
                            error = result.message
                        )
                    }
                }
                Result.Loading -> { /* Already loading */ }
            }
        }
    }

    private fun parseAmountToWei(amount: String, decimals: Int): BigInteger {
        val value = amount.toDoubleOrNull() ?: 0.0
        val multiplier = BigInteger.TEN.pow(decimals)
        return (value * multiplier.toDouble()).toBigDecimal().toBigInteger()
    }

    override fun onCleared() {
        super.onCleared()
        quoteRefreshJob?.cancel()
        debounceJob?.cancel()
    }
}
```

### Step 5: Create SwapScreen (21min)

**File:** `app/src/main/java/com/otistran/flash_trade/presentation/feature/swap/SwapScreen.kt`

```kotlin
package com.otistran.flash_trade.presentation.feature.swap

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(
    viewModel: SwapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Swap") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // From Token
            TokenCard(
                label = "From",
                token = state.tokenFrom,
                amount = state.amount,
                balance = state.userBalance,
                onAmountChange = { viewModel.onEvent(SwapEvent.SetAmount(it)) },
                onMaxClick = { viewModel.onEvent(SwapEvent.SetMaxAmount) },
                onTokenClick = { viewModel.onEvent(SwapEvent.SelectTokenFrom(it)) }
            )

            // Swap Icon
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { viewModel.onEvent(SwapEvent.SwapTokens) }) {
                    Icon(Icons.Default.SwapVert, "Swap")
                }
            }

            // To Token
            TokenCard(
                label = "To",
                token = state.tokenTo,
                amount = state.quote?.amountOut?.toString() ?: "-",
                isReadOnly = true,
                onTokenClick = { viewModel.onEvent(SwapEvent.SelectTokenTo(it)) }
            )

            // Quote Card
            if (state.quote != null) {
                QuoteCard(
                    rate = state.displayRate,
                    gasUsd = state.quote!!.gasUsd,
                    isExpired = state.quoteExpired,
                    onRefresh = { viewModel.onEvent(SwapEvent.RefreshQuote) }
                )
            }

            // Error Banner
            if (state.error != null) {
                ErrorBanner(
                    message = state.error!!,
                    onDismiss = { viewModel.onEvent(SwapEvent.DismissError) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Swap Button
            Button(
                onClick = { viewModel.onEvent(SwapEvent.ExecuteSwap) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isValid && !state.isExecuting,
            ) {
                if (state.isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Swap", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun TokenCard(
    label: String,
    token: Token?,
    amount: String = "",
    balance: String? = null,
    isReadOnly: Boolean = false,
    onAmountChange: (String) -> Unit = {},
    onMaxClick: () -> Unit = {},
    onTokenClick: (Token) -> Unit = {}
) {
    // Simplified - expand with full UI
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(token?.symbol ?: "Select token")
            if (!isReadOnly) {
                TextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") }
                )
            } else {
                Text("≈ $amount")
            }
        }
    }
}

@Composable
fun QuoteCard(
    rate: String,
    gasUsd: String,
    isExpired: Boolean,
    onRefresh: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Rate: $rate")
            Text("Gas: $$gasUsd")
            if (isExpired) {
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer
    )) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(message)
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
```

---

## Todo List

- [ ] Create SwapState with all required fields
- [ ] Create SwapEvent sealed class
- [ ] Create SwapEffect sealed class
- [ ] Create SwapViewModel with MVI pattern
- [ ] Implement fetchQuote with debouncing
- [ ] Implement auto-refresh (5s interval)
- [ ] Implement executeSwap with error handling
- [ ] Create SwapScreen composable
- [ ] Create TokenCard composable
- [ ] Create QuoteCard composable
- [ ] Add loading states and error banners
- [ ] Test with real token data

---

## Success Criteria

- [ ] Screen loads without errors
- [ ] Quote fetched on amount change (debounced)
- [ ] Auto-refresh works every 5s
- [ ] Swap button disabled when invalid
- [ ] Loading indicators during API calls
- [ ] Error states displayed clearly
- [ ] Transaction hash shown on success
- [ ] All files <200 lines

---

## Risk Assessment

**Medium Risk:**
- Complex UI state management
- Auto-refresh may cause flicker
- Amount parsing edge cases

**Potential Issues:**
1. **Debounce timing:** 500ms may be too long/short
2. **Quote expiration:** UI flicker on auto-refresh
3. **Amount precision:** BigInteger conversion accuracy
4. **Memory leaks:** Cancel jobs on screen exit

---

## Security Considerations

1. **Amount validation:** Prevent negative/zero amounts
2. **Quote validation:** Check expiration before swap
3. **User confirmation:** Show swap details before execution
4. **Balance check:** Warn if amount > balance

---

## Next Steps

After completion:
1. Proceed to Phase 5: Navigation & Integration
2. Add SwapScreen to navigation graph
3. Connect TradingScreen → SwapScreen

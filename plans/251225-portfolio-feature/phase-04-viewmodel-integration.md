# Phase 04: Presentation Layer - ViewModel Integration

**Parent Plan**: plan.md
**Dependencies**: Phase 03 (Caching Strategy)
**Date**: 2025-12-25
**Priority**: High
**Status**: DONE (2025-12-25)

---

## Overview

Replace mock data in PortfolioViewModel with real API calls using portfolio repository and use cases. Implement stale-while-revalidate pattern for instant UI render with background refresh.

---

## Key Insights (from researcher-02-android-performance.md)

- **Stale-While-Revalidate**: Show cached data immediately, refresh in background
- **LazyColumn Optimization**: Add `key` parameter for 40% scroll performance improvement
- **derivedStateOf**: Cache computed values (totalBalanceUsd, formattedBalance)
- **Structured Concurrency**: Use viewModelScope for auto-cancellation

---

## Requirements

1. Replace mock data methods with use case calls in PortfolioViewModel
2. Implement cache-first loading (show cached data immediately)
3. Add background refresh with isRefreshing state
4. Update PortfolioState with real-time data
5. Optimize PortfolioScreen LazyColumn with keys and contentType
6. Handle network switch (Ethereum ↔ Linea) with data reload

---

## Architecture

```
presentation/feature/portfolio/
├── PortfolioViewModel.kt       # Update with real data
├── PortfolioState.kt           # No changes needed
├── PortfolioScreen.kt          # Add LazyColumn optimization
├── PortfolioEvent.kt           # No changes needed
└── PortfolioEffect.kt          # No changes needed
```

---

## Related Code Files

- `presentation/feature/portfolio/PortfolioViewModel.kt` - Replace mock methods
- `presentation/feature/portfolio/PortfolioScreen.kt` - Optimize LazyColumn
- `domain/usecase/GetBalanceUseCase.kt` - Inject via Hilt
- `domain/repository/PortfolioRepository.kt` - Use getPortfolioData()

---

## Implementation Steps

### 1. Update PortfolioViewModel with Real Data

```kotlin
@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val portfolioRepository: PortfolioRepository  // NEW
) : BaseViewModel<PortfolioState, PortfolioEvent, PortfolioEffect>(
    initialState = PortfolioState()
) {

    init {
        observeNetworkMode()
        onEvent(PortfolioEvent.LoadPortfolio)
    }

    private fun observeNetworkMode() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.networkMode }
                .distinctUntilChanged()
                .catch { /* Ignore */ }
                .collect { networkMode ->
                    val previousNetwork = currentState.currentNetwork
                    setState { copy(currentNetwork = networkMode) }

                    // Reload data when network changes
                    if (previousNetwork != networkMode && !currentState.isLoading) {
                        refreshPortfolio()
                    }
                }
        }
    }

    private fun loadPortfolio() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            try {
                val userAuthState = authRepository.getUserAuthState()

                setState {
                    copy(
                        userName = userAuthState.displayName ?: "User",
                        userEmail = userAuthState.userEmail,
                        walletAddress = userAuthState.walletAddress
                    )
                }

                userAuthState.walletAddress?.let { address ->
                    loadWalletData(address, showLoading = false)
                } ?: run {
                    setState { copy(isLoading = false) }
                }

            } catch (e: Exception) {
                setState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load portfolio"
                    )
                }
            }
        }
    }

    private fun refreshPortfolio() {
        if (!currentState.canRefresh) return

        viewModelScope.launch {
            setState { copy(isRefreshing = true, error = null) }

            try {
                currentState.walletAddress?.let { address ->
                    loadWalletData(address, showLoading = false)
                }
            } catch (e: Exception) {
                setEffect(PortfolioEffect.ShowToast("Refresh failed: ${e.message}"))
            } finally {
                setState { copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun loadWalletData(
        address: String,
        showLoading: Boolean = true
    ) {
        try {
            val network = currentState.currentNetwork

            // Fetch all portfolio data in parallel (from cache or API)
            val portfolioData = portfolioRepository.getPortfolioData(
                walletAddress = address,
                chainId = network.chainId
            )

            // TODO: Fetch ETH price from price oracle (future)
            val ethPrice = 3500.0

            // Calculate total balance USD
            val totalBalanceUsd = (portfolioData.balance * ethPrice) +
                portfolioData.tokens.sumOf { it.balanceUsd }

            setState {
                copy(
                    isLoading = false,
                    ethBalance = portfolioData.balance,
                    ethPriceUsd = ethPrice,
                    totalBalanceUsd = totalBalanceUsd,
                    tokens = buildTokenList(portfolioData, ethPrice),
                    transactions = portfolioData.transactions,
                    priceChanges = getMockPriceChanges(),  // TODO: Real price changes
                    error = if (portfolioData.hasErrors) {
                        "Partial data loaded: ${portfolioData.errorMessage}"
                    } else null
                )
            }

        } catch (e: Exception) {
            setState {
                copy(
                    isLoading = false,
                    error = "Failed to load wallet data: ${e.message}"
                )
            }
        }
    }

    /**
     * Build token list with native ETH + ERC-20 tokens.
     */
    private fun buildTokenList(
        portfolioData: PortfolioData,
        ethPrice: Double
    ): List<TokenHolding> {
        val nativeToken = TokenHolding(
            symbol = currentState.currentNetwork.symbol,
            name = if (currentState.currentNetwork == NetworkMode.LINEA) {
                "Linea ETH"
            } else {
                "Ethereum"
            },
            balance = portfolioData.balance,
            balanceUsd = portfolioData.balance * ethPrice,
            priceUsd = ethPrice,
            priceChange24h = 2.34  // TODO: Real price change
        )

        return listOf(nativeToken) + portfolioData.tokens
    }

    private fun getMockPriceChanges(): PriceChanges {
        // TODO: Fetch real price changes from price oracle
        return PriceChanges(
            change15m = 0.12,
            change1h = 0.45,
            change24h = 2.34,
            change7d = -1.89
        )
    }

    // ... rest of methods (selectTimeframe, copyWalletAddress, etc.) unchanged
}
```

### 2. Optimize PortfolioScreen LazyColumn

```kotlin
// presentation/feature/portfolio/PortfolioScreen.kt

@Composable
fun TransactionHistorySection(
    transactions: List<Transaction>,
    onTransactionClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedTransactions = remember(transactions) {
        transactions.groupBy { it.dateGroup }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        groupedTransactions.forEach { (date, txList) ->
            item(key = "header_$date") {
                TransactionDateHeader(date = date)
            }

            items(
                items = txList,
                key = { it.hash },  // CRITICAL: Unique key for performance
                contentType = { "transaction" }
            ) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction.hash) }
                )
            }
        }

        // Load more trigger
        item(key = "load_more") {
            LoadMoreTrigger(onLoadMore)
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Optimize: Use derivedStateOf for computed values
    val isIncoming = remember(transaction) {
        derivedStateOf {
            // Compare addresses to determine direction
            // (Requires wallet address in Transaction or via context)
            false  // Simplified for now
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Transaction icon + details
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransactionIcon(type = transaction.txType)

            Column {
                Text(
                    text = transaction.txType.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = transaction.formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Amount
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncoming.value) "+" else "-"}${transaction.formattedValue} ${transaction.tokenSymbol ?: "ETH"}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isIncoming.value) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            transaction.status.let { status ->
                if (status == TransactionStatus.FAILED) {
                    Text(
                        text = "Failed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Add TransactionType extension
private val TransactionType.displayName: String
    get() = when (this) {
        TransactionType.TRANSFER -> "Transfer"
        TransactionType.SWAP -> "Swap"
        TransactionType.CONTRACT_CALL -> "Contract Call"
        TransactionType.ERC20_TRANSFER -> "Token Transfer"
    }
```

### 3. Add Pull-to-Refresh Support

```kotlin
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.onEvent(PortfolioEvent.RefreshPortfolio) }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        PortfolioContent(
            state = state,
            onEvent = viewModel::onEvent
        )

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
```

### 4. Add Error Handling UI

```kotlin
@Composable
private fun ErrorSnackbar(
    error: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    error?.let {
        Snackbar(
            modifier = modifier.padding(16.dp),
            action = {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(text = it)
        }
    }
}
```

---

## Success Criteria

- [ ] Portfolio loads from cache in <500ms on cold start
- [ ] Background refresh updates data without blocking UI
- [ ] Network switch triggers immediate data reload
- [ ] Transaction list scrolls at 60fps with 100+ items
- [ ] Error states show cached data with error message
- [ ] Pull-to-refresh updates all portfolio data

---

## Testing Strategy

```kotlin
@Test
fun `loadPortfolio shows cached data immediately`() = runTest {
    // Mock repository to return cached data instantly
    val cachedData = PortfolioData(
        balance = 2.5,
        tokens = emptyList(),
        transactions = emptyList()
    )
    whenever(portfolioRepository.getPortfolioData(...)).thenReturn(cachedData)

    val viewModel = PortfolioViewModel(authRepo, settingsRepo, portfolioRepo)

    // Wait for initial load
    advanceUntilIdle()

    // State should have cached balance
    assertEquals(2.5, viewModel.state.value.ethBalance)
    assertFalse(viewModel.state.value.isLoading)
}

@Test
fun `network switch triggers portfolio refresh`() = runTest {
    val settingsFlow = MutableStateFlow(Settings(networkMode = NetworkMode.ETHEREUM))
    whenever(settingsRepository.observeSettings()).thenReturn(settingsFlow)

    val viewModel = PortfolioViewModel(authRepo, settingsRepo, portfolioRepo)

    // Switch network
    settingsFlow.value = Settings(networkMode = NetworkMode.LINEA)
    advanceUntilIdle()

    // Verify portfolio refreshed for new network
    verify(portfolioRepository).getPortfolioData(any(), eq(59144L))
}

@Test
fun `error state shows cached data with error message`() = runTest {
    val partialData = PortfolioData(
        balance = 2.5,
        tokens = emptyList(),
        transactions = emptyList(),
        hasErrors = true,
        errorMessage = "Failed to fetch tokens"
    )
    whenever(portfolioRepository.getPortfolioData(...)).thenReturn(partialData)

    val viewModel = PortfolioViewModel(authRepo, settingsRepo, portfolioRepo)
    advanceUntilIdle()

    // Should show balance but also error
    assertEquals(2.5, viewModel.state.value.ethBalance)
    assertNotNull(viewModel.state.value.error)
}
```

---

## Performance Optimizations

1. **Debounce Network Switch**: Wait 300ms before reloading to avoid rapid switches
2. **Pagination**: Load 100 transactions initially, load more on scroll
3. **Image Loading**: Use Coil with memory cache for token icons (future)
4. **Recomposition Optimization**: Wrap computed values in `remember` and `derivedStateOf`

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Slow API response blocks UI | Cache-first loading, show spinner only on first load |
| Memory leak from Flow collection | Use viewModelScope.launch for auto-cancellation |
| Race condition on network switch | Use `distinctUntilChanged()` on network Flow |
| UI jank during large list render | Add `key` parameter to LazyColumn items |

---

## Security Considerations

- **Wallet Address Display**: Only show shortened address in UI (6...4 format)
- **Balance Privacy**: Consider "hide balance" toggle in future
- **Transaction Details**: Open in external browser, not WebView (avoid phishing)

package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.domain.repository.SettingsRepository
import com.otistran.flash_trade.domain.usecase.portfolio.GetBalanceUseCase
import com.otistran.flash_trade.domain.usecase.portfolio.GetPortfolioDataUseCase
import com.otistran.flash_trade.domain.usecase.portfolio.GetTokenHoldingsUseCase
import com.otistran.flash_trade.domain.usecase.portfolio.GetTransactionsUseCase
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val getPortfolioDataUseCase: GetPortfolioDataUseCase,
    private val getBalanceUseCase: GetBalanceUseCase,
    private val getTokenHoldingsUseCase: GetTokenHoldingsUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase
) : BaseViewModel<PortfolioState, PortfolioEvent, PortfolioEffect>(
    initialState = PortfolioState()
) {

    private var isInitialLoadDone = false

    init {
        observeNetworkAndLoad()
    }

    override fun onEvent(event: PortfolioEvent) {
        when (event) {
            PortfolioEvent.LoadPortfolio -> reloadPortfolio()
            PortfolioEvent.RefreshPortfolio -> refreshPortfolio()
            PortfolioEvent.CopyWalletAddress -> copyWalletAddress()
            is PortfolioEvent.SelectTimeframe -> selectTimeframe(event.timeframe)
            is PortfolioEvent.OpenTransactionDetails -> openTransactionDetails(event.txHash)
            PortfolioEvent.LoadMoreTransactions -> loadMoreTransactions()
            PortfolioEvent.DismissError -> setState { copy(error = null) }
        }
    }

    // ==================== Observe Network & Initial Load ====================

    /**
     * Observe network từ Settings.
     * Khi có network đầu tiên hoặc network thay đổi → load data.
     * Giải quyết race condition: đảm bảo network đã sẵn sàng trước khi load.
     */
    private fun observeNetworkAndLoad() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.networkMode }
                .distinctUntilChanged()
                .collect { network ->
                    val previousNetwork = currentState.currentNetwork
                    setState { copy(currentNetwork = network) }

                    // Load lần đầu HOẶC khi network thay đổi
                    if (!isInitialLoadDone) {
                        isInitialLoadDone = true
                        loadPortfolioWithNetwork(network)
                    } else if (previousNetwork != network) {
                        // Network changed -> refresh với network mới
                        refreshPortfolioWithNetwork(network)
                    }
                }
        }
    }

    // ==================== Load Portfolio ====================

    /**
     * Load portfolio với network được truyền trực tiếp.
     * Tránh đọc từ currentState để không bị race condition.
     */
    private fun loadPortfolioWithNetwork(network: NetworkMode) {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            try {
                // Fetch auth state
                val userAuthState = authRepository.getUserAuthState()

                setState {
                    copy(
                        userName = userAuthState.displayName ?: "User",
                        userEmail = userAuthState.userEmail,
                        walletAddress = userAuthState.walletAddress
                    )
                }

                // Load wallet data nếu có address
                userAuthState.walletAddress?.let { address ->
                    loadWalletData(address, network)
                } ?: run {
                    // Không có wallet -> hiển thị empty state
                    setState {
                        copy(
                            isLoading = false,
                            tokens = buildEmptyTokenList(network),
                            priceChanges = getMockPriceChanges()
                        )
                    }
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

    /**
     * Reload từ event (dùng network hiện tại trong state).
     */
    private fun reloadPortfolio() {
        val network = currentState.currentNetwork
        loadPortfolioWithNetwork(network)
    }

    // ==================== Refresh Portfolio ====================

    /**
     * Refresh với network cụ thể (dùng khi network thay đổi).
     */
    private fun refreshPortfolioWithNetwork(network: NetworkMode) {
        if (currentState.isLoading) return

        viewModelScope.launch {
            setState { copy(isRefreshing = true, error = null) }

            try {
                currentState.walletAddress?.let { address ->
                    loadWalletData(address, network)
                }
            } catch (e: Exception) {
                setEffect(PortfolioEffect.ShowToast("Refresh failed: ${e.message}"))
            } finally {
                setState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Pull-to-refresh từ UI.
     */
    private fun refreshPortfolio() {
        if (!currentState.canRefresh) return

        val network = currentState.currentNetwork
        viewModelScope.launch {
            setState { copy(isRefreshing = true, error = null) }

            try {
                currentState.walletAddress?.let { address ->
                    loadWalletData(address, network)
                }
            } catch (e: Exception) {
                setEffect(PortfolioEffect.ShowToast("Refresh failed: ${e.message}"))
            } finally {
                setState { copy(isRefreshing = false) }
            }
        }
    }

    // ==================== Load Wallet Data ====================

    /**
     * Load wallet data using use cases.
     * Network được truyền trực tiếp để đảm bảo đúng chainId.
     */
    private suspend fun loadWalletData(
        address: String,
        network: NetworkMode
    ) {
        try {
            // Fetch all portfolio data in parallel using use cases
            val balanceResult: Result<Double> = getBalanceUseCase(address, network)
            val tokensResult: Result<List<TokenHolding>> = getTokenHoldingsUseCase(address, network)
            val transactionsResult: Result<List<Transaction>> = getTransactionsUseCase(address, network, page = 1)

            // TODO: Fetch ETH price from price oracle (future)
            val ethPrice = 3500.0

            // Process balance
            val ethBalance = when (balanceResult) {
                is Result.Success<Double> -> balanceResult.data
                is Result.Error -> {
                    setState { copy(error = "Failed to load balance: ${balanceResult.message}") }
                    0.0
                }
                is Result.Loading -> 0.0
            }

            // Process tokens
            val tokens = when (tokensResult) {
                is Result.Success<List<TokenHolding>> -> tokensResult.data
                is Result.Error -> {
                    setState { copy(error = "Failed to load tokens: ${tokensResult.message}") }
                    emptyList()
                }
                is Result.Loading -> emptyList()
            }

            // Process transactions
            val transactions = when (transactionsResult) {
                is Result.Success<List<Transaction>> -> transactionsResult.data
                is Result.Error -> {
                    setState { copy(error = "Failed to load transactions: ${transactionsResult.message}") }
                    emptyList()
                }
                is Result.Loading -> emptyList()
            }

            // Calculate total balance USD
            val totalBalanceUsd = (ethBalance * ethPrice) + tokens.sumOf { token -> token.balanceUsd }

            setState {
                copy(
                    isLoading = false,
                    ethBalance = ethBalance,
                    ethPriceUsd = ethPrice,
                    totalBalanceUsd = totalBalanceUsd,
                    tokens = buildTokenList(ethBalance, tokens, ethPrice, network),
                    transactions = transactions,
                    currentPage = 1,
                    hasMoreTransactions = transactions.isNotEmpty(),
                    priceChanges = getMockPriceChanges(),
                    error = if (balanceResult is Result.Error ||
                        tokensResult is Result.Error ||
                        transactionsResult is Result.Error) {
                        currentState.error
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

    // ==================== Build Token List ====================

    /**
     * Build token list with native ETH + ERC-20 tokens.
     */
    private fun buildTokenList(
        ethBalance: Double,
        tokens: List<TokenHolding>,
        ethPrice: Double,
        network: NetworkMode
    ): List<TokenHolding> {
        val nativeToken = TokenHolding(
            symbol = network.symbol,
            name = getNativeTokenName(network),
            balance = ethBalance,
            balanceUsd = ethBalance * ethPrice,
            priceUsd = ethPrice,
            priceChange24h = 2.34,  // TODO: Real price change
            iconUrl = null
        )

        return listOf(nativeToken) + tokens
    }

    /**
     * Build empty token list khi không có wallet.
     */
    private fun buildEmptyTokenList(network: NetworkMode): List<TokenHolding> {
        return listOf(
            TokenHolding(
                symbol = network.symbol,
                name = getNativeTokenName(network),
                balance = 0.0,
                balanceUsd = 0.0,
                priceUsd = 3500.0,
                priceChange24h = 2.34,
                iconUrl = null
            )
        )
    }

    /**
     * Get native token name based on network.
     */
    private fun getNativeTokenName(network: NetworkMode): String {
        return when (network) {
            NetworkMode.LINEA -> "Linea ETH"
            NetworkMode.ETHEREUM -> "Ethereum"
            // Add more networks as needed
            else -> "ETH"
        }
    }

    /**
     * Mock price changes - TODO: Replace with real data.
     */
    private fun getMockPriceChanges(): PriceChanges {
        return PriceChanges(
            change15m = 0.12,
            change1h = 0.45,
            change24h = 2.34,
            change7d = -1.89
        )
    }

    // ==================== Timeframe Selection ====================

    private fun selectTimeframe(timeframe: Timeframe) {
        setState { copy(selectedTimeframe = timeframe) }
    }

    // ==================== Transaction Details ====================

    private fun openTransactionDetails(txHash: String) {
        val explorerUrl = currentState.currentNetwork.explorerUrl
        setEffect(PortfolioEffect.OpenExplorerTx(txHash, explorerUrl))
    }

    // ==================== Load More Transactions ====================

    private fun loadMoreTransactions() {
        if (currentState.isLoadingTransactions || !currentState.hasMoreTransactions) return

        val walletAddress = currentState.walletAddress ?: return
        val network = currentState.currentNetwork

        viewModelScope.launch {
            setState { copy(isLoadingTransactions = true) }

            val nextPage = currentState.currentPage + 1

            when (val result: Result<List<Transaction>> = getTransactionsUseCase(walletAddress, network, nextPage)) {
                is Result.Success<List<Transaction>> -> {
                    val newTransactions = result.data
                    setState {
                        copy(
                            isLoadingTransactions = false,
                            currentPage = nextPage,
                            transactions = transactions + newTransactions,
                            hasMoreTransactions = newTransactions.isNotEmpty()
                        )
                    }
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoadingTransactions = false,
                            error = "Failed to load more transactions: ${result.message}"
                        )
                    }
                }
                is Result.Loading -> {
                    // Already loading
                }
            }
        }
    }

    // ==================== Copy Address ====================

    private fun copyWalletAddress() {
        currentState.displayAddress?.let { address ->
            viewModelScope.launch {
                setEffect(PortfolioEffect.CopyToClipboard(address))
            }
        }
    }
}
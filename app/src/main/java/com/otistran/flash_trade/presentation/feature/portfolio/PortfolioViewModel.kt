package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.domain.repository.PortfolioData
import com.otistran.flash_trade.domain.repository.PortfolioRepository
import com.otistran.flash_trade.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val portfolioRepository: PortfolioRepository
) : BaseViewModel<PortfolioState, PortfolioEvent, PortfolioEffect>(
    initialState = PortfolioState()
) {

    init {
        observeNetworkMode()
        onEvent(PortfolioEvent.LoadPortfolio)
    }

    override fun onEvent(event: PortfolioEvent) {
        when (event) {
            PortfolioEvent.LoadPortfolio -> loadPortfolio()
            PortfolioEvent.RefreshPortfolio -> refreshPortfolio()
            PortfolioEvent.CopyWalletAddress -> copyWalletAddress()
            is PortfolioEvent.SelectTimeframe -> selectTimeframe(event.timeframe)
            is PortfolioEvent.OpenTransactionDetails -> openTransactionDetails(event.txHash)
            PortfolioEvent.LoadMoreTransactions -> loadMoreTransactions()
            PortfolioEvent.DismissError -> setState { copy(error = null) }
        }
    }

    // ==================== Observe Network from Settings ====================

    private fun observeNetworkMode() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.networkMode }
                .distinctUntilChanged()
                .catch { /* Ignore errors, use default */ }
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

    // ==================== Load Portfolio ====================

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
                    loadWalletData(address, showLoading = true)
                } ?: run {
                    setState {
                        copy(
                            isLoading = false,
                            tokens = buildTokenList(0.0, 3500.0),
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

    // ==================== Refresh ====================

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

    // ==================== Wallet Data ====================

    private suspend fun loadWalletData(
        address: String,
        showLoading: Boolean = true
    ) {
        try {
            val network = currentState.currentNetwork

            // Fetch all portfolio data in parallel (from cache or API)
            val portfolioData: PortfolioData = portfolioRepository.getPortfolioData(
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

    private fun buildTokenList(
        ethBalance: Double,
        ethPrice: Double
    ): List<TokenHolding> {
        val symbol = currentState.currentNetwork.symbol
        return listOf(
            TokenHolding(
                symbol = symbol,
                name = if (currentState.currentNetwork == NetworkMode.LINEA) "Linea ETH" else "Ethereum",
                balance = ethBalance,
                balanceUsd = ethBalance * ethPrice,
                priceUsd = ethPrice,
                priceChange24h = 2.34,
                iconUrl = null
            )
        )
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
        if (currentState.isLoadingTransactions) return
        // TODO: Implement pagination with repository
    }

    // ==================== Copy Address ====================

    private fun copyWalletAddress() {
        currentState.displayAddress?.let { address ->
            viewModelScope.launch {
                setEffect(PortfolioEffect.CopyToClipboard(address))
                setEffect(PortfolioEffect.ShowToast("Address copied"))
            }
        }
    }
}

package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.AuthRepository
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
    private val settingsRepository: SettingsRepository
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

                    // Reload data khi network thay đổi
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
                    loadWalletData(address)
                } ?: run {
                    setState {
                        copy(
                            isLoading = false,
                            tokens = getDefaultTokens(),
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
            setState { copy(isRefreshing = true) }

            try {
                currentState.walletAddress?.let { address ->
                    loadWalletData(address)
                }
            } catch (e: Exception) {
                setEffect(PortfolioEffect.ShowToast("Failed to refresh"))
            } finally {
                setState { copy(isRefreshing = false) }
            }
        }
    }

    // ==================== Wallet Data ====================

    private suspend fun loadWalletData(address: String) {
        try {
            val network = currentState.currentNetwork
            // TODO: Sử dụng network.chainId hoặc network.chainName để gọi API

            val ethPrice = 3500.0
            val ethBalance = 0.0

            setState {
                copy(
                    isLoading = false,
                    ethBalance = ethBalance,
                    ethPriceUsd = ethPrice,
                    totalBalanceUsd = ethBalance * ethPrice,
                    tokens = getDefaultTokens(ethBalance, ethPrice),
                    priceChanges = getMockPriceChanges(),
                    transactions = getMockTransactions(address)
                )
            }
        } catch (e: Exception) {
            setState {
                copy(
                    isLoading = false,
                    tokens = getDefaultTokens(),
                    priceChanges = getMockPriceChanges()
                )
            }
        }
    }

    private fun getDefaultTokens(
        ethBalance: Double = 0.0,
        ethPrice: Double = 3500.0
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
        return PriceChanges(
            change15m = 0.12,
            change1h = 0.45,
            change24h = 2.34,
            change7d = -1.89
        )
    }

    private fun getMockTransactions(address: String): List<Transaction> {
        val now = System.currentTimeMillis() / 1000
        return listOf(
            Transaction(
                hash = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                blockNumber = "18500000",
                timeStamp = now - 3600,
                from = address,
                to = "0xDef1C0ded9bec7F1a1670819833240f027b25EfF",
                value = "1000000000000000000",
                gas = "21000",
                gasPrice = "20000000000",
                gasUsed = "21000",
                isError = false,
                txType = TransactionType.SWAP
            ),
            Transaction(
                hash = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                blockNumber = "18499000",
                timeStamp = now - 86400,
                from = "0x742d35Cc6634C0532925a3b844Bc9e7595f1d8C2",
                to = address,
                value = "500000000000000000",
                gas = "21000",
                gasPrice = "18000000000",
                gasUsed = "21000",
                isError = false,
                txType = TransactionType.TRANSFER
            )
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
        // TODO: Implement pagination
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
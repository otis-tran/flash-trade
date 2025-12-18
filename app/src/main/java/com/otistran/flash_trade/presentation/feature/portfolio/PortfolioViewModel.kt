package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<PortfolioState, PortfolioEvent, PortfolioEffect>(
    initialState = PortfolioState()
) {

    init {
        onEvent(PortfolioEvent.LoadPortfolio)
    }

    override fun onEvent(event: PortfolioEvent) {
        when (event) {
            PortfolioEvent.LoadPortfolio -> loadPortfolio()
            PortfolioEvent.RefreshPortfolio -> refreshPortfolio()
            PortfolioEvent.CopyWalletAddress -> copyWalletAddress()
            is PortfolioEvent.SelectNetwork -> selectNetwork(event.network)
            PortfolioEvent.DismissError -> setState { copy(error = null) }
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
                            tokens = getDefaultTokens()
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
            // TODO: Integrate with actual wallet/price API
            val ethPrice = 3500.0
            val ethBalance = 0.0

            setState {
                copy(
                    isLoading = false,
                    ethBalance = ethBalance,
                    ethPriceUsd = ethPrice,
                    totalBalanceUsd = ethBalance * ethPrice,
                    tokens = getDefaultTokens(ethBalance, ethPrice)
                )
            }
        } catch (e: Exception) {
            setState {
                copy(
                    isLoading = false,
                    tokens = getDefaultTokens()
                )
            }
        }
    }

    private fun getDefaultTokens(
        ethBalance: Double = 0.0,
        ethPrice: Double = 3500.0
    ): List<TokenHolding> {
        return listOf(
            TokenHolding(
                symbol = "ETH",
                name = "Ethereum",
                balance = ethBalance,
                balanceUsd = ethBalance * ethPrice,
                priceUsd = ethPrice,
                priceChange24h = 2.34,
                iconUrl = null
            )
        )
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

    // ==================== Network Selection ====================

    private fun selectNetwork(network: Network) {
        setState { copy(selectedNetwork = network) }
    }
}
package com.otistran.flash_trade.presentation.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PortfolioState())
    val state = _state.asStateFlow()

    private val _sideEffect = MutableSharedFlow<PortfolioSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    init {
        loadPortfolio()
    }

    fun onIntent(intent: PortfolioIntent) {
        when (intent) {
            is PortfolioIntent.LoadPortfolio -> loadPortfolio()
            is PortfolioIntent.RefreshPortfolio -> refreshPortfolio()
            is PortfolioIntent.CopyWalletAddress -> copyWalletAddress()
            is PortfolioIntent.SelectNetwork -> selectNetwork(intent.network)
        }
    }

    private fun loadPortfolio() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Load user auth state
                val userAuthState = authRepository.getUserAuthState()

                _state.update {
                    it.copy(
                        userName = userAuthState.displayName ?: "User",
                        userEmail = userAuthState.userEmail,
                        walletAddress = userAuthState.walletAddress
                    )
                }

                // Load wallet balance if address exists
                userAuthState.walletAddress?.let { address ->
                    loadWalletData(address)
                } ?: run {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            tokens = getDefaultTokens()
                        )
                    }
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load portfolio"
                    )
                }
            }
        }
    }

    private fun refreshPortfolio() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            try {
                _state.value.walletAddress?.let { address ->
                    loadWalletData(address)
                }
            } catch (e: Exception) {
                _sideEffect.emit(PortfolioSideEffect.ShowToast("Failed to refresh"))
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun loadWalletData(address: String) {
        try {
            // TODO: Integrate with actual wallet/price API
            // For now, use mock data
            val ethPrice = 3500.0 // Mock ETH price
            val ethBalance = 0.0 // Will be fetched from blockchain

            _state.update {
                it.copy(
                    isLoading = false,
                    ethBalance = ethBalance,
                    ethPriceUsd = ethPrice,
                    totalBalanceUsd = ethBalance * ethPrice,
                    tokens = getDefaultTokens(ethBalance, ethPrice)
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
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

    private fun copyWalletAddress() {
        viewModelScope.launch {
            _state.value.displayAddress?.let { address ->
                _sideEffect.emit(PortfolioSideEffect.CopyToClipboard(address))
                _sideEffect.emit(PortfolioSideEffect.ShowToast("Address copied"))
            }
        }
    }

    private fun selectNetwork(network: Network) {
        _state.update { it.copy(selectedNetwork = network) }
    }
}
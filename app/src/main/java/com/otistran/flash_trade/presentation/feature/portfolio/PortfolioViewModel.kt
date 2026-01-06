package com.otistran.flash_trade.presentation.feature.portfolio

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.domain.repository.SettingsRepository
import com.otistran.flash_trade.domain.usecase.portfolio.GetTokensByAddressUseCase
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val getTokensByAddressUseCase: GetTokensByAddressUseCase,
) : BaseViewModel<PortfolioState, PortfolioEvent, PortfolioEffect>(
    initialState = PortfolioState()
) {
    private var loadJob: Job? = null

    init {
        observeNetworkMode(settingsRepository) { network ->
            setState { copy(currentNetwork = network) }
            loadPortfolioWithNetwork(network)
        }
    }

    override fun onEvent(event: PortfolioEvent) {
        when (event) {
            PortfolioEvent.LoadPortfolio -> refreshPortfolio()
            PortfolioEvent.CopyWalletAddress -> copyWalletAddress()
            is PortfolioEvent.SelectAssetTab -> selectAssetTab(event.tab)
            PortfolioEvent.DismissError -> setState { copy(error = null) }
            // Quick Actions
            PortfolioEvent.OnBuyClick -> setEffect(PortfolioEffect.ShowToast("Buy coming soon"))
            PortfolioEvent.OnSwapClick -> setEffect(PortfolioEffect.NavigateToSwap)
            PortfolioEvent.OnSendClick -> setEffect(PortfolioEffect.ShowToast("Send coming soon"))
            PortfolioEvent.OnReceiveClick -> setState { copy(showQRSheet = true) }
            // QR Sheet
            PortfolioEvent.ShowQRSheet -> setState { copy(showQRSheet = true) }
            PortfolioEvent.HideQRSheet -> setState { copy(showQRSheet = false) }
        }
    }

    /**
     * Load portfolio with network passed directly.
     * Cancels previous load job to prevent race conditions.
     */
    private fun loadPortfolioWithNetwork(network: NetworkMode) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            setState { copy(isLoadingTokens = true, error = null) }

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

                // Load wallet data if address available
                userAuthState.walletAddress?.let { address ->
                    loadWalletData(address, network)
                } ?: run {
                    setState {
                        copy(
                            isLoadingTokens = false,
                            tokens = emptyList(),
                            totalBalanceUsd = null
                        )
                    }
                }

            } catch (e: Exception) {
                setState {
                    copy(
                        isLoadingTokens = false,
                        error = e.message ?: "Failed to load portfolio"
                    )
                }
            }
        }
    }

    /**
     * Pull-to-refresh from UI.
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
     * Load wallet data using Alchemy Portfolio API.
     * Single API call for tokens + prices. Transactions fetched in parallel.
     */
    private suspend fun loadWalletData(
        address: String,
        network: NetworkMode
    ) {
        Timber.d("loadWalletData() START - address=$address, network=$network")
        try {
            // Fetch tokens
            val tokensResult = coroutineScope {
                val tokensDeferred = async { getTokensByAddressUseCase(address, network) }
                tokensDeferred.await()
            }

            // Process tokens result
            when (tokensResult) {
                is Result.Success -> {
                    val data = tokensResult.data
                    Timber.d("Tokens: ${data.tokens.size}, Total: $${data.totalBalanceUsd}")
                    setState {
                        copy(
                            isLoadingTokens = false,
                            tokens = data.tokens,
                            totalBalanceUsd = data.totalBalanceUsd
                        )
                    }
                }

                is Result.Error -> {
                    Timber.e("Tokens error: ${tokensResult.message}")
                    setState {
                        copy(
                            isLoadingTokens = false,
                            error = tokensResult.message
                        )
                    }
                    setEffect(PortfolioEffect.ShowToast(tokensResult.message))
                }

                Result.Loading -> {}
            }
        } catch (e: Exception) {
            setState {
                copy(
                    isLoadingTokens = false,
                    error = "Failed to load wallet data: ${e.message}"
                )
            }
            setEffect(PortfolioEffect.ShowToast("Failed to load portfolio"))
        }
    }

    // ==================== Asset Tab Selection ====================

    private fun selectAssetTab(tab: AssetTab) {
        Timber.d("selectAssetTab() - tab: $tab")
        setState { copy(selectedAssetTab = tab) }
    }

    // ==================== Copy Address ====================

    private fun copyWalletAddress() {
        currentState.displayAddress?.let { address ->
            Timber.d("copyWalletAddress() - copying: $address")
            viewModelScope.launch {
                setEffect(PortfolioEffect.CopyToClipboard(address))
            }
        } ?: Timber.w("copyWalletAddress() - no address to copy")
    }
}
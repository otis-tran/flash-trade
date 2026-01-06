package com.otistran.flash_trade.presentation.feature.activity

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.domain.repository.SettingsRepository
import com.otistran.flash_trade.domain.usecase.activity.GetTransactionsUseCase
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Activity screen - handles transaction history display.
 * Reuses GetTransactionsUseCase from portfolio domain.
 */
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val getTransactionsUseCase: GetTransactionsUseCase
) : BaseViewModel<ActivityState, ActivityEvent, ActivityEffect>(
    initialState = ActivityState()
) {

    private var isInitialLoadDone = false

    init {
        observeNetworkMode(settingsRepository) { network ->
            val previousNetwork = currentState.currentNetwork
            setState { copy(currentNetwork = network) }

            if (!isInitialLoadDone) {
                isInitialLoadDone = true
                loadTransactionsWithNetwork(network)
            } else if (previousNetwork != network) {
                refreshTransactionsWithNetwork(network)
            }
        }
    }

    override fun onEvent(event: ActivityEvent) {
        when (event) {
            ActivityEvent.LoadTransactions -> loadTransactions()
            ActivityEvent.RefreshTransactions -> refreshTransactions()
            ActivityEvent.LoadMoreTransactions -> loadMoreTransactions()
            is ActivityEvent.OpenTransactionDetails -> openTransactionDetails(event.txHash)
            ActivityEvent.DismissError -> setState { copy(error = null) }
        }
    }

    // ==================== Load Transactions ====================

    private fun loadTransactionsWithNetwork(network: NetworkMode) {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            try {
                val userAuthState = authRepository.getUserAuthState()
                setState { copy(walletAddress = userAuthState.walletAddress) }

                userAuthState.walletAddress?.let { address ->
                    fetchTransactions(address, network, page = 1)
                } ?: run {
                    setState { copy(isLoading = false, transactions = emptyList()) }
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load transactions"
                    )
                }
            }
        }
    }

    private fun loadTransactions() {
        val network = currentState.currentNetwork
        loadTransactionsWithNetwork(network)
    }

    // ==================== Refresh Transactions ====================

    private fun refreshTransactionsWithNetwork(network: NetworkMode) {
        if (currentState.isLoading) return

        viewModelScope.launch {
            setState { copy(isRefreshing = true, error = null) }

            try {
                currentState.walletAddress?.let { address ->
                    fetchTransactions(address, network, page = 1)
                }
            } catch (e: Exception) {
                setEffect(ActivityEffect.ShowToast("Refresh failed: ${e.message}"))
            } finally {
                setState { copy(isRefreshing = false) }
            }
        }
    }

    private fun refreshTransactions() {
        if (!currentState.canRefresh) return
        val network = currentState.currentNetwork
        refreshTransactionsWithNetwork(network)
    }

    // ==================== Fetch Transactions ====================

    private suspend fun fetchTransactions(
        address: String,
        network: NetworkMode,
        page: Int
    ) {
        when (val result: Result<List<Transaction>> = getTransactionsUseCase(address, network, page)) {
            is Result.Success<List<Transaction>> -> {
                setState {
                    copy(
                        isLoading = false,
                        transactions = result.data,
                        currentPage = page,
                        hasMoreTransactions = result.data.isNotEmpty()
                    )
                }
            }
            is Result.Error -> {
                setState {
                    copy(
                        isLoading = false,
                        error = "Failed to load transactions: ${result.message}"
                    )
                }
            }
            is Result.Loading -> { /* Loading state handled above */ }
        }
    }

    // ==================== Load More Transactions ====================

    private fun loadMoreTransactions() {
        if (currentState.isLoadingMore || !currentState.hasMoreTransactions) return

        val walletAddress = currentState.walletAddress ?: return
        val network = currentState.currentNetwork

        viewModelScope.launch {
            setState { copy(isLoadingMore = true) }

            val nextPage = currentState.currentPage + 1

            when (val result: Result<List<Transaction>> = getTransactionsUseCase(walletAddress, network, nextPage)) {
                is Result.Success<List<Transaction>> -> {
                    val newTransactions = result.data
                    setState {
                        copy(
                            isLoadingMore = false,
                            currentPage = nextPage,
                            transactions = transactions + newTransactions,
                            hasMoreTransactions = newTransactions.isNotEmpty()
                        )
                    }
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoadingMore = false,
                            error = "Failed to load more: ${result.message}"
                        )
                    }
                }
                is Result.Loading -> { /* Loading state handled above */ }
            }
        }
    }

    // ==================== Transaction Details ====================

    private fun openTransactionDetails(txHash: String) {
        val explorerUrl = currentState.currentNetwork.explorerUrl
        setEffect(ActivityEffect.OpenExplorerTx(txHash, explorerUrl))
    }
}

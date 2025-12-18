package com.otistran.flash_trade.presentation.feature.trading

import androidx.lifecycle.viewModelScope
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.usecase.token.GetTokensUseCase
import com.otistran.flash_trade.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val getTokensUseCase: GetTokensUseCase
) : BaseViewModel<TradingState, TradingEvent, TradingEffect>(
    initialState = TradingState()
) {

    private var searchJob: Job? = null

    init {
        loadTokens()
    }

    override fun onEvent(event: TradingEvent) {
        when (event) {
            TradingEvent.LoadTokens -> loadTokens()
            TradingEvent.LoadMore -> loadMore()
            TradingEvent.Refresh -> refresh()
            is TradingEvent.Search -> search(event.query)
            is TradingEvent.SelectToken -> selectToken(event.token)
            TradingEvent.DismissError -> setState { copy(error = null) }
        }
    }

    private fun loadTokens() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            val filter = TokenFilter(
                minTvl = 10000.0, // Only tokens with > $10k TVL
                page = 1,
                limit = 50
            )

            when (val result = getTokensUseCase(filter)) {
                is Result.Success -> {
                    setState {
                        copy(
                            isLoading = false,
                            tokens = result.data.tokens,
                            currentPage = 1,
                            hasMore = result.data.hasMore
                        )
                    }
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                Result.Loading -> { /* Already loading */ }
            }
        }
    }

    private fun loadMore() {
        val currentState = state.value
        if (currentState.isLoadingMore || !currentState.hasMore) return

        viewModelScope.launch {
            setState { copy(isLoadingMore = true) }

            val nextPage = currentState.currentPage + 1
            val filter = TokenFilter(
                minTvl = 10000.0,
                page = nextPage,
                limit = 50
            )

            when (val result = getTokensUseCase(filter)) {
                is Result.Success -> {
                    setState {
                        copy(
                            isLoadingMore = false,
                            tokens = tokens + result.data.tokens,
                            currentPage = nextPage,
                            hasMore = result.data.hasMore
                        )
                    }
                }
                is Result.Error -> {
                    setState { copy(isLoadingMore = false) }
                    setEffect(TradingEffect.ShowToast(result.message))
                }
                Result.Loading -> { /* Already loading */ }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null, currentPage = 1) }

            val filter = TokenFilter(
                minTvl = 10000.0,
                page = 1,
                limit = 50
            )

            when (val result = getTokensUseCase(filter)) {
                is Result.Success -> {
                    setState {
                        copy(
                            isLoading = false,
                            tokens = result.data.tokens,
                            currentPage = 1,
                            hasMore = result.data.hasMore
                        )
                    }
                    setEffect(TradingEffect.ShowToast("Refreshed!"))
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                Result.Loading -> { /* Already loading */ }
            }
        }
    }

    private fun search(query: String) {
        setState { copy(searchQuery = query) }

        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            // Local filtering is handled in state.displayTokens
            // For API search, uncomment below:
            // searchTokensFromApi(query)
        }
    }

    private fun selectToken(token: Token) {
        setState { copy(selectedToken = token) }
        setEffect(TradingEffect.NavigateToTradeDetails(token.address))
    }
}
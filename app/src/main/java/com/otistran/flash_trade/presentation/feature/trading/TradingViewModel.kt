package com.otistran.flash_trade.presentation.feature.trading

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.otistran.flash_trade.core.base.BaseViewModel
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenDisplayFilter
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.model.TokenSortOrder
import com.otistran.flash_trade.domain.usecase.token.GetFilteredTokensUseCase
import com.otistran.flash_trade.domain.usecase.token.GetPagedTokensUseCase
import com.otistran.flash_trade.domain.usecase.token.SearchTokensUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val getPagedTokensUseCase: GetPagedTokensUseCase,
    private val getFilteredTokensUseCase: GetFilteredTokensUseCase,
    private val searchTokensUseCase: SearchTokensUseCase
) : BaseViewModel<TradingState, TradingEvent, TradingEffect>(
    initialState = TradingState()
) {

    private val _displayFilter = MutableStateFlow(TokenDisplayFilter.DEFAULT)
    private val _searchQuery = MutableStateFlow("")

    // Initial data load trigger
    init {
        // Trigger initial load via RemoteMediator
        getPagedTokensUseCase(
            TokenFilter(
                minTvl = 10000.0,
                sort = TokenSortOrder.TVL_DESC,
                limit = 100
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val pagingTokens: Flow<PagingData<Token>> = combine(
        _displayFilter,
        _searchQuery.debounce(300)
    ) { filter, query ->
        filter to query
    }.flatMapLatest { (filter, query) ->
        if (query.isNotBlank()) {
            searchTokensUseCase(query, filter.safeOnly)
        } else {
            getFilteredTokensUseCase(filter)
        }
    }.cachedIn(viewModelScope)

    override fun onEvent(event: TradingEvent) {
        when (event) {
            is TradingEvent.Search -> {
                _searchQuery.value = event.query
                setState { copy(searchQuery = event.query) }
            }
            is TradingEvent.UpdateFilter -> {
                _displayFilter.value = event.filter
                setState { copy(displayFilter = event.filter) }
            }
            TradingEvent.ToggleFilterSheet -> {
                setState { copy(showFilterSheet = !showFilterSheet) }
            }
            is TradingEvent.SelectToken -> {
                setState { copy(selectedToken = event.token) }
                setEffect(TradingEffect.NavigateToTradeDetails(event.token.address))
            }
            TradingEvent.DismissError -> setState { copy(error = null) }
        }
    }
}
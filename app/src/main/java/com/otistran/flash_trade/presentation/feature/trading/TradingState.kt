package com.otistran.flash_trade.presentation.feature.trading

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.Token

@Stable
data class TradingState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val tokens: List<Token> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val selectedToken: Token? = null
) : UiState {

    val isEmpty: Boolean
        get() = tokens.isEmpty() && !isLoading

    val displayTokens: List<Token>
        get() = if (searchQuery.isBlank()) {
            tokens
        } else {
            tokens.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.symbol.contains(searchQuery, ignoreCase = true)
            }
        }
}
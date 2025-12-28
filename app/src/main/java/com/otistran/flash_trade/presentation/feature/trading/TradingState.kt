package com.otistran.flash_trade.presentation.feature.trading

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.core.base.UiState
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenDisplayFilter

@Stable
data class TradingState(
    val currentNetwork: NetworkMode = NetworkMode.DEFAULT,
    val searchQuery: String = "",
    val displayFilter: TokenDisplayFilter = TokenDisplayFilter.DEFAULT,
    val showFilterSheet: Boolean = false,
    val selectedToken: Token? = null,
    val error: String? = null
) : UiState
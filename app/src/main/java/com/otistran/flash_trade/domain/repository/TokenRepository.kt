package com.otistran.flash_trade.domain.repository

import androidx.paging.PagingData
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenDisplayFilter
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for token data operations.
 */
interface TokenRepository {

    suspend fun getTokenByAddress(address: String): Result<Token?>

    suspend fun searchTokens(query: String, limit: Int = 20): Result<List<Token>>

    fun getPagedTokens(filter: TokenFilter = TokenFilter()): Flow<PagingData<Token>>

    fun getPagedTokensFiltered(displayFilter: TokenDisplayFilter, networkMode: NetworkMode): Flow<PagingData<Token>>

    fun searchPagedTokens(query: String, safeOnly: Boolean = false, networkMode: NetworkMode = NetworkMode.DEFAULT): Flow<PagingData<Token>>
}

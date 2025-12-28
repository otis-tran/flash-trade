package com.otistran.flash_trade.domain.usecase.token

import androidx.paging.PagingData
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchTokensUseCase @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    operator fun invoke(query: String, safeOnly: Boolean = false): Flow<PagingData<Token>> {
        return tokenRepository.searchPagedTokens(query, safeOnly)
    }
}
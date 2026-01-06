package com.otistran.flash_trade.domain.usecase.swap

import androidx.paging.PagingData
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Provides paged token search flow.
 */
class SearchTokensUseCase @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    operator fun invoke(
        query: String,
        safeOnly: Boolean,
        network: NetworkMode
    ): Flow<PagingData<Token>> {
        return tokenRepository.searchPagedTokens(query, safeOnly, network)
    }
}

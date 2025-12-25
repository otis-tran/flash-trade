package com.otistran.flash_trade.domain.usecase.token

import androidx.paging.PagingData
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting paginated token stream using Paging 3.
 * Provides offline-first pagination with automatic network sync.
 */
class GetPagedTokensUseCase @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    /**
     * Execute use case to get paginated token stream.
     *
     * @param filter Token filter criteria (minTvl, sort, etc.)
     * @return Flow of PagingData for Compose UI (LazyPagingItems)
     */
    operator fun invoke(filter: TokenFilter = TokenFilter()): Flow<PagingData<Token>> {
        return tokenRepository.getPagedTokens(filter)
    }
}

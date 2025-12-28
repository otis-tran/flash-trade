package com.otistran.flash_trade.domain.usecase.token

import androidx.paging.PagingData
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenDisplayFilter
import com.otistran.flash_trade.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFilteredTokensUseCase @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    operator fun invoke(displayFilter: TokenDisplayFilter): Flow<PagingData<Token>> {
        return tokenRepository.getPagedTokensFiltered(displayFilter)
    }
}
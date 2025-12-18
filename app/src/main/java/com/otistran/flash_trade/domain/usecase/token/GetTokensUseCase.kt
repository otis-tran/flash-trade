package com.otistran.flash_trade.domain.usecase.token

import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.model.TokenListResult
import com.otistran.flash_trade.domain.repository.TokenRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Use case for fetching token list.
 */
class GetTokensUseCase @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    suspend operator fun invoke(
        filter: TokenFilter = TokenFilter()
    ): Result<TokenListResult> {
        return tokenRepository.getTokens(filter)
    }
}
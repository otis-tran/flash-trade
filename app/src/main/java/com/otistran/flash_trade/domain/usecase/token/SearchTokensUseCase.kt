package com.otistran.flash_trade.domain.usecase.token

import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.repository.TokenRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Use case for searching tokens.
 */
class SearchTokensUseCase @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    suspend operator fun invoke(
        query: String,
        limit: Int = 20
    ): Result<List<Token>> {
        if (query.isBlank()) {
            return Result.Success(emptyList())
        }
        return tokenRepository.searchTokens(query.trim(), limit)
    }
}
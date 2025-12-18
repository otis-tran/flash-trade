package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.model.TokenListResult
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for token data operations.
 */
interface TokenRepository {

    /**
     * Get paginated token list with filters.
     */
    suspend fun getTokens(filter: TokenFilter = TokenFilter()): Result<TokenListResult>

    /**
     * Get single token by address.
     */
    suspend fun getTokenByAddress(address: String): Result<Token?>

    /**
     * Search tokens by name or symbol.
     */
    suspend fun searchTokens(query: String, limit: Int = 20): Result<List<Token>>

    /**
     * Get verified/whitelisted tokens only (safe to trade).
     */
    suspend fun getSafeTokens(page: Int = 1, limit: Int = 50): Result<TokenListResult>

    /**
     * Observe cached tokens (for offline support).
     */
    fun observeTokens(): Flow<List<Token>>
}
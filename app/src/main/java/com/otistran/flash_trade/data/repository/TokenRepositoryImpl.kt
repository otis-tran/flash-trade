package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.mapper.TokenMapper.toDomain
import com.otistran.flash_trade.data.mapper.TokenMapper.toDomainList
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.model.TokenListResult
import com.otistran.flash_trade.domain.model.TokenSortOrder
import com.otistran.flash_trade.domain.repository.TokenRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepositoryImpl @Inject constructor(
    private val kyberApi: KyberApiService
) : TokenRepository {

    // In-memory cache
    private val _cachedTokens = MutableStateFlow<List<Token>>(emptyList())

    override suspend fun getTokens(filter: TokenFilter): Result<TokenListResult> {
        return try {
            val response = kyberApi.getTokens(
                minTvl = filter.minTvl,
                maxTvl = filter.maxTvl,
                minVolume = filter.minVolume,
                maxVolume = filter.maxVolume,
                sort = filter.sort.value,
                page = filter.page,
                limit = filter.limit
            )

            val result = response.toDomain()

            // Update cache on first page
            if (filter.page == 1) {
                _cachedTokens.value = result.tokens
            } else {
                _cachedTokens.value = _cachedTokens.value + result.tokens
            }

            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch tokens", e)
        }
    }

    override suspend fun getTokenByAddress(address: String): Result<Token?> {
        return try {
            // First check cache
            val cached = _cachedTokens.value.find { 
                it.address.equals(address, ignoreCase = true) 
            }
            if (cached != null) {
                return Result.Success(cached)
            }

            // Fetch from API with specific filter (low TVL to include all)
            val response = kyberApi.getTokens(
                minTvl = 0.0,
                limit = 1
            )

            val token = response.data.find { 
                it.address.equals(address, ignoreCase = true) 
            }?.toDomain()

            Result.Success(token)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch token", e)
        }
    }

    override suspend fun searchTokens(query: String, limit: Int): Result<List<Token>> {
        return try {
            // Search in cache first
            val cached = _cachedTokens.value.filter { token ->
                token.name.contains(query, ignoreCase = true) ||
                token.symbol.contains(query, ignoreCase = true) ||
                token.address.contains(query, ignoreCase = true)
            }.take(limit)

            if (cached.isNotEmpty()) {
                return Result.Success(cached)
            }

            // Fetch more tokens if cache doesn't have results
            val response = kyberApi.getTokens(
                minTvl = 100.0,
                sort = TokenSortOrder.TVL_DESC.value,
                limit = 500
            )

            val filtered = response.data
                .toDomainList()
                .filter { token ->
                    token.name.contains(query, ignoreCase = true) ||
                    token.symbol.contains(query, ignoreCase = true) ||
                    token.address.contains(query, ignoreCase = true)
                }
                .take(limit)

            Result.Success(filtered)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Search failed", e)
        }
    }

    override suspend fun getSafeTokens(page: Int, limit: Int): Result<TokenListResult> {
        return try {
            val response = kyberApi.getTokens(
                minTvl = 10000.0, // Higher TVL for safety
                sort = TokenSortOrder.TVL_DESC.value,
                page = page,
                limit = limit
            )

            val result = response.toDomain()

            // Filter only safe tokens
            val safeTokens = result.tokens.filter { it.isSafe }

            Result.Success(
                result.copy(
                    tokens = safeTokens,
                    total = safeTokens.size
                )
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch safe tokens", e)
        }
    }

    override fun observeTokens(): Flow<List<Token>> = _cachedTokens.asStateFlow()
}
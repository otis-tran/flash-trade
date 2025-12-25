package com.otistran.flash_trade.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.otistran.flash_trade.data.local.database.FlashTradeDatabase
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.mapper.TokenMapper.toDomain
import com.otistran.flash_trade.data.mapper.TokenMapper.toDomainList
import com.otistran.flash_trade.data.paging.TokenRemoteMediator
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
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepositoryImpl @Inject constructor(
    private val kyberApi: KyberApiService,
    private val database: FlashTradeDatabase,
    private val tokenDao: TokenDao
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

    // ==================== Paging 3 Support ====================

    /**
     * Get paginated token stream using Paging 3.
     * Offline-first with automatic network sync via RemoteMediator.
     *
     * @param filter Token filter criteria (minTvl, sort, etc.)
     * @return Flow of PagingData for Compose UI (LazyPagingItems)
     */
    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedTokens(filter: TokenFilter): Flow<PagingData<Token>> {
        return Pager(
            config = PagingConfig(
                pageSize = filter.limit,
                prefetchDistance = 20,
                enablePlaceholders = false,
                initialLoadSize = filter.limit * 2,
                maxSize = 500
            ),
            remoteMediator = TokenRemoteMediator(
                filter = filter,
                database = database,
                kyberApi = kyberApi
            ),
            pagingSourceFactory = { tokenDao.pagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }
}
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
import com.otistran.flash_trade.domain.model.TokenDisplayFilter
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.model.TokenListResult
import com.otistran.flash_trade.domain.repository.TokenRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepositoryImpl @Inject constructor(
    private val kyberApi: KyberApiService,
    private val database: FlashTradeDatabase,
    private val tokenDao: TokenDao
) : TokenRepository {

    override suspend fun getTokenByAddress(address: String): Result<Token?> {
        return try {
            val cached = tokenDao.getTokenByAddress(address)
            if (cached != null) {
                return Result.Success(cached.toDomain())
            }
            Result.Success(null)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch token", e)
        }
    }

    override suspend fun searchTokens(query: String, limit: Int): Result<List<Token>> {
        return try {
            val results = tokenDao.searchTokens(query, limit)
            Result.Success(results.toDomainList())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Search failed", e)
        }
    }

    // ==================== Paging 3 ====================

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

    override fun getPagedTokensFiltered(displayFilter: TokenDisplayFilter): Flow<PagingData<Token>> {
        val pagingSourceFactory = when {
            displayFilter.safeOnly -> { { tokenDao.pagingSourceSafeOnly() } }
            displayFilter.verifiedOnly -> { { tokenDao.pagingSourceVerifiedOnly() } }
            displayFilter.hideHoneypots -> { { tokenDao.pagingSourceNoHoneypot() } }
            else -> { { tokenDao.pagingSource() } }
        }

        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    override fun searchPagedTokens(query: String, safeOnly: Boolean): Flow<PagingData<Token>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { tokenDao.pagingSourceSearch(query, safeOnly) }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }
}
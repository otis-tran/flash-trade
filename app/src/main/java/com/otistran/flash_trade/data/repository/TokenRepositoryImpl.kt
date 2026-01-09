package com.otistran.flash_trade.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.otistran.flash_trade.data.local.database.dao.TokenDao
import com.otistran.flash_trade.data.mapper.TokenMapper.toDomain
import com.otistran.flash_trade.data.mapper.TokenMapper.toDomainList
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenDisplayFilter
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.repository.TokenRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for token data - reads from local Room database only.
 * Token data is loaded from API at app startup if DB is empty.
 */
@Singleton
class TokenRepositoryImpl @Inject constructor(
    private val tokenDao: TokenDao
) : TokenRepository {

    override suspend fun getTokenByAddress(
        address: String,
        networkMode: NetworkMode
    ): Result<Token?> {
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

    // ==================== Paging 3 (DB only) ====================

    override fun getPagedTokens(filter: TokenFilter): Flow<PagingData<Token>> {
        Timber.d("getPagedTokens: Creating pager from local DB")
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 20,
                enablePlaceholders = false,
                initialLoadSize = 100
            ),
            pagingSourceFactory = { tokenDao.pagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    override fun getPagedTokensFiltered(displayFilter: TokenDisplayFilter, networkMode: NetworkMode): Flow<PagingData<Token>> {
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

    override fun searchPagedTokens(query: String, safeOnly: Boolean, networkMode: NetworkMode): Flow<PagingData<Token>> {
        Timber.d("searchPagedTokens: query='$query', safeOnly=$safeOnly")
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 10,
                enablePlaceholders = false,
                initialLoadSize = 100
            ),
            pagingSourceFactory = { tokenDao.pagingSourceSearch(query, safeOnly) }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }
}
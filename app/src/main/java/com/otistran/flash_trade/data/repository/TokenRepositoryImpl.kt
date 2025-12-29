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
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.TokenDisplayFilter
import com.otistran.flash_trade.domain.model.TokenFilter
import com.otistran.flash_trade.domain.model.TokenListResult
import com.otistran.flash_trade.domain.repository.TokenRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepositoryImpl @Inject constructor(
    private val kyberApi: KyberApiService,
    private val database: FlashTradeDatabase,
    private val tokenDao: TokenDao
) : TokenRepository {

    override suspend fun getTokenByAddress(
        address: String,
        networkMode: NetworkMode
    ): Result<Token?> {
        // Check dummy tokens for LINEA network
        if (networkMode == NetworkMode.LINEA) {
            val dummyToken = getLineaDummyTokens().firstOrNull { it.address == address }
            if (dummyToken != null) {
                return Result.Success(dummyToken)
            }
        }

        // Check database for regular tokens
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

    override fun getPagedTokensFiltered(displayFilter: TokenDisplayFilter, networkMode: NetworkMode): Flow<PagingData<Token>> {
        // Dummy tokens for LINEA network
        if (networkMode == NetworkMode.LINEA) {
            val dummyTokens = getLineaDummyTokens()
            // Apply filter to dummy tokens
            val filtered = dummyTokens.filter { token ->
                when {
                    displayFilter.safeOnly && !token.isSafe -> false
                    displayFilter.verifiedOnly && !token.isVerified -> false
                    displayFilter.hideHoneypots && token.isHoneypot -> false
                    else -> true
                }
            }
            return flowOf(PagingData.from(filtered))
        }

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
        // Dummy tokens for LINEA network
        if (networkMode == NetworkMode.LINEA) {
            val dummyTokens = getLineaDummyTokens()
            val filtered = if (query.isNotBlank()) {
                dummyTokens.filter { token ->
                    token.name?.contains(query, ignoreCase = true) == true ||
                    token.symbol?.contains(query, ignoreCase = true) == true ||
                    token.address.contains(query, ignoreCase = true)
                }
            } else {
                dummyTokens
            }
            return flowOf(PagingData.from(filtered))
        }

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

    private fun getLineaDummyTokens(): List<Token> = listOf(
        Token(
            address = "0xa219439258ca9da29e9cc4ce5596924745e12b93",
            name = "Tether USD",
            symbol = "USDT",
            decimals = 6,
            logoUrl = null,
            isVerified = true,
            isWhitelisted = true,
            isStable = true,
            isHoneypot = false,
            isFot = false,
            tax = 0.0,
            totalTvl = 50000000.0,
            poolCount = 5,
            maxPoolTvl = 10000000.0,
            maxPoolVolume = 5000000.0,
            avgPoolTvl = 10000000.0,
            cgkRank = 3,
            cmcRank = 3,
            websites = "https://tether.to",
            earliestPoolCreatedAt = 1672531200L
        ),
        Token(
            address = "0x1789e0043623282d5dcc7f213d703c6d8bafbb04",
            name = "Linea",
            symbol = "LINEA",
            decimals = 18,
            logoUrl = null,
            isVerified = true,
            isWhitelisted = true,
            isStable = false,
            isHoneypot = false,
            isFot = false,
            tax = 0.0,
            totalTvl = 10000000.0,
            poolCount = 3,
            maxPoolTvl = 5000000.0,
            maxPoolVolume = 1000000.0,
            avgPoolTvl = 3333333.33,
            cgkRank = null,
            cmcRank = null,
            websites = "https://linea.build",
            earliestPoolCreatedAt = 1698796800L
        )
    )
}
package com.otistran.flash_trade.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.otistran.flash_trade.core.network.NetworkResult
import com.otistran.flash_trade.core.network.safeApiCall
import com.otistran.flash_trade.data.local.database.FlashTradeDatabase
import com.otistran.flash_trade.data.local.entity.TokenEntity
import com.otistran.flash_trade.data.local.entity.TokenRemoteKeysEntity
import com.otistran.flash_trade.data.mapper.TokenMapper.toEntityList
import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.domain.model.TokenFilter
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "TokenRemoteMediator"
private const val STARTING_PAGE_INDEX = 1

/**
 * RemoteMediator for Token pagination.
 * Handles network-to-database sync with TTL-based cache invalidation.
 *
 * @param filter Token filter criteria (minTvl, sort order, etc.)
 * @param database Room database instance
 * @param kyberApi Kyber API service
 */
@OptIn(ExperimentalPagingApi::class)
class TokenRemoteMediator(
    private val filter: TokenFilter,
    private val database: FlashTradeDatabase,
    private val kyberApi: KyberApiService
) : RemoteMediator<Int, TokenEntity>() {

    private val tokenDao = database.tokenDao()

    companion object {
        /** Cache TTL: 5 minutes */
        private val CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(5)
    }

    /**
     * Determines if cache should be invalidated based on TTL.
     */
    private suspend fun shouldInvalidateCache(): Boolean {
        val oldestKeyTime = tokenDao.getOldestKeyCreationTime() ?: return true
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - oldestKeyTime
        return cacheAge > CACHE_TTL_MILLIS
    }

    /**
     * Called before loading to determine if we should skip loading.
     * Returns SKIP_INITIAL_REFRESH if cache is fresh.
     */
    override suspend fun initialize(): InitializeAction {
        val shouldRefresh = shouldInvalidateCache()
        Log.d(TAG, "initialize() - shouldRefresh: $shouldRefresh")

        return if (shouldRefresh) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    /**
     * Main load function called by Paging library.
     * Handles REFRESH, PREPEND, APPEND load types.
     */
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TokenEntity>
    ): MediatorResult {
        return try {
            Log.d(TAG, "load() - loadType: $loadType, pages: ${state.pages.size}")

            val page = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                    remoteKeys?.nextPage?.minus(1) ?: STARTING_PAGE_INDEX
                }
                LoadType.PREPEND -> {
                    val remoteKeys = getRemoteKeyForFirstItem(state)
                    remoteKeys?.prevPage
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    remoteKeys?.nextPage
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            Log.d(TAG, "Fetching page: $page")

            // Fetch from API using SafeApiCall
            val result = safeApiCall {
                kyberApi.getTokens(
                    minTvl = filter.minTvl,
                    maxTvl = filter.maxTvl,
                    minVolume = filter.minVolume,
                    maxVolume = filter.maxVolume,
                    sort = filter.sort.value,
                    page = page,
                    limit = filter.limit
                )
            }

            when (result) {
                is NetworkResult.Success -> {
                    val response = result.data
                    val entities = response.data.toEntityList()
                    val endOfPaginationReached = entities.isEmpty() || page >= response.totalPages

                    Log.d(TAG, "Fetched ${entities.size} tokens, endOfPagination: $endOfPaginationReached")

                    database.withTransaction {
                        if (loadType == LoadType.REFRESH) {
                            Log.d(TAG, "REFRESH - clearing cache")
                            tokenDao.clearAll()
                        }

                        val prevPage = if (page == STARTING_PAGE_INDEX) null else page - 1
                        val nextPage = if (endOfPaginationReached) null else page + 1
                        val now = System.currentTimeMillis()

                        val keys = entities.map { entity ->
                            TokenRemoteKeysEntity(
                                tokenAddress = entity.address,
                                prevPage = prevPage,
                                nextPage = nextPage,
                                createdAt = now
                            )
                        }

                        tokenDao.insertRemoteKeys(keys)
                        tokenDao.insertTokens(entities)

                        Log.d(TAG, "Inserted ${entities.size} tokens and ${keys.size} keys")
                    }

                    MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "API error: ${result.exception.message}")
                    MediatorResult.Error(
                        IOException("Failed to fetch tokens: ${result.exception.message}")
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "IOException", e)
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            Log.e(TAG, "HttpException", e)
            MediatorResult.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            MediatorResult.Error(e)
        }
    }

    /**
     * Get remote key for item closest to current scroll position.
     */
    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, TokenEntity>
    ): TokenRemoteKeysEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { token ->
                tokenDao.getRemoteKeyByTokenAddress(token.address)
            }
        }
    }

    /**
     * Get remote key for first item in list.
     */
    private suspend fun getRemoteKeyForFirstItem(
        state: PagingState<Int, TokenEntity>
    ): TokenRemoteKeysEntity? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { token -> tokenDao.getRemoteKeyByTokenAddress(token.address) }
    }

    /**
     * Get remote key for last item in list.
     */
    private suspend fun getRemoteKeyForLastItem(
        state: PagingState<Int, TokenEntity>
    ): TokenRemoteKeysEntity? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { token -> tokenDao.getRemoteKeyByTokenAddress(token.address) }
    }
}

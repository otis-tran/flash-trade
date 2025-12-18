package com.otistran.flash_trade.data.remote.api

import com.otistran.flash_trade.data.remote.dto.kyber.TokenListResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Kyber Market Service API.
 */
interface KyberApiService {

    /**
     * Get list of tokens with pool information.
     */
    @GET("api/v1/tokens")
    suspend fun getTokens(
        @Query("minTvl") minTvl: Double? = null,
        @Query("maxTvl") maxTvl: Double? = null,
        @Query("minVolume") minVolume: Double? = null,
        @Query("maxVolume") maxVolume: Double? = null,
        @Query("minPoolCreated") minPoolCreated: Long? = null,
        @Query("maxPoolCreated") maxPoolCreated: Long? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): TokenListResponse
}
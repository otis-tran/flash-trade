package com.otistran.flash_trade.data.remote.api

import com.otistran.flash_trade.data.remote.dto.kyber.BuildRouteRequestDto
import com.otistran.flash_trade.data.remote.dto.kyber.BuildRouteResponseDto
import com.otistran.flash_trade.data.remote.dto.kyber.RouteResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * KyberSwap Aggregator V1 API for token swaps.
 * Base URL: https://aggregator-api.kyberswap.com
 */
interface KyberSwapApiService {
    @GET("{chain}/api/v1/routes")
    suspend fun getSwapRoute(
        @Path("chain") chain: String,
        @Query("tokenIn") tokenIn: String,
        @Query("tokenOut") tokenOut: String,
        @Query("amountIn") amountIn: String
    ): RouteResponse

    @POST("{chain}/api/v1/route/build")
    suspend fun buildSwapRoute(
        @Path("chain") chain: String,
        @Body request: BuildRouteRequestDto
    ): BuildRouteResponseDto
}

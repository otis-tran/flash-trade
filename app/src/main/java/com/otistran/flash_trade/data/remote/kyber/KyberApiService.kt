package com.otistran.flash_trade.data.remote.kyber

import com.otistran.flash_trade.data.remote.kyber.dto.SwapBuildRequest
import com.otistran.flash_trade.data.remote.kyber.dto.SwapBuildResponse
import com.otistran.flash_trade.data.remote.kyber.dto.SwapRouteResponse
import com.otistran.flash_trade.data.remote.kyber.dto.TokenListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Kyber Aggregator API Service.
 * Docs: https://docs.kyberswap.com/
 */
interface KyberApiService {

    /**
     * Get token list for a chain.
     */
    @GET("{chain}/api/v1/tokens")
    suspend fun getTokens(
        @Path("chain") chain: String = "ethereum"
    ): TokenListResponse

    /**
     * Get optimal swap route.
     */
    @GET("{chain}/api/v1/routes")
    suspend fun getSwapRoute(
        @Path("chain") chain: String = "ethereum",
        @Query("tokenIn") tokenIn: String,
        @Query("tokenOut") tokenOut: String,
        @Query("amountIn") amountIn: String,
        @Query("saveGas") saveGas: Boolean = false,
        @Query("gasInclude") gasInclude: Boolean = true
    ): SwapRouteResponse

    /**
     * Build swap transaction data.
     */
    @POST("{chain}/api/v1/route/build")
    suspend fun buildSwap(
        @Path("chain") chain: String = "ethereum",
        @Body request: SwapBuildRequest
    ): SwapBuildResponse
}

package com.otistran.flash_trade.data.remote.api

import com.otistran.flash_trade.data.remote.dto.kyber.BuildRouteRequestDto
import com.otistran.flash_trade.data.remote.dto.kyber.BuildRouteResponseDto
import com.otistran.flash_trade.data.remote.dto.kyber.RouteResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * KyberSwap Aggregator V1 API for token swaps.
 * Base URL: https://aggregator-api.kyberswap.com
 *
 * Provides MEV-protected swaps across 400+ DEXs on multiple chains.
 */
interface KyberSwapApiService {

    companion object {
        /** Native token address used for ETH/native swaps */
        const val NATIVE_TOKEN_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"

        /** Supported chains */
        const val CHAIN_BASE = "base"
        const val CHAIN_ETHEREUM = "ethereum"
        const val CHAIN_ARBITRUM = "arbitrum"
        const val CHAIN_POLYGON = "polygon"
    }

    /**
     * Get optimal swap route with pricing.
     *
     * @param chain Chain identifier (base, ethereum, arbitrum, polygon)
     * @param tokenIn Input token address (use NATIVE_TOKEN_ADDRESS for native)
     * @param tokenOut Output token address
     * @param amountIn Amount in wei (string for large numbers)
     * @param slippageTolerance Slippage in bps (10 = 0.1%, max 2000)
     * @param origin User wallet for exclusive pool access
     * @return Route with pricing, gas estimates, and routeSummary
     */
    @GET("{chain}/api/v1/routes")
    suspend fun getSwapRoute(
        @Path("chain") chain: String,
        @Query("tokenIn") tokenIn: String,
        @Query("tokenOut") tokenOut: String,
        @Query("amountIn") amountIn: String,
        @Query("slippageTolerance") slippageTolerance: Int? = null,
        @Query("gasInclude") gasInclude: Boolean = true,
        @Query("origin") origin: String? = null
    ): RouteResponseDto

    /**
     * Build encoded swap transaction data.
     *
     * @param chain Chain identifier (must match getSwapRoute)
     * @param request Build request with routeSummary from getSwapRoute
     * @return Encoded calldata for router contract
     */
    @POST("{chain}/api/v1/route/build")
    suspend fun buildSwapRoute(
        @Path("chain") chain: String,
        @Body request: BuildRouteRequestDto
    ): BuildRouteResponseDto
}

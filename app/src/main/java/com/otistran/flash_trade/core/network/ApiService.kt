package com.otistran.flash_trade.core.network

import retrofit2.Response
import retrofit2.http.*

/**
 * Central API service interface for all network endpoints.
 *
 * Naming conventions:
 * - GET single: get{Resource}ById
 * - GET list: get{Resources} / fetch{Resources}
 * - POST create: create{Resource}
 * - PUT/PATCH update: update{Resource}
 * - DELETE: delete{Resource}
 *
 * Headers:
 * - Add @Headers("No-Auth: true") for endpoints that don't need authentication
 */
interface ApiService {

    // ==================== Auth Endpoints ====================
    // Note: Using Privy for auth, so these might not be needed
    // Keeping as examples for standard REST API patterns

    /*
    @Headers("No-Auth: true")
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @Headers("No-Auth: true")
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): TokenResponse
    */

    // ==================== User Endpoints ====================

    /*
    @GET("users/me")
    suspend fun getCurrentUser(): UserDto

    @GET("users/{id}")
    suspend fun getUserById(
        @Path("id") id: String
    ): UserDto

    @PUT("users/me")
    suspend fun updateCurrentUser(
        @Body request: UpdateUserRequest
    ): UserDto
    */

    // ==================== Wallet Endpoints ====================

    /*
    @GET("wallets")
    suspend fun getWallets(): List<WalletDto>

    @GET("wallets/{address}")
    suspend fun getWalletByAddress(
        @Path("address") address: String
    ): WalletDto

    @GET("wallets/{address}/balance")
    suspend fun getWalletBalance(
        @Path("address") address: String,
        @Query("chainId") chainId: Int? = null
    ): BalanceDto
    */

    // ==================== Token Endpoints ====================

    /*
    @GET("tokens")
    suspend fun getTokens(
        @Query("chainId") chainId: Int? = null,
        @Query("search") search: String? = null
    ): List<TokenDto>

    @GET("tokens/{address}")
    suspend fun getTokenByAddress(
        @Path("address") address: String,
        @Query("chainId") chainId: Int
    ): TokenDto

    @GET("tokens/{address}/price")
    suspend fun getTokenPrice(
        @Path("address") address: String,
        @Query("chainId") chainId: Int
    ): PriceDto
    */

    // ==================== Trade Endpoints ====================

    /*
    @GET("trades")
    suspend fun getTrades(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PaginatedResponse<TradeDto>

    @GET("trades/{id}")
    suspend fun getTradeById(
        @Path("id") id: String
    ): TradeDto

    @POST("trades")
    suspend fun createTrade(
        @Body request: CreateTradeRequest
    ): TradeDto

    @DELETE("trades/{id}")
    suspend fun cancelTrade(
        @Path("id") id: String
    ): Response<Unit>
    */

    // ==================== Kyber Network Endpoints ====================
    // Note: These might be in a separate KyberApi interface

    /*
    @GET("quote")
    suspend fun getQuote(
        @Query("tokenIn") tokenIn: String,
        @Query("tokenOut") tokenOut: String,
        @Query("amountIn") amountIn: String,
        @Query("chainId") chainId: Int
    ): QuoteDto

    @POST("swap")
    suspend fun executeSwap(
        @Body request: SwapRequest
    ): SwapResultDto
    */
}
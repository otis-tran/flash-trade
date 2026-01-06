package com.otistran.flash_trade.data.remote.api

import com.otistran.flash_trade.data.remote.dto.alchemy.AlchemyTokenBalancesRequestDto
import com.otistran.flash_trade.data.remote.dto.alchemy.AlchemyTokenBalancesResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Alchemy Data API service.
 * Fetches token balances by wallet address.
 * Base URL: https://api.g.alchemy.com/data/v1/{apiKey}/
 */
interface AlchemyDataApiService {

    /**
     * Get token balances for wallet addresses.
     *
     * @param request Wallet addresses with network info
     * @return Token balances with metadata
     */
    @POST("assets/tokens/by-address")
    suspend fun getTokensByWallet(
        @Body request: AlchemyTokenBalancesRequestDto
    ): AlchemyTokenBalancesResponseDto
}

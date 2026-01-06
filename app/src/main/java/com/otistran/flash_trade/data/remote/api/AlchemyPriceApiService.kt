package com.otistran.flash_trade.data.remote.api

import com.otistran.flash_trade.data.remote.dto.alchemy.AlchemyPriceRequestDto
import com.otistran.flash_trade.data.remote.dto.alchemy.AlchemyPriceResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Alchemy Prices API service.
 * Fetches token prices by contract address.
 * Base URL: https://api.g.alchemy.com/prices/v1/{apiKey}/
 */
interface AlchemyPriceApiService {

    /**
     * Get token prices by contract addresses.
     * Combines prices from DEXes for each contract address and network.
     *
     * @param request List of addresses with network info
     * @return Token prices in USD
     */
    @POST("tokens/by-address")
    suspend fun getPricesByAddress(
        @Body request: AlchemyPriceRequestDto
    ): AlchemyPriceResponseDto
}

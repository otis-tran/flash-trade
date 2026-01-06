package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.TokenPrice
import com.otistran.flash_trade.util.Result

/**
 * Repository interface for fetching token prices.
 */
interface TokenPriceRepository {

    /**
     * Get price for a single token address.
     *
     * @param address Token contract address
     * @param network Network identifier (default: eth-mainnet)
     * @return Token price or null if not found
     */
    suspend fun getPrice(
        address: String,
        network: String = "eth-mainnet"
    ): Result<TokenPrice?>

    /**
     * Get prices for multiple token addresses.
     * Uses batch API call for efficiency.
     *
     * @param addresses List of token contract addresses
     * @param network Network identifier (default: eth-mainnet)
     * @return Map of address to price (USD)
     */
    suspend fun getPrices(
        addresses: List<String>,
        network: String = "eth-mainnet"
    ): Result<Map<String, Double>>

    /**
     * Clear the in-memory price cache.
     */
    fun clearCache()
}

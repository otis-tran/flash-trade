package com.otistran.flash_trade.domain.usecase.token

import com.otistran.flash_trade.domain.repository.TokenPriceRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Use case for fetching token prices.
 * Handles batch requests efficiently with caching.
 */
class GetTokenPricesUseCase @Inject constructor(
    private val tokenPriceRepository: TokenPriceRepository
) {
    /**
     * Get prices for a list of token addresses.
     *
     * @param addresses List of token contract addresses
     * @param network Network identifier (default: eth-mainnet)
     * @return Map of address to price (USD)
     */
    suspend operator fun invoke(
        addresses: List<String>,
        network: String = "eth-mainnet"
    ): Result<Map<String, Double>> {
        if (addresses.isEmpty()) {
            return Result.success(emptyMap())
        }
        return tokenPriceRepository.getPrices(addresses, network)
    }
}

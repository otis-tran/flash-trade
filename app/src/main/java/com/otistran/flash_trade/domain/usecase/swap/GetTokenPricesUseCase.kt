package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.toAlchemyNetwork
import com.otistran.flash_trade.domain.repository.TokenPriceRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Fetches USD prices for tokens.
 */
class GetTokenPricesUseCase @Inject constructor(
    private val tokenPriceRepository: TokenPriceRepository
) {
    data class TokenPrices(
        val sellPrice: Double,
        val buyPrice: Double
    )

    suspend operator fun invoke(
        sellTokenAddress: String,
        buyTokenAddress: String,
        network: NetworkMode,
        clearCache: Boolean = true
    ): Result<TokenPrices> {
        if (clearCache) {
            tokenPriceRepository.clearCache()
        }

        val networkName = network.toAlchemyNetwork()

        return when (val result = tokenPriceRepository.getPrices(
            addresses = listOf(sellTokenAddress, buyTokenAddress),
            network = networkName
        )) {
            is Result.Success -> {
                val sellPrice = result.data[sellTokenAddress.lowercase()] ?: 0.0
                val buyPrice = result.data[buyTokenAddress.lowercase()] ?: 0.0
                Result.Success(TokenPrices(sellPrice, buyPrice))
            }
            is Result.Error -> Result.Error(result.message, result.cause)
            Result.Loading -> Result.Loading
        }
    }
}

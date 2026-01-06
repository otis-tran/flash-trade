package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.mapper.AlchemyPortfolioMapper
import com.otistran.flash_trade.data.remote.api.AlchemyDataApiService
import com.otistran.flash_trade.data.remote.dto.alchemy.AlchemyTokenBalancesRequestDto
import com.otistran.flash_trade.data.remote.dto.alchemy.BalanceAddressDto
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.toAlchemyNetwork
import com.otistran.flash_trade.domain.repository.AlchemyPortfolioRepository
import com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding
import com.otistran.flash_trade.util.Result
import timber.log.Timber
import javax.inject.Inject

class AlchemyPortfolioRepositoryImpl @Inject constructor(
    private val apiService: AlchemyDataApiService,
    private val mapper: AlchemyPortfolioMapper
) : AlchemyPortfolioRepository {

    override suspend fun getTokenHoldings(
        walletAddress: String,
        network: NetworkMode
    ): Result<List<TokenHolding>> {
        return try {
            Timber.d("Fetching tokens for $walletAddress on ${network.toAlchemyNetwork()}")

            val request = AlchemyTokenBalancesRequestDto(
                addresses = listOf(
                    BalanceAddressDto(
                        address = walletAddress,
                        networks = listOf(network.toAlchemyNetwork())
                    )
                ),
                withMetadata = true,
                withPrices = true,
                includeNativeTokens = true
            )

            val response = apiService.getTokensByWallet(request)
            val tokens = response.data.tokens

            Timber.d("Received ${tokens.size} tokens")

            val holdings = tokens
                .filter { it.error == null }
                .mapIndexed { index, dto -> mapper.mapToTokenHolding(dto, index) }
                .filter { it.balance > 0 }
                .sortedByDescending { it.priceUsd }

            Timber.d("Mapped ${holdings.size} holdings")

            Result.Success(holdings)

        } catch (e: Exception) {
            Timber.e("Failed to fetch token holdings", e)
            Result.Error("Failed to load portfolio: ${e.message}")
        }
    }
}

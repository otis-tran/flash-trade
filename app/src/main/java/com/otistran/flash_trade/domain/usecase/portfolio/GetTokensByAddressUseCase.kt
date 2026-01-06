package com.otistran.flash_trade.domain.usecase.portfolio

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.AlchemyPortfolioRepository
import com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding
import com.otistran.flash_trade.util.Result
import java.math.BigDecimal
import javax.inject.Inject

class GetTokensByAddressUseCase @Inject constructor(
    private val repository: AlchemyPortfolioRepository
) {
    suspend operator fun invoke(
        walletAddress: String,
        network: NetworkMode
    ): Result<PortfolioTokensData> {
        return when (val result = repository.getTokenHoldings(walletAddress, network)) {
            is Result.Success -> {
                val summary = calculateSummary(result.data)
                Result.Success(summary)
            }

            is Result.Error -> Result.Error(result.message)
            Result.Loading -> Result.Loading
        }
    }

    private fun calculateSummary(holdings: List<TokenHolding>): PortfolioTokensData {
        val sortedHoldings = holdings
            .filter { it.balance > 0 }
            .sortedByDescending { it.valueUsd ?: BigDecimal.ZERO }

        val totalValue = sortedHoldings.fold(BigDecimal.ZERO) { acc, token ->
            acc + (token.valueUsd ?: BigDecimal.ZERO)
        }

        return PortfolioTokensData(
            tokens = sortedHoldings,
            totalBalanceUsd = totalValue,
        )
    }
}

/**
 * Portfolio tokens data with computed total.
 */
data class PortfolioTokensData(
    val tokens: List<TokenHolding>,
    val totalBalanceUsd: BigDecimal
)

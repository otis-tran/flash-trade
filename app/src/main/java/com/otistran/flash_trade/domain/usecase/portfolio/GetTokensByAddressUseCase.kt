package com.otistran.flash_trade.domain.usecase.portfolio

import com.otistran.flash_trade.data.local.entity.PurchaseStatus
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.AlchemyPortfolioRepository
import com.otistran.flash_trade.domain.repository.PurchaseRepository
import com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import javax.inject.Inject

class GetTokensByAddressUseCase @Inject constructor(
    private val repository: AlchemyPortfolioRepository,
    private val purchaseRepository: PurchaseRepository
) {
    suspend operator fun invoke(
        walletAddress: String,
        network: NetworkMode
    ): Result<PortfolioTokensData> {
        return when (val result = repository.getTokenHoldings(walletAddress, network)) {
            is Result.Success -> {
                // Get active purchases (HELD or SELLING) to show countdown
                val activePurchases = purchaseRepository
                    .observePurchasesByStatuses(listOf(PurchaseStatus.HELD, PurchaseStatus.SELLING))
                    .first()
                    .associateBy { it.tokenAddress.lowercase() }

                // Join token holdings with purchase data for auto-sell countdown
                val holdingsWithAutoSell = result.data.map { holding ->
                    val purchase = holding.address?.let { 
                        activePurchases[it.lowercase()] 
                    }
                    if (purchase != null && purchase.autoSellTime > System.currentTimeMillis()) {
                        holding.copy(autoSellTime = purchase.autoSellTime)
                    } else {
                        holding
                    }
                }

                val summary = calculateSummary(holdingsWithAutoSell)
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

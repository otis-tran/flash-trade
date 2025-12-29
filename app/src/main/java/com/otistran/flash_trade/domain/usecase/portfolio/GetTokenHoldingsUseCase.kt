package com.otistran.flash_trade.domain.usecase.portfolio

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.PortfolioRepository
import com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Use case for fetching token holdings.
 */
class GetTokenHoldingsUseCase @Inject constructor(
    private val repository: PortfolioRepository
) {
    suspend operator fun invoke(
        walletAddress: String,
        networkMode: NetworkMode
    ): Result<List<TokenHolding>> {
        return repository.getTokenHoldings(walletAddress, networkMode.chainId)
    }
}

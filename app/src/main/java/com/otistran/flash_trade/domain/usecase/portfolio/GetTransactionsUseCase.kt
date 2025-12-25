package com.otistran.flash_trade.domain.usecase.portfolio

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.PortfolioRepository
import com.otistran.flash_trade.presentation.feature.portfolio.Transaction
import javax.inject.Inject

/**
 * Use case for fetching transaction history.
 */
class GetTransactionsUseCase @Inject constructor(
    private val repository: PortfolioRepository
) {
    suspend operator fun invoke(
        walletAddress: String,
        networkMode: NetworkMode,
        page: Int = 1
    ): Result<List<Transaction>> {
        return repository.getTransactions(
            walletAddress,
            networkMode.chainId,
            page
        )
    }
}

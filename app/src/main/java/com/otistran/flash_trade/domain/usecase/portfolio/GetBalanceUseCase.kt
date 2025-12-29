package com.otistran.flash_trade.domain.usecase.portfolio

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.PortfolioRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Use case for fetching wallet balance.
 */
class GetBalanceUseCase @Inject constructor(
    private val repository: PortfolioRepository
) {
    suspend operator fun invoke(
        walletAddress: String,
        networkMode: NetworkMode
    ): Result<Double> {
        return repository.getBalance(walletAddress, networkMode.chainId)
    }
}

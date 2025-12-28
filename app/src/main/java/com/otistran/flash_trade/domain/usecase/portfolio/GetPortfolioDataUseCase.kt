package com.otistran.flash_trade.domain.usecase.portfolio

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.PortfolioData
import com.otistran.flash_trade.domain.repository.PortfolioRepository
import javax.inject.Inject

/**
 * Use case for fetching all portfolio data in parallel.
 * Uses supervisorScope for graceful degradation - partial failures
 * return default values instead of throwing exceptions.
 */
class GetPortfolioDataUseCase @Inject constructor(
    private val repository: PortfolioRepository
) {
    suspend operator fun invoke(
        walletAddress: String,
        networkMode: NetworkMode
    ): PortfolioData {
        return repository.getPortfolioData(walletAddress, networkMode.chainId)
    }
}

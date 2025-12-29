package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding
import com.otistran.flash_trade.presentation.feature.portfolio.Transaction
import com.otistran.flash_trade.util.Result

/**
 * Repository interface for portfolio data operations.
 * Provides methods for fetching balances, token holdings, and transaction history.
 */
interface PortfolioRepository {

    /**
     * Fetch native ETH balance for given address and network.
     * @return Balance in ETH (not Wei)
     */
    suspend fun getBalance(
        walletAddress: String,
        chainId: Long
    ): Result<Double>

    /**
     * Fetch ERC-20 token holdings from token transfer history.
     * Aggregates tokentx to calculate current balances.
     */
    suspend fun getTokenHoldings(
        walletAddress: String,
        chainId: Long
    ): Result<List<TokenHolding>>

    /**
     * Fetch transaction history (normal + token transfers merged).
     * @param page Pagination (1-indexed)
     * @param pageSize Number of results per page
     */
    suspend fun getTransactions(
        walletAddress: String,
        chainId: Long,
        page: Int = 1,
        pageSize: Int = 100
    ): Result<List<Transaction>>

    /**
     * Fetch all portfolio data in parallel.
     * Uses supervisorScope - partial failures return default values.
     */
    suspend fun getPortfolioData(
        walletAddress: String,
        chainId: Long
    ): PortfolioData
}

/**
 * Aggregated portfolio data from multiple sources.
 */
data class PortfolioData(
    val balance: Double,
    val tokens: List<TokenHolding>,
    val transactions: List<Transaction>,
    val hasErrors: Boolean = false,
    val errorMessage: String? = null
)

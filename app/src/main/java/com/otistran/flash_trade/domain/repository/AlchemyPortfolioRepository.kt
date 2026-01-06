package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding
import com.otistran.flash_trade.util.Result

/**
 * Repository for fetching portfolio data from Alchemy API.
 * Returns token holdings with balances, metadata, and USD prices.
 */
interface AlchemyPortfolioRepository {

    /**
     * Get all token holdings for wallet.
     * Includes native token (ETH) and ERC-20 tokens with prices.
     *
     * @param walletAddress Ethereum wallet address
     * @param network Target network
     * @return List of TokenHolding with computed balanceUsd
     */
    suspend fun getTokenHoldings(
        walletAddress: String,
        network: NetworkMode
    ): Result<List<TokenHolding>>
}

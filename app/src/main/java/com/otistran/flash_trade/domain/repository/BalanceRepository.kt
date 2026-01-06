package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.util.Result
import java.math.BigDecimal

/**
 * Repository for fetching token balances.
 */
interface BalanceRepository {

    /**
     * Get native token balance (ETH) for wallet.
     */
    suspend fun getNativeBalance(
        walletAddress: String,
        network: NetworkMode
    ): Result<BigDecimal>

    /**
     * Get ERC-20 token balance for wallet.
     */
    suspend fun getTokenBalance(
        walletAddress: String,
        tokenAddress: String,
        tokenDecimals: Int,
        network: NetworkMode
    ): Result<BigDecimal>
}

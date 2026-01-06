package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.presentation.feature.activity.Transaction
import com.otistran.flash_trade.util.Result

interface TransactionsRepository {
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
}

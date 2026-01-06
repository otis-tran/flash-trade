package com.otistran.flash_trade.domain.usecase.activity

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.TransactionsRepository
import com.otistran.flash_trade.presentation.feature.activity.Transaction
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionsRepository
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

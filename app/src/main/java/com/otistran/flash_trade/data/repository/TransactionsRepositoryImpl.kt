package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.mapper.toDomain
import com.otistran.flash_trade.data.remote.api.EtherscanApiService
import com.otistran.flash_trade.di.IoDispatcher
import com.otistran.flash_trade.domain.repository.TransactionsRepository
import com.otistran.flash_trade.presentation.feature.activity.Transaction
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class TransactionsRepositoryImpl @Inject constructor(
    private val etherscanApi: EtherscanApiService,
    @Named("etherscanApiKey") private val apiKey: String,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionsRepository {
    override suspend fun getTransactions(
        walletAddress: String,
        chainId: Long,
        page: Int,
        pageSize: Int
    ): Result<List<Transaction>> = withContext(ioDispatcher) {
        try {
            val normalTxDeferred = async {
                etherscanApi.getTxList(
                    chainId = chainId,
                    address = walletAddress,
                    page = page,
                    offset = pageSize,
                    apiKey = apiKey
                )
            }

            val tokenTxDeferred = async {
                etherscanApi.getTokenTx(
                    chainId = chainId,
                    address = walletAddress,
                    page = page,
                    offset = pageSize,
                    apiKey = apiKey
                )
            }

            val normalTx = normalTxDeferred.await()
            val tokenTx = tokenTxDeferred.await()

            val allTransactions = mutableListOf<Transaction>()

            normalTx.result?.forEach { allTransactions.add(it.toDomain()) }
            tokenTx.result?.forEach { allTransactions.add(it.toDomain()) }

            // Deduplicate by hash (same tx can appear in both lists)
            val sorted = allTransactions
                .distinctBy { it.hash }
                .sortedByDescending { it.timeStamp }

            Result.Success(sorted)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error", e)
        }
    }
}

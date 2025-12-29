package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.core.datastore.CachedBalance
import com.otistran.flash_trade.core.datastore.UserPreferences
import com.otistran.flash_trade.data.mapper.EtherscanMapper
import com.otistran.flash_trade.data.mapper.toDomain
import com.otistran.flash_trade.data.local.database.dao.TransactionDao
import com.otistran.flash_trade.data.local.entity.toDomain
import com.otistran.flash_trade.data.local.entity.toEntity
import com.otistran.flash_trade.data.remote.dto.etherscan.TokenTxDto
import com.otistran.flash_trade.data.remote.api.EtherscanApiService
import com.otistran.flash_trade.domain.repository.PortfolioData
import com.otistran.flash_trade.domain.repository.PortfolioRepository
import com.otistran.flash_trade.di.IoDispatcher
import com.otistran.flash_trade.util.Result
import com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding
import com.otistran.flash_trade.presentation.feature.portfolio.Transaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named

/**
 * Repository implementation for portfolio data.
 * Uses Etherscan V2 API with tiered caching (DataStore for balances, Room for transactions).
 */
class PortfolioRepositoryImpl @Inject constructor(
    private val etherscanApi: EtherscanApiService,
    private val transactionDao: TransactionDao,
    private val userPreferences: UserPreferences,
    @Named("etherscanApiKey") private val apiKey: String,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PortfolioRepository {

    companion object {
        private const val BALANCE_TTL_MS = 30_000L       // 30 seconds
        private const val TRANSACTION_TTL_MS = 3_600_000L  // 1 hour
    }

    override suspend fun getBalance(
        walletAddress: String,
        chainId: Long
    ): Result<Double> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cached = userPreferences.getCachedBalance(chainId)
            if (cached != null && cached.isValid(BALANCE_TTL_MS)) {
                return@withContext Result.Success(cached.balance)
            }

            // Cache miss or expired - fetch from API
            val response = etherscanApi.getBalance(
                chainId = chainId,
                address = walletAddress,
                apiKey = apiKey
            )

            if (response.status == "1" && response.result != null) {
                val ethBalance = EtherscanMapper.weiToEth(response.result)

                // Update cache
                userPreferences.cacheBalance(chainId, ethBalance)

                Result.Success(ethBalance)
            } else {
                // Return cached data if available, even if expired
                cached?.let { Result.Success(it.balance) }
                    ?: Result.Error(response.message)
            }
        } catch (e: Exception) {
            // On error, try to return cached data
            val cached = userPreferences.getCachedBalance(chainId)
            cached?.let { Result.Success(it.balance) }
                ?: Result.Error(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun getTokenHoldings(
        walletAddress: String,
        chainId: Long
    ): Result<List<TokenHolding>> = withContext(ioDispatcher) {
        try {
            val response = etherscanApi.getTokenTx(
                chainId = chainId,
                address = walletAddress,
                apiKey = apiKey,
                page = 1,
                offset = 1000
            )

            if (response.status == "1" && response.result != null) {
                val holdings = aggregateTokenBalances(response.result, walletAddress)
                Result.Success(holdings)
            } else {
                Result.Success(emptyList())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun getTransactions(
        walletAddress: String,
        chainId: Long,
        page: Int,
        pageSize: Int
    ): Result<List<Transaction>> = withContext(ioDispatcher) {
        try {
            // Check cache (only for page 1)
            if (page == 1) {
                val minTimestamp = System.currentTimeMillis() - TRANSACTION_TTL_MS
                val cached = transactionDao.getRecentTransactions(chainId, minTimestamp)

                if (cached.isNotEmpty()) {
                    return@withContext Result.Success(cached.map { it.toDomain() })
                }
            }

            // Fetch from API (parallel normal + token tx)
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

            val sorted = allTransactions.sortedByDescending { it.timeStamp }

            // Cache results (page 1 only)
            if (page == 1 && sorted.isNotEmpty()) {
                transactionDao.insertTransactions(sorted.map { it.toEntity(chainId) })

                // Clean expired cache
                val expiredTimestamp = System.currentTimeMillis() - TRANSACTION_TTL_MS
                transactionDao.deleteExpired(chainId, expiredTimestamp)
            }

            Result.Success(sorted)
        } catch (e: Exception) {
            // On error, return any cached data
            val cached = transactionDao.getTransactions(chainId, pageSize)
            if (cached.isNotEmpty()) {
                Result.Success(cached.map { it.toDomain() })
            } else {
                Result.Error(e.message ?: "Unknown error", e)
            }
        }
    }

    override suspend fun getPortfolioData(
        walletAddress: String,
        chainId: Long
    ): PortfolioData = supervisorScope {
        val errors = mutableListOf<String>()

        val balanceDeferred = async {
            getBalance(walletAddress, chainId).fold(
                onSuccess = { it },
                onError = { message, _ ->
                    errors.add("Balance: $message")
                    0.0
                }
            )
        }

        val tokensDeferred = async {
            getTokenHoldings(walletAddress, chainId).fold(
                onSuccess = { it },
                onError = { message, _ ->
                    errors.add("Tokens: $message")
                    emptyList()
                }
            )
        }

        val transactionsDeferred = async {
            getTransactions(walletAddress, chainId).fold(
                onSuccess = { it },
                onError = { message, _ ->
                    errors.add("Transactions: $message")
                    emptyList()
                }
            )
        }

        PortfolioData(
            balance = balanceDeferred.await(),
            tokens = tokensDeferred.await(),
            transactions = transactionsDeferred.await(),
            hasErrors = errors.isNotEmpty(),
            errorMessage = errors.joinToString("; ").takeIf { it.isNotEmpty() }
        )
    }

    /**
     * Aggregate token transfers to calculate current token balances.
     * Sums incoming - outgoing for each token contract.
     */
    private fun aggregateTokenBalances(
        transfers: List<TokenTxDto>,
        walletAddress: String
    ): List<TokenHolding> {
        val balanceMap = mutableMapOf<String, TokenBalanceAccumulator>()

        transfers.forEach { tx ->
            val contractAddress = tx.contractAddress
            val accumulator = balanceMap.getOrPut(contractAddress) {
                TokenBalanceAccumulator(
                    symbol = tx.tokenSymbol,
                    name = tx.tokenName,
                    decimals = tx.tokenDecimal.toIntOrNull() ?: 18
                )
            }

            val value = tx.value.toBigDecimalOrNull() ?: BigDecimal.ZERO

            when {
                tx.to.equals(walletAddress, ignoreCase = true) -> {
                    accumulator.balance += value
                }
                tx.from.equals(walletAddress, ignoreCase = true) -> {
                    accumulator.balance -= value
                }
            }
        }

        return balanceMap.values
            .filter { it.balance > BigDecimal.ZERO }
            .map { acc ->
                val balance = acc.balance.divide(
                    BigDecimal.TEN.pow(acc.decimals)
                ).toDouble()

                TokenHolding(
                    symbol = acc.symbol,
                    name = acc.name,
                    balance = balance,
                    balanceUsd = 0.0,
                    priceUsd = 0.0,
                    priceChange24h = 0.0
                )
            }
    }
}

private data class TokenBalanceAccumulator(
    val symbol: String,
    val name: String,
    val decimals: Int,
    var balance: BigDecimal = BigDecimal.ZERO
)

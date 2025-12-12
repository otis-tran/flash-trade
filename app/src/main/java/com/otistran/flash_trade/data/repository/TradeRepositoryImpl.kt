package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.local.dao.TradeDao
import com.otistran.flash_trade.data.local.entity.TradeEntity
import com.otistran.flash_trade.di.IoDispatcher
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.Trade
import com.otistran.flash_trade.domain.model.TradeStatus
import com.otistran.flash_trade.domain.repository.TradeRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class TradeRepositoryImpl @Inject constructor(
    private val tradeDao: TradeDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TradeRepository {

    override suspend fun executeTrade(token: Token, amount: Double): Result<Trade> =
        withContext(ioDispatcher) {
            try {
                // TODO: Implement Kyber API trade execution
                val trade = Trade(
                    id = UUID.randomUUID().toString(),
                    tokenAddress = token.address,
                    tokenSymbol = token.symbol,
                    amount = amount,
                    buyPriceUsd = token.priceUsd * amount,
                    status = TradeStatus.PENDING,
                    autoSellTimestamp = System.currentTimeMillis() + (24 * 60 * 60 * 1000),
                    createdAt = System.currentTimeMillis(),
                    walletAddress = "" // TODO: Get from wallet
                )
                tradeDao.insert(TradeEntity.fromDomain(trade))
                Result.Success(trade)
            } catch (e: Exception) {
                Result.Error("Failed to execute trade: ${e.message}", e)
            }
        }

    override suspend fun getTradeById(id: String): Result<Trade> =
        withContext(ioDispatcher) {
            try {
                val entity = tradeDao.getById(id)
                if (entity != null) {
                    Result.Success(entity.toDomain())
                } else {
                    Result.Error("Trade not found")
                }
            } catch (e: Exception) {
                Result.Error("Failed to get trade: ${e.message}", e)
            }
        }

    override fun getAllTrades(): Flow<List<Trade>> =
        tradeDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getPendingTrades(): Flow<List<Trade>> =
        tradeDao.getPending().map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateTradeStatus(id: String, status: TradeStatus): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                tradeDao.updateStatus(id, status.name)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error("Failed to update trade: ${e.message}", e)
            }
        }
}

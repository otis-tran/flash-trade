package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.Trade
import com.otistran.flash_trade.domain.model.TradeStatus
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for trade operations.
 * Implementation in data layer.
 */
interface TradeRepository {
    suspend fun executeTrade(token: Token, amount: Double): Result<Trade>
    suspend fun getTradeById(id: String): Result<Trade>
    fun getAllTrades(): Flow<List<Trade>>
    fun getPendingTrades(): Flow<List<Trade>>
    suspend fun updateTradeStatus(id: String, status: TradeStatus): Result<Unit>
}

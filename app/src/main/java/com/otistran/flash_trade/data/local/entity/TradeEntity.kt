package com.otistran.flash_trade.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.otistran.flash_trade.domain.model.Trade
import com.otistran.flash_trade.domain.model.TradeStatus

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey
    val id: String,
    val tokenAddress: String,
    val tokenSymbol: String,
    val amount: Double,
    val buyPriceUsd: Double,
    val sellPriceUsd: Double?,
    val status: String,
    val autoSellTimestamp: Long,
    val createdAt: Long,
    val walletAddress: String
) {
    fun toDomain(): Trade = Trade(
        id = id,
        tokenAddress = tokenAddress,
        tokenSymbol = tokenSymbol,
        amount = amount,
        buyPriceUsd = buyPriceUsd,
        sellPriceUsd = sellPriceUsd,
        status = TradeStatus.valueOf(status),
        autoSellTimestamp = autoSellTimestamp,
        createdAt = createdAt,
        walletAddress = walletAddress
    )

    companion object {
        fun fromDomain(trade: Trade): TradeEntity = TradeEntity(
            id = trade.id,
            tokenAddress = trade.tokenAddress,
            tokenSymbol = trade.tokenSymbol,
            amount = trade.amount,
            buyPriceUsd = trade.buyPriceUsd,
            sellPriceUsd = trade.sellPriceUsd,
            status = trade.status.name,
            autoSellTimestamp = trade.autoSellTimestamp,
            createdAt = trade.createdAt,
            walletAddress = trade.walletAddress
        )
    }
}

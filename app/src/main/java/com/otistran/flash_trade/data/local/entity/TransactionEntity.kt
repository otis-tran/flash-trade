package com.otistran.flash_trade.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.otistran.flash_trade.presentation.feature.portfolio.Transaction
import com.otistran.flash_trade.presentation.feature.portfolio.TransactionType

/**
 * Room entity for caching transaction data.
 * Separate cache per network via chainId.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val hash: String,
    val chainId: Long,
    val blockNumber: String,
    val timeStamp: Long,
    val fromAddress: String,
    val toAddress: String,
    val value: String,
    val gas: String,
    val gasPrice: String,
    val gasUsed: String,
    val isError: Boolean,
    val txType: String,
    val tokenSymbol: String?,
    val tokenName: String?,
    val tokenDecimal: Int?,
    val contractAddress: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Map TransactionEntity to domain Transaction.
 */
fun TransactionEntity.toDomain(): Transaction = Transaction(
    hash = hash,
    blockNumber = blockNumber,
    timeStamp = timeStamp,
    from = fromAddress,
    to = toAddress,
    value = value,
    gas = gas,
    gasPrice = gasPrice,
    gasUsed = gasUsed,
    isError = isError,
    txType = TransactionType.valueOf(txType),
    tokenSymbol = tokenSymbol,
    tokenName = tokenName,
    tokenDecimal = tokenDecimal,
    contractAddress = contractAddress
)

/**
 * Map domain Transaction to TransactionEntity.
 */
fun Transaction.toEntity(chainId: Long): TransactionEntity = TransactionEntity(
    hash = hash,
    chainId = chainId,
    blockNumber = blockNumber,
    timeStamp = timeStamp,
    fromAddress = from,
    toAddress = to,
    value = value,
    gas = gas,
    gasPrice = gasPrice,
    gasUsed = gasUsed,
    isError = isError,
    txType = txType.name,
    tokenSymbol = tokenSymbol,
    tokenName = tokenName,
    tokenDecimal = tokenDecimal,
    contractAddress = contractAddress
)

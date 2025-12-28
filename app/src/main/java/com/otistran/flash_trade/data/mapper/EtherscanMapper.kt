package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.data.remote.dto.etherscan.TokenTxDto
import com.otistran.flash_trade.data.remote.dto.etherscan.TxDto
import com.otistran.flash_trade.presentation.feature.portfolio.Transaction
import com.otistran.flash_trade.presentation.feature.portfolio.TransactionType
import java.math.BigDecimal

// ==================== DTO -> Domain Extensions ====================

/**
 * Map TokenTxDto to Transaction domain model.
 */
fun TokenTxDto.toDomain(): Transaction = Transaction(
    hash = hash,
    blockNumber = blockNumber,
    timeStamp = timeStamp.toLongOrNull() ?: 0L,
    from = from,
    to = to,
    value = value,
    gas = gas,
    gasPrice = gasPrice,
    gasUsed = gasUsed,
    isError = false,
    txType = TransactionType.ERC20_TRANSFER,
    tokenSymbol = tokenSymbol,
    tokenName = tokenName,
    tokenDecimal = tokenDecimal.toIntOrNull(),
    contractAddress = contractAddress
)

/**
 * Map TxDto to Transaction domain model.
 */
fun TxDto.toDomain(): Transaction = Transaction(
    hash = hash,
    blockNumber = blockNumber,
    timeStamp = timeStamp.toLongOrNull() ?: 0L,
    from = from,
    to = to,
    value = value,
    gas = gas,
    gasPrice = gasPrice,
    gasUsed = gasUsed,
    isError = isError == "1",
    txType = inferTransactionType(this),
    tokenSymbol = null,
    tokenName = null,
    tokenDecimal = null,
    contractAddress = null
)

/**
 * Infer transaction type from TxDto fields.
 */
private fun inferTransactionType(tx: TxDto): TransactionType {
    return when {
        tx.functionName?.contains("swap", ignoreCase = true) == true -> TransactionType.SWAP
        !tx.input.isNullOrBlank() && tx.input != "0x" -> TransactionType.CONTRACT_CALL
        else -> TransactionType.TRANSFER
    }
}

// ==================== Object for Utility Functions ====================

/**
 * Utility object for Etherscan-related conversions.
 */
object EtherscanMapper {

    private val WEI_DIVISOR = BigDecimal("1000000000000000000")

    /**
     * Convert Wei (string) to ETH (Double).
     * Uses BigDecimal to avoid precision loss.
     */
    fun weiToEth(weiString: String): Double {
        return weiString.toBigDecimalOrNull()
            ?.divide(WEI_DIVISOR)
            ?.toDouble() ?: 0.0
    }

    /**
     * Convert Wei (string) to token amount with decimals.
     */
    fun weiToToken(weiString: String, decimals: Int): Double {
        val divisor = BigDecimal.TEN.pow(decimals)
        return weiString.toBigDecimalOrNull()
            ?.divide(divisor)
            ?.toDouble() ?: 0.0
    }
}

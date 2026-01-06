package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.data.remote.dto.alchemy.TokenBalanceDto
import com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Maps Alchemy Token Balances API response to domain model.
 */
class AlchemyPortfolioMapper @Inject constructor() {

    /**
     * Map TokenBalanceDto to TokenHolding.
     * Computes balance and balanceUsd from raw values.
     */
    fun mapToTokenHolding(dto: TokenBalanceDto, index: Int): TokenHolding {
        val metadata = dto.tokenMetadata
        val rawBalance = parseBalance(dto.tokenBalance)
        val decimals = metadata?.decimals ?: 18

        val balance = rawBalance.divide(
            BigDecimal.TEN.pow(decimals),
            decimals.coerceAtMost(8),
            RoundingMode.DOWN
        ).toDouble()

        // Get USD price (can be null)
        val priceUsd = dto.tokenPrices
            ?.firstOrNull { it.currency.equals("usd", ignoreCase = true) }
            ?.value
            ?.toBigDecimalOrNull()

        return TokenHolding(
            id = index,
            symbol = metadata?.symbol ?: shortenAddress(dto.tokenAddress),
            name = metadata?.name ?: "Unknown Token",
            balance = balance,
            priceUsd = priceUsd,
            iconUrl = metadata?.logo,
            address = dto.tokenAddress
        )
    }

    /**
     * Parse balance from hex (0x...) or decimal string.
     */
    private fun parseBalance(balanceStr: String): BigDecimal {
        return if (balanceStr.startsWith("0x")) {
            try {
                BigDecimal(balanceStr.removePrefix("0x").toBigInteger(16))
            } catch (e: Exception) {
                BigDecimal.ZERO
            }
        } else {
            balanceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }
    }

    /**
     * Shorten address for display when symbol not available.
     */
    private fun shortenAddress(address: String?): String {
        return address?.let {
            "${it.take(6)}...${it.takeLast(4)}"
        } ?: "???"
    }
}

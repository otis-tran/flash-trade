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

    companion object {
        /** ETH logo from Kyber assets */
        private const val NATIVE_ETH_LOGO = "https://storage.googleapis.com/ks-setting-1d682dca/8fca1ea5-2637-48bc-bb08-c734065442fe1693634037115.png"
    }

    /**
     * Map TokenBalanceDto to TokenHolding.
     * Computes balance and balanceUsd from raw values.
     * Handles native token (ETH) when tokenAddress is null.
     */
    fun mapToTokenHolding(dto: TokenBalanceDto, index: Int): TokenHolding {
        val metadata = dto.tokenMetadata
        val isNativeToken = dto.tokenAddress == null

        // Native token detection - use ETH defaults
        val symbol = if (isNativeToken) "ETH" else metadata?.symbol ?: shortenAddress(dto.tokenAddress)
        val name = if (isNativeToken) "Ethereum" else metadata?.name ?: "Unknown Token"
        val decimals = if (isNativeToken) 18 else metadata?.decimals ?: 18
        val logo = if (isNativeToken) NATIVE_ETH_LOGO else metadata?.logo

        val rawBalance = parseBalance(dto.tokenBalance)
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
            symbol = symbol,
            name = name,
            balance = balance,
            priceUsd = priceUsd,
            iconUrl = logo,
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

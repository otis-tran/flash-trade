package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.data.remote.dto.alchemy.TokenPriceDataDto
import com.otistran.flash_trade.domain.model.TokenPrice
import java.time.Instant

/**
 * Mapper for converting Alchemy API DTOs to domain models.
 */
object TokenPriceMapper {

    /**
     * Convert Alchemy token price DTO to domain model.
     * Returns null if USD price is not available.
     */
    fun TokenPriceDataDto.toDomain(): TokenPrice? {
        val address = this.address ?: return null
        val priceUsd = prices
            ?.firstOrNull { it.currency == "usd" }
            ?.value
            ?.toDoubleOrNull()
            ?: return null

        val lastUpdated = prices
            .firstOrNull { it.currency == "usd" }
            ?.lastUpdatedAt
            ?.let { parseIsoTimestamp(it) }
            ?: System.currentTimeMillis()

        return TokenPrice(
            address = address,
            priceUsd = priceUsd,
            lastUpdatedAt = lastUpdated
        )
    }

    /**
     * Parse ISO 8601 timestamp to epoch milliseconds.
     */
    private fun parseIsoTimestamp(iso: String): Long {
        return try {
            Instant.parse(iso).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

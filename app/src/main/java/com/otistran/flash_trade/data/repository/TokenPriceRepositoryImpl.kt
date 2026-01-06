package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.mapper.TokenPriceMapper.toDomain
import com.otistran.flash_trade.data.remote.api.AlchemyPriceApiService
import com.otistran.flash_trade.data.remote.dto.alchemy.AddressDto
import com.otistran.flash_trade.data.remote.dto.alchemy.AlchemyPriceRequestDto
import com.otistran.flash_trade.domain.model.TokenPrice
import com.otistran.flash_trade.domain.repository.TokenPriceRepository
import com.otistran.flash_trade.util.Result
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for fetching token prices from Alchemy API.
 * Implements in-memory caching with 5-minute TTL.
 */
@Singleton
class TokenPriceRepositoryImpl @Inject constructor(
    private val alchemyApi: AlchemyPriceApiService
) : TokenPriceRepository {

    private val cache = ConcurrentHashMap<String, CachedPrice>()
    private val cacheTtlMs = 5 * 60 * 1000L // 5 minutes

    override suspend fun getPrice(
        address: String,
        network: String
    ): Result<TokenPrice?> {
        // Check cache first
        val normalizedAddress = address.lowercase()
        cache[normalizedAddress]?.let { cached ->
            if (!cached.isExpired()) {
                return Result.success(cached.price)
            }
        }

        // Fetch from API via batch method
        return getPrices(listOf(address), network).let { result ->
            when (result) {
                is Result.Success -> {
                    val price = result.data[normalizedAddress]
                    if (price != null) {
                        Result.success(
                            TokenPrice(normalizedAddress, price, System.currentTimeMillis())
                        )
                    } else {
                        Result.success(null)
                    }
                }
                is Result.Error -> Result.error(result.message, result.cause)
                is Result.Loading -> Result.loading()
            }
        }
    }

    override suspend fun getPrices(
        addresses: List<String>,
        network: String
    ): Result<Map<String, Double>> {
        if (addresses.isEmpty()) {
            return Result.success(emptyMap())
        }

        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, Double>()
        val uncached = mutableListOf<String>()

        // Check cache for each address
        addresses.forEach { addr ->
            val normalizedAddr = addr.lowercase()
            val cached = cache[normalizedAddr]
            if (cached != null && !cached.isExpired()) {
                cached.price?.priceUsd?.let { result[normalizedAddr] = it }
            } else {
                uncached.add(normalizedAddr)
            }
        }

        // Return early if all addresses are cached
        if (uncached.isEmpty()) {
            return Result.success(result)
        }

        // Fetch uncached addresses from API
        return try {
            val request = AlchemyPriceRequestDto(
                addresses = uncached.map { AddressDto(network, it) }
            )
            val response = alchemyApi.getPricesByAddress(request)

            response.data.forEach { dto ->
                val price = dto.toDomain()
                val normalizedAddr = dto.address?.lowercase()
                if (price != null && normalizedAddr != null) {
                    result[normalizedAddr] = price.priceUsd
                    cache[normalizedAddr] = CachedPrice(price, now + cacheTtlMs)
                }
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.error("Failed to fetch prices: ${e.message}", e)
        }
    }

    override fun clearCache() {
        cache.clear()
    }

    /**
     * Cached price entry with expiration.
     */
    private data class CachedPrice(
        val price: TokenPrice?,
        val expiresAt: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    }
}

package com.otistran.flash_trade.data.remote.dto.alchemy

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request DTO for Alchemy Prices API.
 * POST /tokens/by-address
 */
@JsonClass(generateAdapter = true)
data class AlchemyPriceRequestDto(
    @Json(name = "addresses") val addresses: List<AddressDto>
)

@JsonClass(generateAdapter = true)
data class AddressDto(
    @Json(name = "network") val network: String,
    @Json(name = "address") val address: String
)

/**
 * Response DTO for Alchemy Prices API.
 */
@JsonClass(generateAdapter = true)
data class AlchemyPriceResponseDto(
    @Json(name = "data") val data: List<TokenPriceDataDto>
)

@JsonClass(generateAdapter = true)
data class TokenPriceDataDto(
    @Json(name = "network") val network: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "prices") val prices: List<PriceDto>? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class PriceDto(
    @Json(name = "currency") val currency: String,
    @Json(name = "value") val value: String,
    @Json(name = "lastUpdatedAt") val lastUpdatedAt: String
)

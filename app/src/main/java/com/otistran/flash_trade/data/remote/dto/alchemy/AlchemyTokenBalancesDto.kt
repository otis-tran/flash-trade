package com.otistran.flash_trade.data.remote.dto.alchemy

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request DTO for Alchemy Token Balances API.
 * POST /assets/tokens/balances/by-address
 */
@JsonClass(generateAdapter = true)
data class AlchemyTokenBalancesRequestDto(
    @Json(name = "addresses") val addresses: List<BalanceAddressDto>,
    @Json(name = "withMetadata") val withMetadata: Boolean = true,
    @Json(name = "withPrices") val withPrices: Boolean = true,
    @Json(name = "includeNativeTokens") val includeNativeTokens: Boolean = true
)

@JsonClass(generateAdapter = true)
data class BalanceAddressDto(
    @Json(name = "address") val address: String,
    @Json(name = "networks") val networks: List<String>
)

/**
 * Response DTO for Alchemy Token Balances API.
 */
@JsonClass(generateAdapter = true)
data class AlchemyTokenBalancesResponseDto(
    @Json(name = "data") val data: TokenBalancesDataDto
)

@JsonClass(generateAdapter = true)
data class TokenBalancesDataDto(
    @Json(name = "tokens") val tokens: List<TokenBalanceDto>,
    @Json(name = "pageKey") val pageKey: String? = null
)

@JsonClass(generateAdapter = true)
data class TokenBalanceDto(
    @Json(name = "address") val address: String,
    @Json(name = "network") val network: String,
    @Json(name = "tokenAddress") val tokenAddress: String? = null,
    @Json(name = "tokenBalance") val tokenBalance: String,
    @Json(name = "tokenMetadata") val tokenMetadata: TokenMetadataDto? = null,
    @Json(name = "tokenPrices") val tokenPrices: List<TokenPriceDto>? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class TokenMetadataDto(
    @Json(name = "decimals") val decimals: Int? = null,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "symbol") val symbol: String?=null
)

@JsonClass(generateAdapter = true)
data class TokenPriceDto(
    @Json(name = "currency") val currency: String,
    @Json(name = "value") val value: String,
    @Json(name = "lastUpdatedAt") val lastUpdatedAt: String? = null
)


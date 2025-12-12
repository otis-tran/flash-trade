package com.otistran.flash_trade.data.remote.kyber.dto

import com.otistran.flash_trade.domain.model.Token
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenDto(
    @Json(name = "address") val address: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "name") val name: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "chainId") val chainId: Int? = null,
    @Json(name = "logoURI") val logoUri: String? = null,
    @Json(name = "price") val price: Double? = null
) {
    fun toDomain(defaultChainId: Int): Token = Token(
        address = address,
        symbol = symbol,
        name = name,
        decimals = decimals,
        chainId = chainId ?: defaultChainId,
        logoUrl = logoUri,
        priceUsd = price ?: 0.0
    )
}

@JsonClass(generateAdapter = true)
data class TokenListResponse(
    @Json(name = "tokens") val tokens: List<TokenDto>
)

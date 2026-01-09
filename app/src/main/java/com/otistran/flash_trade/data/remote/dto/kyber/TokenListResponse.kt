package com.otistran.flash_trade.data.remote.dto.kyber
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response wrapper for Kyber token list API.
 */
@JsonClass(generateAdapter = true)
data class TokenListResponse(
    @Json(name = "data") val data: List<TokenDto>,
    @Json(name = "page") val page: Int,
    @Json(name = "pageSize") val pageSize: Int,
    @Json(name = "total") val total: Int,
    @Json(name = "count") val count: Int,
    @Json(name = "totalPages") val totalPages: Int,
    @Json(name = "filter") val filter: TokenFilterDto? = null
)

@JsonClass(generateAdapter = true)
data class TokenDto(
    @Json(name = "address") val address: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "decimals") val decimals: Int = 0,
    @Json(name = "logoUrl") val logoUrl: String? = null,
    @Json(name = "totalSupply") val totalSupply: Double? = null,
    @Json(name = "isVerified") val isVerified: Boolean = false,
    @Json(name = "isWhitelisted") val isWhitelisted: Boolean = false,
    @Json(name = "isStable") val isStable: Boolean = false,
    @Json(name = "isHoneypot") val isHoneypot: Boolean? = null,
    @Json(name = "isFot") val isFot: Boolean? = null,
    @Json(name = "tax") val tax: Double? = null,
    @Json(name = "websites") val websites: String? = null,
    @Json(name = "cgkRank") val cgkRank: Int? = null,
    @Json(name = "cmcRank") val cmcRank: Int? = null,
    @Json(name = "poolCount") val poolCount: Int = 0,
    @Json(name = "totalTvlAllPools") val totalTvlAllPools: String? = null,
    @Json(name = "avgPoolTvl") val avgPoolTvl: String? = null,
    @Json(name = "maxPoolTvl") val maxPoolTvl: String? = null,
    @Json(name = "maxPoolVolume") val maxPoolVolume: String? = null,
    @Json(name = "maxPoolTvlAddress") val maxPoolTvlAddress: String? = null,
    @Json(name = "earliestPoolCreatedAt") val earliestPoolCreatedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class TokenFilterDto(
    @Json(name = "minTvl") val minTvl: Double? = null,
    @Json(name = "maxTvl") val maxTvl: Double? = null,
    @Json(name = "minVolume") val minVolume: Double? = null,
    @Json(name = "maxVolume") val maxVolume: Double? = null,
    @Json(name = "minPoolCreated") val minPoolCreated: Long? = null,
    @Json(name = "maxPoolCreated") val maxPoolCreated: Long? = null,
    @Json(name = "sort") val sort: String? = null
)
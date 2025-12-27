package com.otistran.flash_trade.domain.model

/**
 * Domain model for a tradeable token.
 */
data class Token(
    val address: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val logoUrl: String?,
    val isVerified: Boolean,
    val isWhitelisted: Boolean,
    val isStable: Boolean,
    val isHoneypot: Boolean,
    val totalTvl: Double,
    val poolCount: Int,
    val cgkRank: Int?,
    val cmcRank: Int?
) {
    /** Token is safe to trade (verified, whitelisted, not honeypot) */
    val isSafe: Boolean
        get() = isVerified && isWhitelisted && !isHoneypot

    /** Token has significant liquidity */
    val hasLiquidity: Boolean
        get() = totalTvl > 10_000 && poolCount > 0

    /** Formatted TVL display */
    val formattedTvl: String
        get() = when {
            totalTvl >= 1_000_000_000 -> "$${String.format("%.2f", totalTvl / 1_000_000_000)}B"
            totalTvl >= 1_000_000 -> "$${String.format("%.2f", totalTvl / 1_000_000)}M"
            totalTvl >= 1_000 -> "$${String.format("%.2f", totalTvl / 1_000)}K"
            else -> "$${String.format("%.2f", totalTvl)}"
        }

    /** Short address display */
    val shortAddress: String
        get() = "${address.take(6)}...${address.takeLast(4)}"
}

/**
 * Token list with pagination info.
 */
data class TokenListResult(
    val tokens: List<Token>,
    val page: Int,
    val totalPages: Int,
    val total: Int,
    val hasMore: Boolean
)

/**
 * Filter options for token list.
 */
data class TokenFilter(
    val minTvl: Double? = 1000.0,
    val maxTvl: Double? = null,
    val minVolume: Double? = 1000.0,
    val maxVolume: Double? = null,
    val sort: TokenSortOrder = TokenSortOrder.TVL_DESC,
    val page: Int = 1,
    val limit: Int = 100
)

enum class TokenSortOrder(val value: String) {
    TVL_DESC("tvl_desc"),
    TVL_ASC("tvl_asc"),
    CREATED_DESC("created_desc"),
    CREATED_ASC("created_asc")
}
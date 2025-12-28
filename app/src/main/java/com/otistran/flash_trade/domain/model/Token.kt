package com.otistran.flash_trade.domain.model

/**
 * Domain model for a tradeable token.
 */
data class Token(
    val address: String,
    val name: String?,
    val symbol: String?,
    val decimals: Int,
    val logoUrl: String?,
    val isVerified: Boolean,
    val isWhitelisted: Boolean,
    val isStable: Boolean,
    val isHoneypot: Boolean,
    val isFot: Boolean,
    val tax: Double,
    val totalTvl: Double,
    val poolCount: Int,
    val maxPoolTvl: Double?,
    val maxPoolVolume: Double?,
    val avgPoolTvl: Double?,
    val cgkRank: Int?,
    val cmcRank: Int?,
    val websites: String?,
    val earliestPoolCreatedAt: Long?
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: symbol?.takeIf { it.isNotBlank() }
            ?: shortAddress

    val displaySymbol: String
        get() = symbol?.takeIf { it.isNotBlank() } ?: "???"

    val isSafe: Boolean
        get() = isVerified && isWhitelisted && !isHoneypot && !isFot

    val riskLevel: RiskLevel
        get() = when {
            isHoneypot -> RiskLevel.SCAM
            !isVerified && !isWhitelisted -> RiskLevel.HIGH
            isVerified && !isWhitelisted -> RiskLevel.MEDIUM
            isFot || tax > 0 -> RiskLevel.MEDIUM
            isSafe && hasLiquidity -> RiskLevel.LOW
            else -> RiskLevel.MEDIUM
        }

    val hasLiquidity: Boolean
        get() = totalTvl > 10_000 && poolCount > 0

    val isWellKnown: Boolean
        get() = cgkRank != null || cmcRank != null

    val bestRank: Int?
        get() = listOfNotNull(cgkRank, cmcRank).minOrNull()

    val formattedTvl: String
        get() = formatCurrency(totalTvl)

    val formattedMaxVolume: String
        get() = maxPoolVolume?.let { formatCurrency(it) } ?: "N/A"

    val shortAddress: String
        get() = "${address.take(6)}...${address.takeLast(4)}"

    val ageInDays: Long?
        get() = earliestPoolCreatedAt?.let {
            (System.currentTimeMillis() / 1000 - it) / 86400
        }

    private fun formatCurrency(value: Double): String = when {
        value >= 1_000_000_000 -> "$%.2fB".format(value / 1_000_000_000)
        value >= 1_000_000 -> "$%.2fM".format(value / 1_000_000)
        value >= 1_000 -> "$%.2fK".format(value / 1_000)
        else -> "$%.2f".format(value)
    }
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
 * API filter options for token list.
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

/**
 * UI display filter options.
 */
data class TokenDisplayFilter(
    val safeOnly: Boolean = false,
    val verifiedOnly: Boolean = false,
    val hideHoneypots: Boolean = true,
    val searchQuery: String = ""
) {
    companion object {
        val DEFAULT = TokenDisplayFilter(hideHoneypots = true)
        val SAFE = TokenDisplayFilter(safeOnly = true, hideHoneypots = true)
    }
}

enum class TokenSortOrder(val value: String) {
    TVL_DESC("tvl_desc"),
    TVL_ASC("tvl_asc"),
    CREATED_DESC("created_desc"),
    CREATED_ASC("created_asc")
}

enum class RiskLevel(val displayName: String, val color: Long) {
    LOW("Low Risk", 0xFF4CAF50),
    MEDIUM("Medium Risk", 0xFFFF9800),
    HIGH("High Risk", 0xFFF44336),
    SCAM("Scam", 0xFF9C27B0)
}
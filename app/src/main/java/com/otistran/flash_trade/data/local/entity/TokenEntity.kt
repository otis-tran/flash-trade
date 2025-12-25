package com.otistran.flash_trade.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for cached token data.
 * Indexed on address (unique lookups) and totalTvl (sorted queries).
 */
@Entity(
    tableName = "tokens",
    indices = [
        Index(value = ["address"], unique = true),
        Index(value = ["total_tvl"])
    ]
)
data class TokenEntity(
    @PrimaryKey
    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "symbol")
    val symbol: String,

    @ColumnInfo(name = "decimals")
    val decimals: Int,

    @ColumnInfo(name = "logo_url")
    val logoUrl: String?,

    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean,

    @ColumnInfo(name = "is_whitelisted")
    val isWhitelisted: Boolean,

    @ColumnInfo(name = "is_stable")
    val isStable: Boolean,

    @ColumnInfo(name = "is_honeypot")
    val isHoneypot: Boolean,

    @ColumnInfo(name = "total_tvl")
    val totalTvl: Double,

    @ColumnInfo(name = "pool_count")
    val poolCount: Int,

    @ColumnInfo(name = "cgk_rank")
    val cgkRank: Int?,

    @ColumnInfo(name = "cmc_rank")
    val cmcRank: Int?,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

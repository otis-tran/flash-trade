package com.otistran.flash_trade.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for cached token data.
 */
@Entity(
    tableName = "tokens",
    indices = [
        Index(value = ["address"], unique = true),
        Index(value = ["total_tvl"]),
        Index(value = ["is_verified"]),
        Index(value = ["is_honeypot"]),
        Index(value = ["symbol"]),
        Index(value = ["sync_generation"]),  // for cleanup queries
        Index(value = ["pool_count", "total_tvl"])  // composite index for sort order
    ]
)
data class TokenEntity(
    @PrimaryKey
    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "symbol")
    val symbol: String?,

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

    @ColumnInfo(name = "is_fot")
    val isFot: Boolean,

    @ColumnInfo(name = "tax")
    val tax: Double,

    @ColumnInfo(name = "total_tvl")
    val totalTvl: Double,

    @ColumnInfo(name = "pool_count")
    val poolCount: Int,

    @ColumnInfo(name = "max_pool_tvl")
    val maxPoolTvl: Double?,

    @ColumnInfo(name = "max_pool_volume")
    val maxPoolVolume: Double?,

    @ColumnInfo(name = "avg_pool_tvl")
    val avgPoolTvl: Double?,

    @ColumnInfo(name = "cgk_rank")
    val cgkRank: Int?,

    @ColumnInfo(name = "cmc_rank")
    val cmcRank: Int?,

    @ColumnInfo(name = "websites")
    val websites: String?,

    @ColumnInfo(name = "earliest_pool_created_at")
    val earliestPoolCreatedAt: Long?,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "sync_generation", defaultValue = "0")
    val syncGeneration: Int = 0  // NEW: tracks which sync session this token was seen in
)
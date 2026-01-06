package com.otistran.flash_trade.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks pagination state for RemoteMediator.
 * Each token has a remote key entry with prev/next page numbers.
 */
@Entity(tableName = "token_remote_keys")
data class TokenRemoteKeysEntity(
    @PrimaryKey
    @ColumnInfo(name = "token_address")
    val tokenAddress: String,

    @ColumnInfo(name = "prev_page")
    val prevPage: Int?,

    @ColumnInfo(name = "next_page")
    val nextPage: Int?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

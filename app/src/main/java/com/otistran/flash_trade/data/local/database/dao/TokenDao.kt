package com.otistran.flash_trade.data.local.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.otistran.flash_trade.data.local.entity.TokenEntity
import com.otistran.flash_trade.data.local.entity.TokenRemoteKeysEntity

/**
 * DAO for token operations with Paging 3 support.
 */
@Dao
interface TokenDao {

    // ==================== Token Operations ====================

    /**
     * Returns PagingSource for Paging 3 library.
     * Sorted by total TVL descending by default.
     */
    @Query("SELECT * FROM tokens ORDER BY total_tvl DESC")
    fun pagingSource(): PagingSource<Int, TokenEntity>

    /**
     * Get all cached tokens (for search/filter).
     */
    @Query("SELECT * FROM tokens ORDER BY total_tvl DESC")
    suspend fun getAllTokens(): List<TokenEntity>

    /**
     * Get single token by address.
     */
    @Query("SELECT * FROM tokens WHERE address = :address LIMIT 1")
    suspend fun getTokenByAddress(address: String): TokenEntity?

    /**
     * Search tokens by name or symbol.
     */
    @Query("""
        SELECT * FROM tokens
        WHERE name LIKE '%' || :query || '%'
           OR symbol LIKE '%' || :query || '%'
        ORDER BY total_tvl DESC
        LIMIT :limit
    """)
    suspend fun searchTokens(query: String, limit: Int): List<TokenEntity>

    /**
     * Insert or replace tokens (UPSERT).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokens(tokens: List<TokenEntity>)

    /**
     * Clear all tokens (for cache refresh).
     */
    @Query("DELETE FROM tokens")
    suspend fun clearTokens()

    /**
     * Get tokens count.
     */
    @Query("SELECT COUNT(*) FROM tokens")
    suspend fun getTokenCount(): Int

    /**
     * Get tokens older than TTL threshold.
     */
    @Query("SELECT * FROM tokens WHERE cached_at < :threshold LIMIT 1")
    suspend fun getStaleToken(threshold: Long): TokenEntity?

    // ==================== Remote Keys Operations ====================

    /**
     * Insert or replace remote keys.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemoteKeys(keys: List<TokenRemoteKeysEntity>)

    /**
     * Get remote key by token address.
     */
    @Query("SELECT * FROM token_remote_keys WHERE token_address = :tokenAddress")
    suspend fun getRemoteKeyByTokenAddress(tokenAddress: String): TokenRemoteKeysEntity?

    /**
     * Clear all remote keys (for cache refresh).
     */
    @Query("DELETE FROM token_remote_keys")
    suspend fun clearRemoteKeys()

    /**
     * Get creation time of oldest remote key (for TTL check).
     */
    @Query("SELECT created_at FROM token_remote_keys ORDER BY created_at ASC LIMIT 1")
    suspend fun getOldestKeyCreationTime(): Long?

    // ==================== Atomic Transactions ====================

    /**
     * Atomic transaction: clear cache and keys.
     */
    @Transaction
    suspend fun clearAll() {
        clearTokens()
        clearRemoteKeys()
    }
}

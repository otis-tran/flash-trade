package com.otistran.flash_trade.data.local.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.otistran.flash_trade.data.local.entity.TokenEntity
import com.otistran.flash_trade.data.local.entity.TokenRemoteKeysEntity

/**
 * DAO for token operations with Paging 3 support.
 */
@Dao
interface TokenDao {

    // ==================== Paging Sources ====================

    @Query("SELECT * FROM tokens ORDER BY total_tvl DESC")
    fun pagingSource(): PagingSource<Int, TokenEntity>

    @Query("""
        SELECT * FROM tokens 
        WHERE is_verified = 1 
          AND is_whitelisted = 1 
          AND is_honeypot = 0 
          AND is_fot = 0
        ORDER BY total_tvl DESC
    """)
    fun pagingSourceSafeOnly(): PagingSource<Int, TokenEntity>

    @Query("""
        SELECT * FROM tokens 
        WHERE is_verified = 1
        ORDER BY total_tvl DESC
    """)
    fun pagingSourceVerifiedOnly(): PagingSource<Int, TokenEntity>

    @Query("""
        SELECT * FROM tokens 
        WHERE is_honeypot = 0
        ORDER BY total_tvl DESC
    """)
    fun pagingSourceNoHoneypot(): PagingSource<Int, TokenEntity>

    @Query("""
        SELECT * FROM tokens
        WHERE (name LIKE '%' || :query || '%' OR symbol LIKE '%' || :query || '%')
          AND (:safeOnly = 0 OR (is_verified = 1 AND is_whitelisted = 1 AND is_honeypot = 0 AND is_fot = 0))
        ORDER BY total_tvl DESC
    """)
    fun pagingSourceSearch(query: String, safeOnly: Boolean = false): PagingSource<Int, TokenEntity>

    // ==================== Standard Queries ====================

    @Query("SELECT * FROM tokens WHERE address = :address LIMIT 1")
    suspend fun getTokenByAddress(address: String): TokenEntity?

    @Query("""
        SELECT * FROM tokens
        WHERE name LIKE '%' || :query || '%' OR symbol LIKE '%' || :query || '%'
        ORDER BY total_tvl DESC
        LIMIT :limit
    """)
    suspend fun searchTokens(query: String, limit: Int): List<TokenEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokens(tokens: List<TokenEntity>)

    /**
     * Upsert tokens - insert or update if exists.
     * Used by background sync.
     */
    @Upsert
    suspend fun upsertTokens(tokens: List<TokenEntity>)

    @Query("DELETE FROM tokens")
    suspend fun clearTokens()

    @Query("SELECT COUNT(*) FROM tokens")
    suspend fun getTokenCount(): Int

    // ==================== Sync Queries ====================

    /**
     * Delete tokens not seen in the current sync generation.
     * Returns number of deleted rows.
     */
    @Query("DELETE FROM tokens WHERE sync_generation < :minGeneration")
    suspend fun deleteStaleTokens(minGeneration: Int): Int

    /**
     * Count tokens matching a specific filter.
     * Used to check if we have enough tokens after filtering.
     */
    @Query("""
        SELECT COUNT(*) FROM tokens 
        WHERE is_verified = 1 
          AND is_whitelisted = 1 
          AND is_honeypot = 0 
          AND is_fot = 0
    """)
    suspend fun countSafeTokens(): Int

    @Query("SELECT COUNT(*) FROM tokens WHERE is_verified = 1")
    suspend fun countVerifiedTokens(): Int

    @Query("SELECT COUNT(*) FROM tokens WHERE is_honeypot = 0")
    suspend fun countNonHoneypotTokens(): Int

    /**
     * Get tokens older than TTL threshold.
     */
    @Query("SELECT * FROM tokens WHERE cached_at < :threshold LIMIT 1")
    suspend fun getStaleToken(threshold: Long): TokenEntity?

    // ==================== Remote Keys ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemoteKeys(keys: List<TokenRemoteKeysEntity>)

    @Query("SELECT * FROM token_remote_keys WHERE token_address = :tokenAddress")
    suspend fun getRemoteKeyByTokenAddress(tokenAddress: String): TokenRemoteKeysEntity?

    @Query("DELETE FROM token_remote_keys")
    suspend fun clearRemoteKeys()

    @Query("SELECT created_at FROM token_remote_keys ORDER BY created_at ASC LIMIT 1")
    suspend fun getOldestKeyCreationTime(): Long?

    @Transaction
    suspend fun clearAll() {
        clearTokens()
        clearRemoteKeys()
    }
}
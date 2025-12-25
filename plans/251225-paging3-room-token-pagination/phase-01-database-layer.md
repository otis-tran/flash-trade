# Phase 1: Database Layer

**Estimated Effort:** 2-3 hours
**Dependencies:** None
**Status:** Pending

---

## Objectives

1. Add Paging 3 dependencies to build.gradle.kts
2. Create TokenEntity with proper indices for query performance
3. Create TokenRemoteKeysEntity for pagination state tracking
4. Create TokenDao with PagingSource query + CRUD operations
5. Update FlashTradeDatabase to include new entities

---

## Implementation Steps

### Step 1.1: Add Paging 3 Dependencies

**File:** `app/build.gradle.kts`

**Action:** Add after line 164 (Room dependencies):

```kotlin
// Paging 3
implementation("androidx.paging:paging-runtime:3.3.5")
implementation("androidx.paging:paging-compose:3.3.5")
```

**Verification:**
- Run `./gradlew app:dependencies` to confirm versions resolved
- Sync project with Gradle files

---

### Step 1.2: Create TokenEntity

**File:** `app/src/main/java/com/otistran/flash_trade/data/local/entity/token-entity.kt`

**Content:**

```kotlin
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
```

**Rationale:**
- **Primary Key:** `address` (unique identifier, matches domain model)
- **Indices:**
  - `address` (unique): Fast lookups by token address
  - `total_tvl`: Optimizes ORDER BY total_tvl DESC queries
- **snake_case columns:** Room convention for SQL compatibility
- **cachedAt:** Timestamp for TTL-based invalidation

---

### Step 1.3: Create TokenRemoteKeysEntity

**File:** `app/src/main/java/com/otistran/flash_trade/data/local/entity/token-remote-keys-entity.kt`

**Content:**

```kotlin
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
```

**Rationale:**
- **Primary Key:** `token_address` (1:1 mapping with TokenEntity)
- **prevPage/nextPage:** Pagination state for RemoteMediator
  - `prevPage = null` indicates first page
  - `nextPage = null` indicates last page
- **createdAt:** TTL tracking for cache invalidation

---

### Step 1.4: Create TokenDao

**File:** `app/src/main/java/com/otistran/flash_trade/data/local/database/dao/token-dao.kt`

**Content:**

```kotlin
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

    /**
     * Atomic transaction: clear cache and keys.
     */
    @Transaction
    suspend fun clearAll() {
        clearTokens()
        clearRemoteKeys()
    }
}
```

**Rationale:**
- **pagingSource():** Returns Room's PagingSource for Paging 3 integration
- **getAllTokens(), searchTokens():** Backward compatibility with existing features
- **insertTokens():** UPSERT strategy (REPLACE on conflict) for cache updates
- **clearAll():** Atomic transaction to maintain consistency between tokens and keys
- **getStaleToken(), getOldestKeyCreationTime():** TTL-based cache invalidation helpers

---

### Step 1.5: Update FlashTradeDatabase

**File:** `app/src/main/java/com/otistran/flash_trade/data/local/database/FlashTradeDatabase.kt`

**Before:**
```kotlin
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlashTradeDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
```

**After:**
```kotlin
@Database(
    entities = [
        TransactionEntity::class,
        TokenEntity::class,
        TokenRemoteKeysEntity::class
    ],
    version = 2, // Incremented for schema change
    exportSchema = false
)
abstract class FlashTradeDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun tokenDao(): TokenDao
}
```

**Migration Note:**
Since this is early development and no production data exists:
- Use `.fallbackToDestructiveMigration()` in DatabaseModule (if configured)
- For production: create proper migration strategy

**File to Update (if exists):** `di/database-module.kt` or similar

Add destructive migration:
```kotlin
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): FlashTradeDatabase =
    Room.databaseBuilder(
        context,
        FlashTradeDatabase::class.java,
        "flash_trade_db"
    )
    .fallbackToDestructiveMigration() // OK for development
    .build()
```

---

## Mapper Creation

### Step 1.6: Create Token Entity Mapper

**File:** `app/src/main/java/com/otistran/flash_trade/data/mapper/token-entity-mapper.kt`

**Content:**

```kotlin
package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.data.local.entity.TokenEntity
import com.otistran.flash_trade.domain.model.Token

/**
 * Mapper between TokenEntity (Room) and Token (Domain).
 */
object TokenEntityMapper {

    /**
     * Convert TokenEntity to domain Token.
     */
    fun TokenEntity.toDomain(): Token = Token(
        address = address,
        name = name,
        symbol = symbol,
        decimals = decimals,
        logoUrl = logoUrl,
        isVerified = isVerified,
        isWhitelisted = isWhitelisted,
        isStable = isStable,
        isHoneypot = isHoneypot,
        totalTvl = totalTvl,
        poolCount = poolCount,
        cgkRank = cgkRank,
        cmcRank = cmcRank
    )

    /**
     * Convert list of entities to domain list.
     */
    fun List<TokenEntity>.toDomainList(): List<Token> = map { it.toDomain() }

    /**
     * Convert domain Token to TokenEntity.
     */
    fun Token.toEntity(): TokenEntity = TokenEntity(
        address = address,
        name = name,
        symbol = symbol,
        decimals = decimals,
        logoUrl = logoUrl,
        isVerified = isVerified,
        isWhitelisted = isWhitelisted,
        isStable = isStable,
        isHoneypot = isHoneypot,
        totalTvl = totalTvl,
        poolCount = poolCount,
        cgkRank = cgkRank,
        cmcRank = cmcRank,
        cachedAt = System.currentTimeMillis()
    )

    /**
     * Convert domain list to entity list.
     */
    fun List<Token>.toEntityList(): List<TokenEntity> = map { it.toEntity() }
}
```

---

## Verification Checklist

- [ ] Paging dependencies added to build.gradle.kts
- [ ] Gradle sync successful
- [ ] TokenEntity created with indices
- [ ] TokenRemoteKeysEntity created
- [ ] TokenDao created with all methods
- [ ] FlashTradeDatabase updated to version 2
- [ ] Database module configured with fallbackToDestructiveMigration
- [ ] TokenEntityMapper created
- [ ] Project builds without errors
- [ ] No compilation errors in Database class

---

## Files Created (Summary)

1. `data/local/entity/token-entity.kt` (~60 lines)
2. `data/local/entity/token-remote-keys-entity.kt` (~25 lines)
3. `data/local/database/dao/token-dao.kt` (~90 lines)
4. `data/mapper/token-entity-mapper.kt` (~60 lines)

**Total:** 4 new files, ~235 lines

---

## Files Modified (Summary)

1. `app/build.gradle.kts` (+2 lines)
2. `data/local/database/FlashTradeDatabase.kt` (+4 lines, version bump)

**Total:** 2 files modified, ~6 lines changed

---

## Next Phase

Proceed to **Phase 2: Paging Infrastructure** to implement TokenRemoteMediator.

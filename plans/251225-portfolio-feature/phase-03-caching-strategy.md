# Phase 03: Data Layer - Caching Strategy

**Parent Plan**: plan.md
**Dependencies**: Phase 02 (Portfolio Repository)
**Date**: 2025-12-25
**Priority**: High
**Status**: DONE (2025-12-25)

---

## Overview

Implement tiered caching with Room (transactions) and DataStore (balances) to achieve <500ms portfolio render from cache. Add TTL-based invalidation to balance fresh data vs. performance.

---

## Key Insights (from researcher-02-android-performance.md)

- **Tiered Caching**: Room for complex data (transactions), DataStore for simple key-value (balances)
- **TTL Strategy**: 30s for balances (volatile), 1h for transactions (historical)
- **Stale-While-Revalidate**: Show cached data immediately, refresh in background
- **Cache Hit Rate Target**: >80% to minimize API calls and stay under rate limits

---

## Requirements

1. Create Room entities for transactions (TransactionEntity) with cacheTimestamp
2. Create DAO with query methods (getAll, insert, deleteOld)
3. Extend UserPreferences DataStore with balance cache fields (ethBalance, cacheTimestamp)
4. Implement TTL validation logic (30s for balance, 1h for transactions)
5. Update PortfolioRepository to check cache before API calls

---

## Architecture

```
data/local/
├── database/
│   ├── FlashTradeDatabase.kt       # Room database
│   └── dao/
│       └── TransactionDao.kt
└── entity/
    └── TransactionEntity.kt

core/datastore/
└── UserPreferences.kt              # Add balance cache

data/repository/
└── PortfolioRepositoryImpl.kt      # Add cache checks
```

---

## Related Code Files

- `core/datastore/UserPreferences.kt` - Existing DataStore (extend with balance cache)
- `data/repository/PortfolioRepositoryImpl.kt` - Add cache logic (Phase 02)
- `domain/model/Settings.kt` - NetworkMode for cache key differentiation

---

## Implementation Steps

### 1. Create TransactionEntity

```kotlin
// data/local/entity/TransactionEntity.kt
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val hash: String,
    val chainId: Long,           // Separate cache per network
    val blockNumber: String,
    val timeStamp: Long,
    val fromAddress: String,
    val toAddress: String,
    val value: String,
    val gas: String,
    val gasPrice: String,
    val gasUsed: String,
    val isError: Boolean,
    val txType: String,          // Enum as string
    val tokenSymbol: String?,
    val tokenName: String?,
    val tokenDecimal: Int?,
    val contractAddress: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

// Mapper extensions
fun TransactionEntity.toDomain(): Transaction = Transaction(
    hash = hash,
    blockNumber = blockNumber,
    timeStamp = timeStamp,
    from = fromAddress,
    to = toAddress,
    value = value,
    gas = gas,
    gasPrice = gasPrice,
    gasUsed = gasUsed,
    isError = isError,
    txType = TransactionType.valueOf(txType),
    tokenSymbol = tokenSymbol,
    tokenName = tokenName,
    tokenDecimal = tokenDecimal,
    contractAddress = contractAddress
)

fun Transaction.toEntity(chainId: Long): TransactionEntity = TransactionEntity(
    hash = hash,
    chainId = chainId,
    blockNumber = blockNumber,
    timeStamp = timeStamp,
    fromAddress = from,
    toAddress = to,
    value = value,
    gas = gas,
    gasPrice = gasPrice,
    gasUsed = gasUsed,
    isError = isError,
    txType = txType.name,
    tokenSymbol = tokenSymbol,
    tokenName = tokenName,
    tokenDecimal = tokenDecimal,
    contractAddress = contractAddress
)
```

### 2. Create TransactionDao

```kotlin
// data/local/database/dao/TransactionDao.kt
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE chainId = :chainId ORDER BY timeStamp DESC LIMIT :limit")
    suspend fun getTransactions(chainId: Long, limit: Int = 100): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE chainId = :chainId AND cachedAt > :minTimestamp ORDER BY timeStamp DESC")
    suspend fun getRecentTransactions(chainId: Long, minTimestamp: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE chainId = :chainId AND cachedAt < :expiredTimestamp")
    suspend fun deleteExpired(chainId: Long, expiredTimestamp: Long)

    @Query("DELETE FROM transactions WHERE chainId = :chainId")
    suspend fun deleteAllForChain(chainId: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE chainId = :chainId")
    suspend fun getTransactionCount(chainId: Long): Int
}
```

### 3. Create FlashTradeDatabase

```kotlin
// data/local/database/FlashTradeDatabase.kt
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlashTradeDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}

// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FlashTradeDatabase {
        return Room.databaseBuilder(
            context,
            FlashTradeDatabase::class.java,
            "flash_trade.db"
        ).build()
    }

    @Provides
    fun provideTransactionDao(database: FlashTradeDatabase): TransactionDao {
        return database.transactionDao()
    }
}
```

### 4. Extend UserPreferences with Balance Cache

```kotlin
// core/datastore/UserPreferences.kt (add to existing class)

private object Keys {
    // ... existing keys ...

    // Portfolio cache keys (per network)
    val ETH_BALANCE_ETHEREUM = stringPreferencesKey("eth_balance_ethereum")
    val ETH_BALANCE_LINEA = stringPreferencesKey("eth_balance_linea")
    val BALANCE_CACHE_TS_ETHEREUM = longPreferencesKey("balance_cache_ts_ethereum")
    val BALANCE_CACHE_TS_LINEA = longPreferencesKey("balance_cache_ts_linea")
}

// Cache methods
suspend fun cacheBalance(chainId: Long, balance: Double) {
    context.dataStore.edit { prefs ->
        val balanceKey = getBalanceKey(chainId)
        val timestampKey = getBalanceTimestampKey(chainId)

        prefs[balanceKey] = balance.toString()
        prefs[timestampKey] = System.currentTimeMillis()
    }
}

suspend fun getCachedBalance(chainId: Long): CachedBalance? {
    val prefs = context.dataStore.data.first()
    val balanceKey = getBalanceKey(chainId)
    val timestampKey = getBalanceTimestampKey(chainId)

    val balance = prefs[balanceKey]?.toDoubleOrNull() ?: return null
    val timestamp = prefs[timestampKey] ?: return null

    return CachedBalance(balance, timestamp)
}

private fun getBalanceKey(chainId: Long): Preferences.Key<String> {
    return when (chainId) {
        1L -> Keys.ETH_BALANCE_ETHEREUM
        59144L -> Keys.ETH_BALANCE_LINEA
        else -> throw IllegalArgumentException("Unknown chainId: $chainId")
    }
}

private fun getBalanceTimestampKey(chainId: Long): Preferences.Key<Long> {
    return when (chainId) {
        1L -> Keys.BALANCE_CACHE_TS_ETHEREUM
        59144L -> Keys.BALANCE_CACHE_TS_LINEA
        else -> throw IllegalArgumentException("Unknown chainId: $chainId")
    }
}

data class CachedBalance(val balance: Double, val timestamp: Long) {
    fun isValid(ttlMs: Long): Boolean {
        return System.currentTimeMillis() - timestamp < ttlMs
    }
}
```

### 5. Update PortfolioRepositoryImpl with Cache Logic

```kotlin
class PortfolioRepositoryImpl @Inject constructor(
    private val etherscanApi: EtherscanApiService,
    private val transactionDao: TransactionDao,
    private val userPreferences: UserPreferences,
    @Named("etherscanApiKey") private val apiKey: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PortfolioRepository {

    companion object {
        private const val BALANCE_TTL_MS = 30_000L      // 30 seconds
        private const val TRANSACTION_TTL_MS = 3_600_000L  // 1 hour
    }

    override suspend fun getBalance(
        walletAddress: String,
        chainId: Long
    ): Result<Double> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cached = userPreferences.getCachedBalance(chainId)
            if (cached != null && cached.isValid(BALANCE_TTL_MS)) {
                return@withContext Result.success(cached.balance)
            }

            // Cache miss or expired - fetch from API
            val response = etherscanApi.getBalance(
                chainId = chainId,
                address = walletAddress,
                apiKey = apiKey
            )

            if (response.status == "1" && response.result != null) {
                val ethBalance = EtherscanMapper.weiToEth(response.result)

                // Update cache
                userPreferences.cacheBalance(chainId, ethBalance)

                Result.success(ethBalance)
            } else {
                // Return cached data if available, even if expired
                cached?.let { Result.success(it.balance) }
                    ?: Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            // On error, try to return cached data
            val cached = userPreferences.getCachedBalance(chainId)
            cached?.let { Result.success(it.balance) }
                ?: Result.failure(e)
        }
    }

    override suspend fun getTransactions(
        walletAddress: String,
        chainId: Long,
        page: Int,
        pageSize: Int
    ): Result<List<Transaction>> = withContext(ioDispatcher) {
        try {
            // Check cache (only for page 1)
            if (page == 1) {
                val minTimestamp = System.currentTimeMillis() - TRANSACTION_TTL_MS
                val cached = transactionDao.getRecentTransactions(chainId, minTimestamp)

                if (cached.isNotEmpty()) {
                    return@withContext Result.success(cached.map { it.toDomain() })
                }
            }

            // Fetch from API (parallel normal + token tx)
            val normalTxDeferred = async {
                etherscanApi.getTxList(
                    chainId = chainId,
                    address = walletAddress,
                    page = page,
                    offset = pageSize,
                    apiKey = apiKey
                )
            }

            val tokenTxDeferred = async {
                etherscanApi.getTokenTx(
                    chainId = chainId,
                    address = walletAddress,
                    page = page,
                    offset = pageSize,
                    apiKey = apiKey
                )
            }

            val normalTx = normalTxDeferred.await()
            val tokenTx = tokenTxDeferred.await()

            val allTransactions = mutableListOf<Transaction>()
            normalTx.result?.forEach { allTransactions.add(it.toDomain()) }
            tokenTx.result?.forEach { allTransactions.add(it.toDomain()) }

            val sorted = allTransactions.sortedByDescending { it.timeStamp }

            // Cache results (page 1 only)
            if (page == 1 && sorted.isNotEmpty()) {
                transactionDao.insertTransactions(sorted.map { it.toEntity(chainId) })

                // Clean expired cache
                val expiredTimestamp = System.currentTimeMillis() - TRANSACTION_TTL_MS
                transactionDao.deleteExpired(chainId, expiredTimestamp)
            }

            Result.success(sorted)
        } catch (e: Exception) {
            // On error, return any cached data
            val cached = transactionDao.getTransactions(chainId, pageSize)
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }
}
```

---

## Success Criteria

- [ ] Room database created and accessible via Hilt
- [ ] Balance cache returns valid data within 30s TTL
- [ ] Transaction cache returns data within 1h TTL
- [ ] Expired cache cleaned up automatically
- [ ] Network failures fall back to cached data
- [ ] Cache hit rate >80% in normal usage (verified via logs)

---

## Testing Strategy

```kotlin
@Test
fun `balance cache returns fresh data within TTL`() = runTest {
    val userPrefs = UserPreferences(context)

    // Cache balance
    userPrefs.cacheBalance(1L, 2.5)

    // Should return cached value immediately
    val cached = userPrefs.getCachedBalance(1L)
    assertNotNull(cached)
    assertEquals(2.5, cached.balance)
    assertTrue(cached.isValid(30_000L))
}

@Test
fun `expired cache triggers API refresh`() = runTest {
    // Mock expired cache (timestamp in past)
    val expiredCache = CachedBalance(1.0, System.currentTimeMillis() - 60_000)

    // Repository should call API when cache expired
    val result = repository.getBalance("0x123", 1L)

    verify(etherscanApi).getBalance(any(), any(), any())
}

@Test
fun `transaction cache returns data on network failure`() = runTest {
    // Populate cache
    val cachedTx = listOf(Transaction(...))
    transactionDao.insertTransactions(cachedTx.map { it.toEntity(1L) })

    // Mock API failure
    whenever(etherscanApi.getTxList(...)).thenThrow(IOException())

    // Should return cached data
    val result = repository.getTransactions("0x123", 1L)
    assertTrue(result.isSuccess)
    assertEquals(1, result.getOrNull()?.size)
}
```

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Cache grows unbounded (1000s of transactions) | Limit cache to 200 most recent tx per network, delete old |
| DataStore write delays block UI | All cache writes use withContext(Dispatchers.IO) |
| Room migration issues in production | Use fallbackToDestructiveMigration for v1 (no user data yet) |
| Network switch invalidates cache | Clear cache on network change event |

---

## Performance Optimizations

1. **Batch Writes**: Insert all transactions in single transaction via `@Transaction` annotation
2. **Index chainId**: Add index on `chainId` column for faster queries
3. **Limit Cache Size**: Keep only 200 most recent transactions per network

```kotlin
@Query("DELETE FROM transactions WHERE hash NOT IN (SELECT hash FROM transactions WHERE chainId = :chainId ORDER BY timeStamp DESC LIMIT 200)")
suspend fun trimCache(chainId: Long)
```

---

## Security Considerations

- **Cache Encryption**: Transactions contain wallet addresses (PII) - consider encrypting Room DB in future
- **Cache Invalidation**: Clear all cache on logout (call `transactionDao.deleteAll()`)
- **SQL Injection**: Use parameterized queries (already done via Room @Query)

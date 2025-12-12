# Phase 03: Data Layer - Local

## Context
- **Parent Plan:** [plan.md](plan.md)
- **Dependencies:** Phase 02 (domain entities)
- **Docs:** [system-architecture.md](../../docs/system-architecture.md)

## Overview
| Field | Value |
|-------|-------|
| Date | 2024-12-12 |
| Priority | High |
| Implementation Status | Pending |
| Review Status | Pending |

**Description:** Setup Room database, DAOs, entities, and DataStore preferences.

## Key Insights
- Room entities map to database tables (separate from domain entities)
- DAOs use Flow for reactive queries
- DataStore for user preferences

## Requirements
1. FlashTradeDatabase with Room
2. TradeEntity, WalletEntity for database
3. TradeDao, WalletDao for queries
4. UserPreferences with DataStore

## Architecture

```
data/local/
├── database/
│   └── FlashTradeDatabase.kt
├── entity/
│   ├── TradeEntity.kt
│   └── WalletEntity.kt
├── dao/
│   ├── TradeDao.kt
│   └── WalletDao.kt
└── datastore/
    └── UserPreferences.kt
```

## Implementation Steps

### Step 1: Create TradeEntity.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/local/entity/TradeEntity.kt`

```kotlin
package com.otistran.flash_trade.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.otistran.flash_trade.domain.model.Trade
import com.otistran.flash_trade.domain.model.TradeStatus

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey
    val id: String,
    val tokenAddress: String,
    val tokenSymbol: String,
    val amount: Double,
    val buyPriceUsd: Double,
    val sellPriceUsd: Double?,
    val status: String,
    val autoSellTimestamp: Long,
    val createdAt: Long,
    val walletAddress: String
) {
    fun toDomain(): Trade = Trade(
        id = id,
        tokenAddress = tokenAddress,
        tokenSymbol = tokenSymbol,
        amount = amount,
        buyPriceUsd = buyPriceUsd,
        sellPriceUsd = sellPriceUsd,
        status = TradeStatus.valueOf(status),
        autoSellTimestamp = autoSellTimestamp,
        createdAt = createdAt,
        walletAddress = walletAddress
    )

    companion object {
        fun fromDomain(trade: Trade): TradeEntity = TradeEntity(
            id = trade.id,
            tokenAddress = trade.tokenAddress,
            tokenSymbol = trade.tokenSymbol,
            amount = trade.amount,
            buyPriceUsd = trade.buyPriceUsd,
            sellPriceUsd = trade.sellPriceUsd,
            status = trade.status.name,
            autoSellTimestamp = trade.autoSellTimestamp,
            createdAt = trade.createdAt,
            walletAddress = trade.walletAddress
        )
    }
}
```

### Step 2: Create WalletEntity.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/local/entity/WalletEntity.kt`

```kotlin
package com.otistran.flash_trade.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.otistran.flash_trade.domain.model.Wallet

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey
    val address: String,
    val chainId: Int,
    val balance: Double,
    val createdAt: Long
) {
    fun toDomain(): Wallet = Wallet(
        address = address,
        chainId = chainId,
        balance = balance,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(wallet: Wallet): WalletEntity = WalletEntity(
            address = wallet.address,
            chainId = wallet.chainId,
            balance = wallet.balance,
            createdAt = wallet.createdAt
        )
    }
}
```

### Step 3: Create TradeDao.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/local/dao/TradeDao.kt`

```kotlin
package com.otistran.flash_trade.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.otistran.flash_trade.data.local.entity.TradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trade: TradeEntity)

    @Update
    suspend fun update(trade: TradeEntity)

    @Query("SELECT * FROM trades WHERE id = :id")
    suspend fun getById(id: String): TradeEntity?

    @Query("SELECT * FROM trades ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE status = 'PENDING' OR status = 'COMPLETED'")
    fun getPending(): Flow<List<TradeEntity>>

    @Query("UPDATE trades SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM trades WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

### Step 4: Create WalletDao.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/local/dao/WalletDao.kt`

```kotlin
package com.otistran.flash_trade.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.otistran.flash_trade.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: WalletEntity)

    @Query("SELECT * FROM wallets LIMIT 1")
    suspend fun getWallet(): WalletEntity?

    @Query("SELECT * FROM wallets LIMIT 1")
    fun observeWallet(): Flow<WalletEntity?>

    @Query("UPDATE wallets SET balance = :balance WHERE address = :address")
    suspend fun updateBalance(address: String, balance: Double)

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()
}
```

### Step 5: Create FlashTradeDatabase.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/local/database/FlashTradeDatabase.kt`

```kotlin
package com.otistran.flash_trade.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.otistran.flash_trade.data.local.dao.TradeDao
import com.otistran.flash_trade.data.local.dao.WalletDao
import com.otistran.flash_trade.data.local.entity.TradeEntity
import com.otistran.flash_trade.data.local.entity.WalletEntity

@Database(
    entities = [TradeEntity::class, WalletEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlashTradeDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun walletDao(): WalletDao

    companion object {
        const val DATABASE_NAME = "flash_trade_db"
    }
}
```

### Step 6: Create UserPreferences.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/local/datastore/UserPreferences.kt`

```kotlin
package com.otistran.flash_trade.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SELECTED_CHAIN_ID = intPreferencesKey("selected_chain_id")
        val AUTO_SELL_ENABLED = booleanPreferencesKey("auto_sell_enabled")
        val USER_ID = stringPreferencesKey("user_id")
        val WALLET_ADDRESS = stringPreferencesKey("wallet_address")
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_ONBOARDED] ?: false }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }
    val selectedChainId: Flow<Int> = context.dataStore.data.map { it[Keys.SELECTED_CHAIN_ID] ?: 1 }
    val autoSellEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_SELL_ENABLED] ?: true }
    val userId: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val walletAddress: Flow<String?> = context.dataStore.data.map { it[Keys.WALLET_ADDRESS] }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_ONBOARDED] = value }
    }

    suspend fun setBiometricEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = value }
    }

    suspend fun setSelectedChainId(chainId: Int) {
        context.dataStore.edit { it[Keys.SELECTED_CHAIN_ID] = chainId }
    }

    suspend fun setAutoSellEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SELL_ENABLED] = value }
    }

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { it[Keys.USER_ID] = userId }
    }

    suspend fun setWalletAddress(address: String) {
        context.dataStore.edit { it[Keys.WALLET_ADDRESS] = address }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
```

## Todo List
- [ ] Create data/local/entity/ directory
- [ ] Create TradeEntity.kt
- [ ] Create WalletEntity.kt
- [ ] Create data/local/dao/ directory
- [ ] Create TradeDao.kt
- [ ] Create WalletDao.kt
- [ ] Create data/local/database/ directory
- [ ] Create FlashTradeDatabase.kt
- [ ] Create data/local/datastore/ directory
- [ ] Create UserPreferences.kt
- [ ] Verify compilation

## Success Criteria
- [ ] All 6 files created
- [ ] Each file <200 lines
- [ ] Room entities have toDomain/fromDomain mappers
- [ ] DAOs return Flow for reactive queries

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Schema changes need migration | Medium | exportSchema=false for dev |

## Security Considerations
- No private keys stored in Room
- Sensitive data goes to encrypted storage (later)

## Next Steps
→ Phase 04: Data Layer - Remote

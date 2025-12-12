# Phase 05: DI Modules

## Context
- **Parent Plan:** [plan.md](plan.md)
- **Dependencies:** Phase 03, Phase 04 (database, API service)
- **Docs:** [code-standards.md](../../docs/code-standards.md)

## Overview
| Field | Value |
|-------|-------|
| Date | 2024-12-12 |
| Priority | High |
| Implementation Status | Pending |
| Review Status | Pending |

**Description:** Setup Hilt dependency injection with modules for app, network, database, and repositories.

## Key Insights
- @HiltAndroidApp required on Application class
- @AndroidEntryPoint on MainActivity
- Singleton scope for app-wide dependencies
- @Binds for interface implementations

## Requirements
1. FlashTradeApplication with @HiltAndroidApp
2. Update MainActivity with @AndroidEntryPoint
3. NetworkModule, DatabaseModule, RepositoryModule

## Architecture

```
di/
├── AppModule.kt           # App-wide dependencies
├── NetworkModule.kt       # Retrofit, API services
├── DatabaseModule.kt      # Room database, DAOs
└── RepositoryModule.kt    # Repository bindings

FlashTradeApplication.kt   # Root package
```

## Implementation Steps

### Step 1: Create FlashTradeApplication.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/FlashTradeApplication.kt`

```kotlin
package com.otistran.flash_trade

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FlashTradeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide components here
    }
}
```

### Step 2: Update MainActivity.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/MainActivity.kt`

```kotlin
package com.otistran.flash_trade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.otistran.flash_trade.ui.theme.FlashtradeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlashtradeTheme {
                // Navigation will be added here
            }
        }
    }
}
```

### Step 3: Create AppModule.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/di/AppModule.kt`

```kotlin
package com.otistran.flash_trade.di

import android.content.Context
import com.otistran.flash_trade.data.local.datastore.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences = UserPreferences(context)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
```

### Step 4: Create NetworkModule.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/di/NetworkModule.kt`

```kotlin
package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.remote.api.ApiConstants
import com.otistran.flash_trade.data.remote.kyber.KyberApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConstants.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ApiConstants.KYBER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideKyberApiService(retrofit: Retrofit): KyberApiService =
        retrofit.create(KyberApiService::class.java)
}
```

### Step 5: Create DatabaseModule.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/di/DatabaseModule.kt`

```kotlin
package com.otistran.flash_trade.di

import android.content.Context
import androidx.room.Room
import com.otistran.flash_trade.data.local.dao.TradeDao
import com.otistran.flash_trade.data.local.dao.WalletDao
import com.otistran.flash_trade.data.local.database.FlashTradeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FlashTradeDatabase = Room.databaseBuilder(
        context,
        FlashTradeDatabase::class.java,
        FlashTradeDatabase.DATABASE_NAME
    ).build()

    @Provides
    fun provideTradeDao(database: FlashTradeDatabase): TradeDao =
        database.tradeDao()

    @Provides
    fun provideWalletDao(database: FlashTradeDatabase): WalletDao =
        database.walletDao()
}
```

### Step 6: Create RepositoryModule.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/di/RepositoryModule.kt`

```kotlin
package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.TradeRepositoryImpl
import com.otistran.flash_trade.data.repository.WalletRepositoryImpl
import com.otistran.flash_trade.domain.repository.TradeRepository
import com.otistran.flash_trade.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTradeRepository(
        impl: TradeRepositoryImpl
    ): TradeRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        impl: WalletRepositoryImpl
    ): WalletRepository
}
```

### Step 7: Create Repository Implementations
**Path:** `app/src/main/java/com/otistran/flash_trade/data/repository/TradeRepositoryImpl.kt`

```kotlin
package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.local.dao.TradeDao
import com.otistran.flash_trade.data.local.entity.TradeEntity
import com.otistran.flash_trade.di.IoDispatcher
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.domain.model.Trade
import com.otistran.flash_trade.domain.model.TradeStatus
import com.otistran.flash_trade.domain.repository.TradeRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class TradeRepositoryImpl @Inject constructor(
    private val tradeDao: TradeDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TradeRepository {

    override suspend fun executeTrade(token: Token, amount: Double): Result<Trade> =
        withContext(ioDispatcher) {
            try {
                // TODO: Implement Kyber API trade execution
                val trade = Trade(
                    id = UUID.randomUUID().toString(),
                    tokenAddress = token.address,
                    tokenSymbol = token.symbol,
                    amount = amount,
                    buyPriceUsd = token.priceUsd * amount,
                    status = TradeStatus.PENDING,
                    autoSellTimestamp = System.currentTimeMillis() + (24 * 60 * 60 * 1000),
                    createdAt = System.currentTimeMillis(),
                    walletAddress = "" // TODO: Get from wallet
                )
                tradeDao.insert(TradeEntity.fromDomain(trade))
                Result.Success(trade)
            } catch (e: Exception) {
                Result.Error("Failed to execute trade: ${e.message}", e)
            }
        }

    override suspend fun getTradeById(id: String): Result<Trade> =
        withContext(ioDispatcher) {
            try {
                val entity = tradeDao.getById(id)
                if (entity != null) {
                    Result.Success(entity.toDomain())
                } else {
                    Result.Error("Trade not found")
                }
            } catch (e: Exception) {
                Result.Error("Failed to get trade: ${e.message}", e)
            }
        }

    override fun getAllTrades(): Flow<List<Trade>> =
        tradeDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getPendingTrades(): Flow<List<Trade>> =
        tradeDao.getPending().map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateTradeStatus(id: String, status: TradeStatus): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                tradeDao.updateStatus(id, status.name)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error("Failed to update trade: ${e.message}", e)
            }
        }
}
```

### Step 8: Create WalletRepositoryImpl.kt
**Path:** `app/src/main/java/com/otistran/flash_trade/data/repository/WalletRepositoryImpl.kt`

```kotlin
package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.local.dao.WalletDao
import com.otistran.flash_trade.data.local.entity.WalletEntity
import com.otistran.flash_trade.di.IoDispatcher
import com.otistran.flash_trade.domain.model.Wallet
import com.otistran.flash_trade.domain.repository.WalletRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WalletRepositoryImpl @Inject constructor(
    private val walletDao: WalletDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WalletRepository {

    override suspend fun createWallet(): Result<Wallet> =
        withContext(ioDispatcher) {
            try {
                // TODO: Implement Privy TEE wallet creation
                val wallet = Wallet(
                    address = "", // Will be populated by Privy
                    chainId = 1,
                    balance = 0.0,
                    createdAt = System.currentTimeMillis()
                )
                walletDao.insert(WalletEntity.fromDomain(wallet))
                Result.Success(wallet)
            } catch (e: Exception) {
                Result.Error("Failed to create wallet: ${e.message}", e)
            }
        }

    override suspend fun getWallet(): Result<Wallet?> =
        withContext(ioDispatcher) {
            try {
                Result.Success(walletDao.getWallet()?.toDomain())
            } catch (e: Exception) {
                Result.Error("Failed to get wallet: ${e.message}", e)
            }
        }

    override fun observeWallet(): Flow<Wallet?> =
        walletDao.observeWallet().map { it?.toDomain() }

    override suspend fun refreshBalance(): Result<Double> =
        withContext(ioDispatcher) {
            try {
                // TODO: Fetch balance from blockchain
                Result.Success(0.0)
            } catch (e: Exception) {
                Result.Error("Failed to refresh balance: ${e.message}", e)
            }
        }
}
```

### Step 9: Update AndroidManifest.xml
Add `android:name=".FlashTradeApplication"` to `<application>` tag.

## Todo List
- [ ] Create FlashTradeApplication.kt
- [ ] Update MainActivity.kt with @AndroidEntryPoint
- [ ] Create di/ directory
- [ ] Create AppModule.kt
- [ ] Create NetworkModule.kt
- [ ] Create DatabaseModule.kt
- [ ] Create RepositoryModule.kt
- [ ] Create data/repository/ directory
- [ ] Create TradeRepositoryImpl.kt
- [ ] Create WalletRepositoryImpl.kt
- [ ] Update AndroidManifest.xml
- [ ] Verify compilation

## Success Criteria
- [ ] All files created
- [ ] Each file <200 lines
- [ ] App compiles with Hilt
- [ ] No circular dependencies

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Hilt compilation errors | High | Check @InstallIn scopes |

## Security Considerations
- No hardcoded secrets in modules
- Use BuildConfig for API keys

## Next Steps
→ Phase 06: Presentation Base

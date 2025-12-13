# Phase 02: Data Layer

**Duration:** 20 minutes
**Dependencies:** Phase 01 (domain models)

## Objectives

Implement data persistence with DataStore and repository implementation.

## Files to Create/Modify

### 1. Extend UserPreferences.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/data/local/datastore/UserPreferences.kt`

**Changes:**
Add theme and network mode keys to existing UserPreferences:

```kotlin
// ADD to Keys object:
val THEME_MODE = stringPreferencesKey("theme_mode")
val NETWORK_MODE = stringPreferencesKey("network_mode")

// ADD flows:
val themeMode: Flow<String> = context.dataStore.data.map {
    it[Keys.THEME_MODE] ?: "DARK"
}
val networkMode: Flow<String> = context.dataStore.data.map {
    it[Keys.NETWORK_MODE] ?: "TESTNET"
}

// ADD setters:
suspend fun setThemeMode(mode: String) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode }
}

suspend fun setNetworkMode(mode: String) {
    context.dataStore.edit { it[Keys.NETWORK_MODE] = mode }
}
```

**Rationale:**
- Reuse existing UserPreferences (KISS principle)
- String storage for enums (safe for enum changes)
- Follows existing pattern in UserPreferences

### 2. SettingsRepositoryImpl.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/data/repository/SettingsRepositoryImpl.kt`

```kotlin
package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.local.datastore.UserPreferences
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.SettingsModel
import com.otistran.flash_trade.domain.model.ThemeMode
import com.otistran.flash_trade.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository using DataStore.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val userPreferences: UserPreferences
) : SettingsRepository {

    override fun observeSettings(): Flow<SettingsModel> {
        return combine(
            userPreferences.networkMode,
            userPreferences.themeMode,
            userPreferences.autoSellEnabled
        ) { network, theme, autoSell ->
            SettingsModel(
                networkMode = NetworkMode.valueOf(network),
                themeMode = ThemeMode.valueOf(theme),
                isAutoSellEnabled = autoSell
            )
        }
    }

    override suspend fun setNetworkMode(mode: NetworkMode) {
        userPreferences.setNetworkMode(mode.name)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        userPreferences.setThemeMode(mode.name)
    }

    override suspend fun setAutoSellEnabled(enabled: Boolean) {
        userPreferences.setAutoSellEnabled(enabled)
    }

    override suspend fun clearSettings() {
        userPreferences.clear()
    }
}
```

**Rationale:**
- Combines multiple preferences into single SettingsModel
- Enum name/value conversion for type safety
- Singleton scope for single source of truth
- Delegates to UserPreferences (single responsibility)

### 3. SettingsModule.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/di/SettingsModule.kt`

```kotlin
package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.SettingsRepositoryImpl
import com.otistran.flash_trade.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for settings dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
```

**Rationale:**
- Follows existing module pattern (see RepositoryModule.kt)
- SingletonComponent for app-wide settings
- Abstract Binds for interface-implementation binding

## Testing Considerations

### Unit Tests

**SettingsRepositoryImplTest.kt:**
```kotlin
@Test
fun observeSettings_combinesPreferences() = runTest {
    // Given
    coEvery { userPreferences.networkMode } returns flowOf("MAINNET")
    coEvery { userPreferences.themeMode } returns flowOf("LIGHT")
    coEvery { userPreferences.autoSellEnabled } returns flowOf(true)

    // When
    val settings = repository.observeSettings().first()

    // Then
    assertEquals(NetworkMode.MAINNET, settings.networkMode)
    assertEquals(ThemeMode.LIGHT, settings.themeMode)
    assertTrue(settings.isAutoSellEnabled)
}

@Test
fun setNetworkMode_updatesPreferences() = runTest {
    // When
    repository.setNetworkMode(NetworkMode.MAINNET)

    // Then
    coVerify { userPreferences.setNetworkMode("MAINNET") }
}
```

## Acceptance Criteria

- [ ] UserPreferences extended with theme/network keys
- [ ] SettingsRepositoryImpl implements interface
- [ ] SettingsModule provides repository binding
- [ ] observeSettings() combines all preferences
- [ ] All setters delegate to UserPreferences
- [ ] Files < 150 lines
- [ ] Compiles without errors

## Next Phase

Phase 03 will create presentation layer (ViewModel, Intent, State).

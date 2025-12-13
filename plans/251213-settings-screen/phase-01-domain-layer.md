# Phase 01: Domain Layer

**Duration:** 15 minutes
**Dependencies:** None

## Objectives

Create domain models and repository interface for settings management.

## Files to Create

### 1. SettingsModel.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/domain/model/SettingsModel.kt`

```kotlin
package com.otistran.flash_trade.domain.model

/**
 * Domain model for app settings.
 * Represents user preferences for network, theme, and other configs.
 */
data class SettingsModel(
    val networkMode: NetworkMode = NetworkMode.TESTNET,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isAutoSellEnabled: Boolean = true
)

/**
 * Network modes for trading.
 */
enum class NetworkMode {
    TESTNET,  // Safe for testing, no real money
    MAINNET;  // Production, real money transactions

    val displayName: String
        get() = when (this) {
            TESTNET -> "Testnet"
            MAINNET -> "Mainnet"
        }

    val isProduction: Boolean get() = this == MAINNET
}

/**
 * Theme modes for app UI.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;  // Follow system settings

    val displayName: String
        get() = when (this) {
            LIGHT -> "Light"
            DARK -> "Dark"
            SYSTEM -> "System"
        }
}
```

**Rationale:**
- Immutable data class follows domain entity pattern
- Enums provide type safety for modes
- Display names centralized in domain layer
- `isProduction` helper for safety checks

### 2. SettingsRepository.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/domain/repository/SettingsRepository.kt`

```kotlin
package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.SettingsModel
import com.otistran.flash_trade.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for settings data.
 * Follows clean architecture - domain layer contract.
 */
interface SettingsRepository {
    /**
     * Observes current settings. Emits on every change.
     */
    fun observeSettings(): Flow<SettingsModel>

    /**
     * Updates network mode.
     * @param mode New network mode
     */
    suspend fun setNetworkMode(mode: NetworkMode)

    /**
     * Updates theme mode.
     * @param mode New theme mode
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Updates auto-sell preference.
     * @param enabled Whether auto-sell is enabled
     */
    suspend fun setAutoSellEnabled(enabled: Boolean)

    /**
     * Clears all settings (for logout).
     */
    suspend fun clearSettings()
}
```

**Rationale:**
- Flow-based reactive API matches existing repositories
- Suspend functions for write operations
- Single source of truth via `observeSettings()`
- Clear separation of concerns

## Acceptance Criteria

- [ ] SettingsModel compiles with NetworkMode/ThemeMode enums
- [ ] SettingsRepository interface defined
- [ ] All files < 100 lines
- [ ] KDoc comments included
- [ ] Follows existing domain pattern (see Token.kt, TradeRepository.kt)

## Next Phase

Phase 02 will implement SettingsRepository using DataStore.

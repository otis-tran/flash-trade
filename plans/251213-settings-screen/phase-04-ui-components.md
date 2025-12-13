# Phase 04: UI Components

**Duration:** 30 minutes
**Dependencies:** Phase 03 (presentation layer)

## Objectives

Create Compose UI components with Kyber branding and Material3.

## Files to Create

### 1. SettingsScreen.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/SettingsScreen.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.otistran.flash_trade.presentation.common.LoadingIndicator
import com.otistran.flash_trade.presentation.settings.components.*

/**
 * Settings screen.
 * Displays network mode, theme mode, and logout options.
 */
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                SettingsSideEffect.NavigateToLogin -> onNavigateToLogin()
                is SettingsSideEffect.ShowToast -> {
                    // Show toast (use SnackbarHost in scaffold)
                }
            }
        }
    }

    // Show dialogs
    if (state.showMainnetConfirmDialog) {
        NetworkConfirmDialog(
            onConfirm = { viewModel.onIntent(SettingsIntent.ConfirmMainnetSwitch) },
            onDismiss = { viewModel.onIntent(SettingsIntent.CancelMainnetSwitch) }
        )
    }

    if (state.showLogoutConfirmSheet) {
        LogoutConfirmSheet(
            onConfirm = { viewModel.onIntent(SettingsIntent.ConfirmLogout) },
            onDismiss = { viewModel.onIntent(SettingsIntent.CancelLogout) }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (state.isLoading) {
            LoadingIndicator(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NetworkModeSection(
                    networkMode = state.networkMode,
                    onNetworkModeChange = { mode ->
                        viewModel.onIntent(SettingsIntent.ToggleNetworkMode(mode))
                    }
                )

                ThemeModeSection(
                    themeMode = state.themeMode,
                    onThemeModeChange = { mode ->
                        viewModel.onIntent(SettingsIntent.ToggleThemeMode(mode))
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                LogoutSection(
                    isLoggingOut = state.isLoggingOut,
                    onLogoutClick = {
                        viewModel.onIntent(SettingsIntent.RequestLogout)
                    }
                )
            }
        }
    }
}
```

### 2. NetworkModeSection.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/components/NetworkModeSection.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.otistran.flash_trade.domain.model.NetworkMode

/**
 * Network mode toggle section.
 * Shows mainnet/testnet switch with indicator chip.
 */
@Composable
fun NetworkModeSection(
    networkMode: NetworkMode,
    onNetworkModeChange: (NetworkMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Network Mode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (networkMode.isProduction) {
                            "Real transactions on mainnet"
                        } else {
                            "Safe testing environment"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = networkMode == NetworkMode.MAINNET,
                    onCheckedChange = { isMainnet ->
                        val newMode = if (isMainnet) {
                            NetworkMode.MAINNET
                        } else {
                            NetworkMode.TESTNET
                        }
                        onNetworkModeChange(newMode)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            // Network indicator chip
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = networkMode.displayName.uppercase(),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (networkMode.isProduction) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    labelColor = if (networkMode.isProduction) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            )
        }
    }
}
```

### 3. ThemeModeSection.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/components/ThemeModeSection.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.otistran.flash_trade.domain.model.ThemeMode

/**
 * Theme mode toggle section.
 * Shows dark/light mode switch with icon animation.
 */
@Composable
fun ThemeModeSection(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Crossfade(targetState = themeMode, label = "theme_icon") { mode ->
                    Icon(
                        imageVector = if (mode == ThemeMode.DARK) {
                            Icons.Default.DarkMode
                        } else {
                            Icons.Default.LightMode
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = themeMode.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = themeMode == ThemeMode.LIGHT,
                onCheckedChange = { isLight ->
                    val newMode = if (isLight) {
                        ThemeMode.LIGHT
                    } else {
                        ThemeMode.DARK
                    }
                    onThemeModeChange(newMode)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
```

### 4. LogoutSection.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/components/LogoutSection.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Logout section (danger zone).
 * Red-accented button with confirmation flow.
 */
@Composable
fun LogoutSection(
    isLoggingOut: Boolean,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )

            Button(
                onClick = onLogoutClick,
                enabled = !isLoggingOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        }
    }
}
```

### 5. NetworkConfirmDialog.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/components/NetworkConfirmDialog.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

/**
 * Confirmation dialog for switching to mainnet.
 * Emphasizes real money risk.
 */
@Composable
fun NetworkConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Switch to Mainnet?")
        },
        text = {
            Text(
                "You are about to switch to mainnet. " +
                "All transactions will use real money. " +
                "Make sure you understand the risks."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("I Understand")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
```

### 6. LogoutConfirmSheet.kt

**Location:** `app/src/main/java/com/otistran/flash_trade/presentation/settings/components/LogoutConfirmSheet.kt`

```kotlin
package com.otistran.flash_trade.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet confirmation for logout.
 * Mobile-friendly alternative to dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoutConfirmSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Logout",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Logging out will disconnect your wallet and clear all local data. " +
                      "You'll need to log in again to access your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}
```

## Visual Requirements

- **Touch Targets:** Minimum 48dp for all interactive elements
- **Spacing:** 16dp padding, 12-16dp between sections
- **Cards:** Rounded corners (default Material3), KyberNavyLight surface
- **Colors:**
  - Primary actions: KyberTeal
  - Danger: Error color (#FF6B6B)
  - Mainnet chip: ErrorContainer background
- **Typography:** Material3 default (titleMedium, bodySmall, etc.)

## Acceptance Criteria

- [ ] SettingsScreen scaffold with sections
- [ ] NetworkModeSection with toggle + chip
- [ ] ThemeModeSection with crossfade animation
- [ ] LogoutSection with danger zone styling
- [ ] NetworkConfirmDialog with warning icon
- [ ] LogoutConfirmSheet with bottom sheet
- [ ] All components < 150 lines
- [ ] 48dp min touch targets
- [ ] Compiles without errors

## Next Phase

Phase 05 will integrate into navigation.

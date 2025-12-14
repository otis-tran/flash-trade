package com.otistran.flash_trade.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otistran.flash_trade.presentation.common.LoadingIndicator
import com.otistran.flash_trade.presentation.settings.components.LogoutConfirmSheet
import com.otistran.flash_trade.presentation.settings.components.LogoutSection
import com.otistran.flash_trade.presentation.settings.components.NetworkConfirmDialog
import com.otistran.flash_trade.presentation.settings.components.NetworkModeSection
import com.otistran.flash_trade.presentation.settings.components.ThemeModeSection

/**
 * Settings screen.
 * Displays network mode, theme mode, and logout options.
 * No nested Scaffold - uses Surface (root Scaffold in MainActivity).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                SettingsSideEffect.NavigateToLogin -> onNavigateToLogin()
                is SettingsSideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar for Settings (no nested Scaffold)
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            if (state.isLoading) {
                LoadingIndicator(modifier = Modifier.fillMaxSize())
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
}

package com.otistran.flash_trade.presentation.feature.settings

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
import com.otistran.flash_trade.core.ui.components.LoadingIndicator
import com.otistran.flash_trade.presentation.feature.settings.components.LogoutConfirmSheet
import com.otistran.flash_trade.presentation.feature.settings.components.LogoutSection
import com.otistran.flash_trade.presentation.feature.settings.components.NetworkConfirmDialog
import com.otistran.flash_trade.presentation.feature.settings.components.NetworkModeSection
import com.otistran.flash_trade.presentation.feature.settings.components.ThemeModeSection

/**
 * Settings screen.
 * Displays network mode, theme mode, and logout options.
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
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToLogin -> onNavigateToLogin()
                is SettingsEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Dialogs
    if (state.showMainnetConfirmDialog) {
        NetworkConfirmDialog(
            onConfirm = { viewModel.onEvent(SettingsEvent.ConfirmMainnetSwitch) },
            onDismiss = { viewModel.onEvent(SettingsEvent.CancelMainnetSwitch) }
        )
    }

    if (state.showLogoutConfirmSheet) {
        LogoutConfirmSheet(
            onConfirm = { viewModel.onEvent(SettingsEvent.ConfirmLogout) },
            onDismiss = { viewModel.onEvent(SettingsEvent.CancelLogout) }
        )
    }

    // Main content
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar
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

            // Content
            if (state.isLoading) {
                LoadingIndicator(modifier = Modifier.fillMaxSize())
            } else {
                SettingsContent(
                    state = state,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NetworkModeSection(
            networkMode = state.networkMode,
            onNetworkModeChange = { mode ->
                onEvent(SettingsEvent.ChangeNetworkMode(mode))
            }
        )

        ThemeModeSection(
            themeMode = state.themeMode,
            onThemeModeChange = { mode ->
                onEvent(SettingsEvent.ChangeThemeMode(mode))
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        LogoutSection(
            isLoggingOut = state.isLoggingOut,
            onLogoutClick = {
                onEvent(SettingsEvent.RequestLogout)
            }
        )
    }
}
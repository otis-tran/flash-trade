package com.otistran.flash_trade.presentation.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.otistran.flash_trade.core.ui.components.NetworkSelectorBottomSheet
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.presentation.feature.settings.components.AutoSellSettingsSection
import com.otistran.flash_trade.presentation.feature.settings.components.LogoutConfirmSheet
import com.otistran.flash_trade.presentation.feature.settings.components.LogoutSection
import com.otistran.flash_trade.presentation.feature.settings.components.NetworkModeSection
import com.otistran.flash_trade.presentation.feature.settings.components.ThemeModeSection

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

    // Network Selector Bottom Sheet
    if (state.showNetworkSelector) {
        NetworkSelectorBottomSheet(
            networks = NetworkMode.entries,
            selectedNetwork = state.networkMode,
            onNetworkSelected = { network ->
                viewModel.onEvent(SettingsEvent.SelectNetwork(network))
            },
            onDismiss = {
                viewModel.onEvent(SettingsEvent.ToggleNetworkSelector)
            }
        )
    }

    // Logout Confirmation
    if (state.showLogoutConfirmSheet) {
        LogoutConfirmSheet(
            onConfirm = { viewModel.onEvent(SettingsEvent.ConfirmLogout) },
            onDismiss = { viewModel.onEvent(SettingsEvent.CancelLogout) }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NetworkModeSection(
            networkMode = state.networkMode,
            onClick = { onEvent(SettingsEvent.ToggleNetworkSelector) }
        )

        ThemeModeSection(
            themeMode = state.themeMode,
            onThemeModeChange = { mode ->
                onEvent(SettingsEvent.ChangeThemeMode(mode))
            }
        )

        AutoSellSettingsSection(
            state = state,
            onEvent = onEvent
        )

        Spacer(modifier = Modifier.height(32.dp))

        LogoutSection(
            isLoggingOut = state.isLoggingOut,
            onLogoutClick = { onEvent(SettingsEvent.RequestLogout) }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}
package com.otistran.flash_trade.presentation.feature.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.otistran.flash_trade.presentation.feature.settings.SettingsEvent
import com.otistran.flash_trade.presentation.feature.settings.SettingsState
import kotlinx.coroutines.delay

private val PRESET_DURATIONS = listOf(1, 2, 3, 1440)
private const val DEFAULT_DURATION = 1

/**
 * Auto-sell settings section with toggle and duration picker.
 */
@Composable
fun AutoSellSettingsSection(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    // Track save indicator visibility
    var showSaveIndicator by remember { mutableStateOf(false) }
    var lastSavedDuration by remember { mutableStateOf(state.autoSellDurationMinutes) }

    // Local state for custom input to preserve cursor position
    var customInput by remember { mutableStateOf(TextFieldValue("")) }
    var inputError by remember { mutableStateOf<String?>(null) }

    // Sync customInput with state when preset is selected or on initial load
    LaunchedEffect(state.autoSellDurationMinutes) {
        if (state.autoSellDurationMinutes !in PRESET_DURATIONS) {
            // Only update if text doesn't match (avoid cursor reset during typing)
            if (customInput.text != state.autoSellDurationMinutes.toString()) {
                val text = state.autoSellDurationMinutes.toString()
                customInput = TextFieldValue(text = text, selection = TextRange(text.length))
            }
        } else {
            customInput = TextFieldValue("")
        }
        inputError = null // Clear error when state changes
    }

    // Trigger save animation when duration changes
    LaunchedEffect(state.autoSellDurationMinutes) {
        if (state.autoSellDurationMinutes != lastSavedDuration) {
            lastSavedDuration = state.autoSellDurationMinutes
            showSaveIndicator = true
            delay(1500)
            showSaveIndicator = false
        }
    }

    // Validate and submit custom input
    fun submitCustomInput() {
        val text = customInput.text.trim()
        if (text.isEmpty()) {
            // Empty input - default to 1m
            onEvent(SettingsEvent.SetAutoSellDuration(DEFAULT_DURATION))
            focusManager.clearFocus()
            inputError = null
            return
        }
        
        val minutes = text.toIntOrNull()
        when {
            minutes == null -> {
                inputError = "Please enter a valid number"
            }
            minutes < 1 -> {
                inputError = "Minimum is 1 minute"
            }
            minutes > 1440 -> {
                inputError = "Maximum is 1440 minutes (24h)"
            }
            else -> {
                onEvent(SettingsEvent.SetAutoSellDuration(minutes))
                focusManager.clearFocus()
                inputError = null
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with save indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-Sell",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedVisibility(visible = showSaveIndicator) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Saved",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Enable Auto-Sell",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Automatically sell tokens after purchase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.isAutoSellEnabled,
                    onCheckedChange = { onEvent(SettingsEvent.SetAutoSellEnabled(it)) }
                )
            }

            // Duration picker (only shown when enabled)
            if (state.isAutoSellEnabled) {
                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sell After",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Preset buttons: 1m, 2m, 3m, 24h
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_DURATIONS.forEach { minutes ->
                            val label = when (minutes) {
                                1 -> "1m"
                                2 -> "2m"
                                3 -> "3m"
                                1440 -> "24h"
                                else -> "${minutes}m"
                            }
                            val isSelected = state.autoSellDurationMinutes == minutes
                            FilterChip(
                                selected = isSelected,
                                onClick = { 
                                    onEvent(SettingsEvent.SetAutoSellDuration(minutes))
                                    focusManager.clearFocus()
                                },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    // Custom input with validation
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { value ->
                            // Filter to only allow digits
                            val filtered = value.copy(text = value.text.filter { it.isDigit() })
                            customInput = filtered
                            inputError = null // Clear error while typing
                            
                            // Auto-save valid values while typing
                            filtered.text.toIntOrNull()?.let { minutes ->
                                if (minutes in 1..1440) {
                                    onEvent(SettingsEvent.SetAutoSellDuration(minutes))
                                }
                            }
                        },
                        label = { Text("Custom (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = inputError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitCustomInput() }
                        ),
                        supportingText = {
                            inputError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } ?: Text("1-1440 minutes (max 24h)")
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

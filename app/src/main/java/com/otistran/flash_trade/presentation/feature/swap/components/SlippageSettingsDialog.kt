package com.otistran.flash_trade.presentation.feature.swap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private val PRESET_VALUES = listOf(0.1, 0.5, 1.0)

@Composable
fun SlippageSettingsDialog(
    currentSlippage: Double,
    onSlippageSelected: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var customValue by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<Double?>(
        if (currentSlippage in PRESET_VALUES) currentSlippage else null
    ) }
    var showHighSlippageWarning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initialize custom value if current is not a preset
    LaunchedEffect(currentSlippage) {
        if (currentSlippage !in PRESET_VALUES) {
            customValue = currentSlippage.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Title
                Text(
                    text = "Slippage Tolerance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = "Maximum price change you're willing to accept",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Preset chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_VALUES.forEach { preset ->
                        SlippageChip(
                            value = preset,
                            isSelected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                customValue = ""
                                errorMessage = null
                                showHighSlippageWarning = preset > 5.0
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom input
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (errorMessage != null)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = customValue,
                            onValueChange = { value ->
                                // Validate input
                                if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    customValue = value
                                    selectedPreset = null

                                    val parsed = value.toDoubleOrNull()
                                    when {
                                        parsed == null && value.isNotEmpty() -> {
                                            errorMessage = "Invalid number"
                                        }
                                        parsed != null && parsed < 0.01 -> {
                                            errorMessage = "Min 0.01%"
                                        }
                                        parsed != null && parsed > 50 -> {
                                            errorMessage = "Max 50%"
                                        }
                                        else -> {
                                            errorMessage = null
                                            showHighSlippageWarning = (parsed ?: 0.0) > 5.0
                                        }
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (customValue.isEmpty()) {
                                            Text(
                                                text = "0.5",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                    Text(
                                        text = "%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // High slippage warning
                if (showHighSlippageWarning && errorMessage == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚠️ High slippage may result in unfavorable trades",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val finalValue = selectedPreset
                                ?: customValue.toDoubleOrNull()
                                ?: 0.5

                            if (finalValue in 0.01..50.0) {
                                onSlippageSelected(finalValue)
                            }
                        },
                        enabled = errorMessage == null && (selectedPreset != null || customValue.isNotEmpty())
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun SlippageChip(
    value: Double,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = "${value}%",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

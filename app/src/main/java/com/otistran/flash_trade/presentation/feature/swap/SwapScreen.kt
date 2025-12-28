package com.otistran.flash_trade.presentation.feature.swap

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.otistran.flash_trade.domain.model.Token

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(
    tokenAddress: String = "",
    viewModel: SwapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToTxDetails: (String) -> Unit = {},
    onOpenTokenSelector: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Load token from address param
    LaunchedEffect(tokenAddress) {
        if (tokenAddress.isNotBlank()) {
            viewModel.onEvent(SwapEvent.LoadToken(tokenAddress))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SwapEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SwapEffect.NavigateToTxDetails -> {
                    onNavigateToTxDetails(effect.txHash)
                }
                SwapEffect.NavigateBack -> onNavigateBack()
                is SwapEffect.OpenTokenSelector -> onOpenTokenSelector(effect.isFrom)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Swap") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // From Token Card
            TokenInputCard(
                label = "From",
                token = state.tokenFrom,
                amount = state.amount,
                balance = state.userBalance,
                onAmountChange = { viewModel.onEvent(SwapEvent.SetAmount(it)) },
                onMaxClick = { viewModel.onEvent(SwapEvent.SetMaxAmount) },
                onTokenClick = { onOpenTokenSelector(true) }
            )

            // Swap Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.onEvent(SwapEvent.SwapTokens) },
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = "Swap tokens",
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // To Token Card
            TokenOutputCard(
                label = "To",
                token = state.tokenTo,
                estimatedOutput = state.estimatedOutput,
                isLoading = state.isLoadingQuote,
                onTokenClick = { onOpenTokenSelector(false) }
            )

            // Quote Card
            if (state.quote != null || state.isLoadingQuote) {
                QuoteInfoCard(
                    rate = state.displayRate,
                    gasUsd = state.quote?.gasUsd ?: "-",
                    isExpired = state.quoteExpired,
                    isLoading = state.isLoadingQuote,
                    onRefresh = { viewModel.onEvent(SwapEvent.RefreshQuote) }
                )
            }

            // Error Banner
            if (state.error != null) {
                ErrorBanner(
                    message = state.error!!,
                    onDismiss = { viewModel.onEvent(SwapEvent.DismissError) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Swap Button
            Button(
                onClick = { viewModel.onEvent(SwapEvent.ExecuteSwap) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state.canSwap,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when {
                            state.tokenFrom == null -> "Select token"
                            state.tokenTo == null -> "Select token"
                            state.amount.isEmpty() -> "Enter amount"
                            state.quote == null && !state.isLoadingQuote -> "Get quote"
                            state.quoteExpired -> "Quote expired"
                            else -> "Swap"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenInputCard(
    label: String,
    token: Token?,
    amount: String,
    balance: String,
    onAmountChange: (String) -> Unit,
    onMaxClick: () -> Unit,
    onTokenClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(
                    "Balance: $balance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TokenSelector(token = token, onClick = onTokenClick)
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = onMaxClick) {
                            Text("MAX", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TokenOutputCard(
    label: String,
    token: Token?,
    estimatedOutput: String,
    isLoading: Boolean,
    onTokenClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TokenSelector(token = token, onClick = onTokenClick)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = estimatedOutput,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenSelector(token: Token?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (token != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(token.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = token.name,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(token.symbol, fontWeight = FontWeight.Medium)
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline)
                )
                Text("Select", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuoteInfoCard(
    rate: String,
    gasUsd: String,
    isExpired: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rate", style = MaterialTheme.typography.labelSmall)
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(rate, style = MaterialTheme.typography.bodySmall)
                        if (isExpired) {
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    "Refresh",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Est. Gas", style = MaterialTheme.typography.labelSmall)
                Text("$$gasUsd", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

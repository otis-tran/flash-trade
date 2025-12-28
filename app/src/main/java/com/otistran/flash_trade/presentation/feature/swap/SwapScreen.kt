package com.otistran.flash_trade.presentation.feature.swap

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.otistran.flash_trade.domain.model.Token
import com.otistran.flash_trade.ui.theme.KyberTeal

// Semantic colors
private val WarningOrange = Color(0xFFFFB800)
private val ErrorRed = Color(0xFFFF5252)
private val SuccessGreen = Color(0xFF00C853)

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
            CenterAlignedTopAppBar(
                title = { Text("Swap") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(SwapEvent.ToggleSlippageSettings) }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (state.showSlippageSettings) KyberTeal
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Slippage Settings Card (Expandable)
            AnimatedVisibility(
                visible = state.showSlippageSettings,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SlippageSettingsCard(
                    currentSlippage = state.slippageTolerance,
                    onSlippageChange = { viewModel.onEvent(SwapEvent.SetSlippage(it)) }
                )
            }

            // From Token Card (Glass effect)
            GlassTokenInputCard(
                label = "You Pay",
                token = state.tokenFrom,
                amount = state.amount,
                balance = state.userBalance,
                onAmountChange = { viewModel.onEvent(SwapEvent.SetAmount(it)) },
                onMaxClick = { viewModel.onEvent(SwapEvent.SetMaxAmount) },
                onTokenClick = { onOpenTokenSelector(true) }
            )

            // Swap Direction Button
            SwapDirectionButton(
                onClick = { viewModel.onEvent(SwapEvent.SwapTokens) },
                isLoading = state.isLoadingQuote
            )

            // To Token Card (Glass effect)
            GlassTokenOutputCard(
                label = "You Receive",
                token = state.tokenTo,
                estimatedOutput = state.estimatedOutput,
                isLoading = state.isLoadingQuote,
                onTokenClick = { onOpenTokenSelector(false) }
            )

            // Quote Info Card
            if (state.quote != null || state.isLoadingQuote) {
                QuoteDetailsCard(
                    rate = state.displayRate,
                    gasUsd = state.quote?.gasUsd ?: "-",
                    priceImpact = state.formattedPriceImpact,
                    isPriceImpactHigh = state.isPriceImpactHigh,
                    isPriceImpactVeryHigh = state.isPriceImpactVeryHigh,
                    slippage = state.formattedSlippage,
                    minimumReceived = state.minimumReceived,
                    isExpired = state.quoteExpired,
                    isLoading = state.isLoadingQuote,
                    onRefresh = { viewModel.onEvent(SwapEvent.RefreshQuote) }
                )
            }

            // Error Banner
            AnimatedVisibility(visible = state.error != null) {
                ErrorBanner(
                    message = state.error ?: "",
                    onDismiss = { viewModel.onEvent(SwapEvent.DismissError) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Swap Button
            SwapButton(
                state = state,
                onClick = { viewModel.onEvent(SwapEvent.ExecuteSwap) }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SlippageSettingsCard(
    currentSlippage: Double,
    onSlippageChange: (Double) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slippage Tolerance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SlippageOption.entries.forEach { option ->
                    FilterChip(
                        selected = currentSlippage == option.value,
                        onClick = { onSlippageChange(option.value) },
                        label = { Text(option.label) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KyberTeal.copy(alpha = 0.2f),
                            selectedLabelColor = KyberTeal
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = currentSlippage == option.value,
                            selectedBorderColor = KyberTeal
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTokenInputCard(
    label: String,
    token: Token?,
    amount: String,
    balance: String,
    onAmountChange: (String) -> Unit,
    onMaxClick: () -> Unit,
    onTokenClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Balance: $balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onMaxClick),
                        color = KyberTeal.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "MAX",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = KyberTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Token Selector
                TokenSelectorButton(token = token, onClick = onTokenClick)

                // Amount Input
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (amount.isEmpty()) {
                        Text(
                            text = "0.0",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                    BasicTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(KyberTeal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTokenOutputCard(
    label: String,
    token: Token?,
    estimatedOutput: String,
    isLoading: Boolean,
    onTokenClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TokenSelectorButton(token = token, onClick = onTokenClick)

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                        },
                        label = "output"
                    ) { loading ->
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = KyberTeal
                            )
                        } else {
                            Text(
                                text = estimatedOutput,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.End,
                                color = if (estimatedOutput == "-")
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenSelectorButton(
    token: Token?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                Text(
                    text = token.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline)
                )
                Text(
                    text = "Select",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwapDirectionButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    val rotation by animateFloatAsState(
        targetValue = if (isLoading) 360f else 0f,
        animationSpec = tween(300),
        label = "rotate"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.SwapVert,
                    contentDescription = "Swap tokens",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun QuoteDetailsCard(
    rate: String,
    gasUsd: String,
    priceImpact: String,
    isPriceImpactHigh: Boolean,
    isPriceImpactVeryHigh: Boolean,
    slippage: String,
    minimumReceived: String,
    isExpired: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isExpired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            isPriceImpactVeryHigh -> ErrorRed.copy(alpha = 0.1f)
            isPriceImpactHigh -> WarningOrange.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        label = "bgColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Rate Row
            QuoteRow(
                label = "Rate",
                value = rate,
                isLoading = isLoading,
                trailing = if (isExpired) {
                    {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(20.dp)) {
                            Icon(
                                Icons.Default.Refresh,
                                "Refresh",
                                modifier = Modifier.size(16.dp),
                                tint = ErrorRed
                            )
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price Impact Row
            QuoteRow(
                label = "Price Impact",
                value = priceImpact,
                valueColor = when {
                    isPriceImpactVeryHigh -> ErrorRed
                    isPriceImpactHigh -> WarningOrange
                    else -> SuccessGreen
                },
                trailing = if (isPriceImpactHigh) {
                    {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "High impact",
                            modifier = Modifier.size(14.dp),
                            tint = if (isPriceImpactVeryHigh) ErrorRed else WarningOrange
                        )
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Slippage Row
            QuoteRow(label = "Max. Slippage", value = slippage)

            Spacer(modifier = Modifier.height(8.dp))

            // Minimum Received Row
            QuoteRow(label = "Min. Received", value = minimumReceived)

            Spacer(modifier = Modifier.height(8.dp))

            // Gas Row
            QuoteRow(label = "Est. Gas", value = "$$gasUsd")
        }
    }
}

@Composable
private fun QuoteRow(
    label: String,
    value: String,
    isLoading: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = KyberTeal
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = valueColor
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SwapButton(
    state: SwapState,
    onClick: () -> Unit
) {
    val buttonText = when {
        state.tokenFrom == null -> "Select token"
        state.tokenTo == null -> "Select token"
        state.amount.isEmpty() -> "Enter amount"
        state.quote == null && !state.isLoadingQuote -> "Get quote"
        state.quoteExpired -> "Quote expired - Refresh"
        state.isPriceImpactVeryHigh -> "Swap Anyway (High Impact)"
        else -> "Swap"
    }

    val buttonColor = when {
        state.isPriceImpactVeryHigh && state.canSwap -> ErrorRed
        else -> KyberTeal
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = state.canSwap,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (state.isExecuting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==================== Glassmorphism Component ====================

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        content()
    }
}

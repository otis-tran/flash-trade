package com.otistran.flash_trade.presentation.feature.swap

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.otistran.flash_trade.presentation.feature.swap.components.QuoteCountdownIndicator
import com.otistran.flash_trade.presentation.feature.swap.components.QuoteDetailsBox
import com.otistran.flash_trade.presentation.feature.swap.components.SlippageSettingsDialog
import com.otistran.flash_trade.presentation.feature.swap.components.SwapButton
import com.otistran.flash_trade.presentation.feature.swap.components.SwapTriggerButton
import com.otistran.flash_trade.presentation.feature.swap.components.TokenInputSection
import com.otistran.flash_trade.presentation.feature.swap.components.TokenSelectorBottomSheet

private val ErrorRed = Color(0xFFFF5252)

/**
 * Swap Screen - matches design doc Section 2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(
    modifier: Modifier = Modifier,
    viewModel: SwapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tokens = viewModel.tokensFlow.collectAsLazyPagingItems()
    val context = LocalContext.current

    // Refresh balances when screen becomes visible (e.g., after returning from tx details)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onEvent(SwapEvent.RefreshBalances)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SwapEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SwapEffect.NavigateToTxDetails -> {
                    Toast.makeText(context, "Transaction sent!", Toast.LENGTH_SHORT).show()
                    onNavigateToHome()
                }
                SwapEffect.NavigateBack -> onNavigateBack()
                SwapEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    Scaffold(
        topBar = {
            SwapTopBar(
                onBack = { viewModel.onEvent(SwapEvent.NavigateBack) },
                onCancel = { viewModel.onEvent(SwapEvent.Cancel) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            TokenInputSection(
                label = "Sell",
                token = state.sellToken,
                amount = state.sellAmount,
                amountUsd = state.sellAmountUsd,
                tokenPrice = state.sellTokenPrice,
                isLoading = false,
                maxSwapAmount = state.maxSwapAmount,
                onAmountChange = { viewModel.onEvent(SwapEvent.SetSellAmount(it)) },
                onTokenClick = { viewModel.onEvent(SwapEvent.OpenSellTokenSelector) }
            )

            SwapTriggerButton(onClick = { viewModel.onEvent(SwapEvent.SwapTokens) })

            TokenInputSection(
                label = "Buy",
                token = state.buyToken,
                amount = state.buyAmount,
                amountUsd = state.buyAmountUsd,
                tokenPrice = state.buyTokenPrice,
                isLoading = state.isLoadingQuote,
                readOnly = true,
                onAmountChange = { },
                onTokenClick = { viewModel.onEvent(SwapEvent.OpenBuyTokenSelector) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = state.hasValidQuote, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    QuoteDetailsBox(
                        exchangeRate = state.displayExchangeRate,
                        networkFee = state.displayNetworkFee,
                        priceImpact = state.displayPriceImpact,
                        slippage = state.slippage,
                        onSlippageClick = { viewModel.onEvent(SwapEvent.OpenSlippageDialog) }
                    )
                    
                    QuoteCountdownIndicator(
                        secondsRemaining = state.quoteExpiresInSeconds,
                        isVisible = state.showQuoteCountdown,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(visible = state.hasValidQuote) {
                Text(
                    text = "Includes network fee. Price may change during execution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            SwapButton(
                text = state.ctaButtonText,
                enabled = state.ctaButtonEnabled,
                isLoading = state.isExecuting,
                onClick = { viewModel.onEvent(SwapEvent.ExecuteSwap) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Token Selector Bottom Sheet
    TokenSelectorBottomSheet(
        isVisible = state.showTokenSelector,
        tokens = tokens,
        searchQuery = state.tokenSearchQuery,
        onSearchQueryChange = { viewModel.onEvent(SwapEvent.SearchTokens(it)) },
        onTokenSelected = { token ->
            timber.log.Timber.d("SwapScreen: Token selected from list - symbol=${token.symbol} address=${token.address} decimals=${token.decimals}")
            val swapToken = token.toSwapToken()
            timber.log.Timber.d("SwapScreen: Converted to SwapToken - symbol=${swapToken.symbol} address=${swapToken.address} decimals=${swapToken.decimals}")
            if (state.isSelectingSellToken) {
                viewModel.onEvent(SwapEvent.SelectSellToken(swapToken))
            } else {
                viewModel.onEvent(SwapEvent.SelectBuyToken(swapToken))
            }
        },
        onDismiss = { viewModel.onEvent(SwapEvent.CloseTokenSelector) },
        showSafeTokensOnly = state.showSafeTokensOnly,
        onToggleFilter = { viewModel.onEvent(SwapEvent.ToggleTokenFilter) }
    )

    // Slippage Settings Dialog
    if (state.showSlippageDialog) {
        SlippageSettingsDialog(
            currentSlippage = state.slippage,
            onSlippageSelected = { slippage ->
                viewModel.onEvent(SwapEvent.SetSlippage(slippage))
            },
            onDismiss = { viewModel.onEvent(SwapEvent.CloseSlippageDialog) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapTopBar(onBack: () -> Unit, onCancel: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = "Swap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            TextButton(onClick = onCancel) {
                Text(text = "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

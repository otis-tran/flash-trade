package com.otistran.flash_trade.presentation.feature.portfolio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otistran.flash_trade.presentation.feature.portfolio.components.AssetTabsSection
import com.otistran.flash_trade.presentation.feature.portfolio.components.BalanceSection
import com.otistran.flash_trade.presentation.feature.portfolio.components.EmptyNFTsPlaceholder
import com.otistran.flash_trade.presentation.feature.portfolio.components.EmptyTokensPlaceholder
import com.otistran.flash_trade.presentation.feature.portfolio.components.QRCodeView
import com.otistran.flash_trade.presentation.feature.portfolio.components.QuickActionsGrid
import com.otistran.flash_trade.presentation.feature.portfolio.components.TokenHoldingItem
import com.otistran.flash_trade.presentation.feature.portfolio.components.TokenHoldingItemShimmer
import com.otistran.flash_trade.presentation.feature.portfolio.components.PortfolioHeaderSection

/**
 * Home Screen (Portfolio) - matches design doc Section 1.
 *
 * Layout:
 * - Header: Wallet address + Copy/QR icons
 * - Balance: Large font, left-aligned, PnL indicator
 * - Quick Actions: 4-column grid (Buy, Swap, Send, Receive)
 * - Tabs: Token/NFT with network filter
 * - Asset List: Token holdings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    modifier: Modifier = Modifier,
    onNavigateToSwap: () -> Unit = {},
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PortfolioEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is PortfolioEffect.CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Wallet Address", effect.text)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                }
                is PortfolioEffect.NavigateToSwap -> {
                    onNavigateToSwap()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeContent(
                state = state,
                onEvent = viewModel::onEvent
            )
        }

        // QR Code Bottom Sheet
        if (state.showQRSheet && state.walletAddress != null) {
            ReceiveQRBottomSheet(
                address = state.walletAddress!!,
                chainId = state.currentNetwork.chainId,
                networkName = state.currentNetwork.symbol,
                onDismiss = { viewModel.onEvent(PortfolioEvent.HideQRSheet) },
                onCopyAddress = { viewModel.onEvent(PortfolioEvent.CopyWalletAddress) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: PortfolioState,
    onEvent: (PortfolioEvent) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(PortfolioEvent.LoadPortfolio) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                PortfolioHeaderSection(
                    hasWallet = state.hasWallet,
                    displayShortAddress = state.displayShortAddress,
                    onCopyAddress = { onEvent(PortfolioEvent.CopyWalletAddress) },
                    onReceiveClick = { onEvent(PortfolioEvent.OnReceiveClick) }
                )
            }
            item(key = "balance") {
                BalanceSection(
                    state = state,
                    isLoading = state.isLoadingTokens
                )
            }
            item(key = "quick_actions") {
                QuickActionsGrid(onEvent = onEvent)
            }
            item(key = "tabs") {
                AssetTabsSection(state = state, onEvent = onEvent)
            }

            if (state.selectedAssetTab == AssetTab.TOKEN) {
                if (state.isLoadingTokens) {
                    items(1) {
                        TokenHoldingItemShimmer()
                    }
                } else if (state.tokens.isEmpty()) {
                    item(key = "empty_tokens") { EmptyTokensPlaceholder() }
                } else {
                    items(items = state.tokens, key = { it.id }) { token ->
                        TokenHoldingItem(token = token, network = state.currentNetwork)
                    }
                }
            } else {
                item(key = "empty_nfts") { EmptyNFTsPlaceholder() }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiveQRBottomSheet(
    address: String,
    chainId: Long,
    networkName: String,
    onDismiss: () -> Unit,
    onCopyAddress: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        QRCodeView(
            address = address,
            chainId = chainId,
            networkName = networkName,
            onCopyAddress = onCopyAddress
        )
    }
}

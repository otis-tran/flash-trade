package com.otistran.flash_trade.presentation.portfolio

import androidx.compose.runtime.Stable
import com.otistran.flash_trade.presentation.base.MviState

/**
 * UI state for portfolio screen.
 */
@Stable
data class PortfolioState(
    val displayName: String? = null,
    val userEmail: String? = null,
    val walletAddress: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
) : MviState {
    /** Display name or fallback to email prefix or "User". */
    val userName: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: userEmail?.substringBefore('@')
            ?: "User"

    /** Shortened wallet address for display (0x1234...5678). */
    val shortWalletAddress: String?
        get() = walletAddress?.let {
            if (it.length > 12) "${it.take(6)}...${it.takeLast(4)}" else it
        }
}

package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.domain.model.RouteSummaryResponse
import io.privy.wallet.ethereum.EmbeddedEthereumWallet

/**
 * Parameters for executing a swap transaction.
 * Used by both SwapTokenUseCase (UI) and AutoSellWorker (background).
 */
data class ExecuteSwapParams(
    val tokenIn: TokenInfo,
    val tokenOut: TokenInfo,
    val routeSummary: RouteSummaryResponse,
    val amountIn: java.math.BigInteger,
    val userAddress: String,
    val wallet: EmbeddedEthereumWallet,
    val chainId: Long,
    val chainName: String,
    val slippageBps: Int = 50 // Default 0.5% for backward compatibility
)

/**
 * Token information for swap execution.
 */
data class TokenInfo(
    val address: String,
    val symbol: String,
    val decimals: Int = 18
)

/**
 * Result of swap execution.
 */
sealed class ExecuteSwapResult {
    /** Transaction confirmed successfully */
    data class Success(val txHash: String) : ExecuteSwapResult()
    
    /** Transaction reverted on-chain */
    data class Reverted(val txHash: String) : ExecuteSwapResult()
    
    /** Transaction submitted but receipt timeout (still pending) */
    data class Pending(val txHash: String) : ExecuteSwapResult()
    
    /** Error before transaction submission */
    data class Error(val message: String) : ExecuteSwapResult()
}

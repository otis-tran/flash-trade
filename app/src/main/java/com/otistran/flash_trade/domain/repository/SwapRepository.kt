package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.EncodedSwap
import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.model.SwapResult
import com.otistran.flash_trade.util.Result
import java.math.BigInteger

/**
 * Repository for swap operations.
 */
interface SwapRepository {

    /**
     * Get swap quote from KyberSwap Aggregator.
     * @param chain Chain identifier (e.g., "base")
     * @param tokenIn Input token address
     * @param tokenOut Output token address
     * @param amountIn Amount in wei
     * @param slippageTolerance Slippage in bps (default 10 = 0.1%)
     * @param userAddress User wallet address for exclusive pools
     * @return Quote or error
     */
    suspend fun getQuote(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger,
        slippageTolerance: Int = 10,
        userAddress: String? = null
    ): Result<Quote>

    /**
     * Build encoded swap transaction.
     * @param chain Chain identifier
     * @param quote Quote to execute
     * @param senderAddress User wallet address
     * @param recipientAddress Recipient address (default = sender)
     * @return Encoded swap data or error
     */
    suspend fun buildSwap(
        chain: String,
        quote: Quote,
        senderAddress: String,
        recipientAddress: String? = null
    ): Result<EncodedSwap>

    /**
     * Execute swap transaction via Privy wallet.
     * @param encodedSwap Encoded transaction data
     * @return Transaction hash or error
     */
    suspend fun executeSwap(
        encodedSwap: EncodedSwap
    ): Result<SwapResult>
}

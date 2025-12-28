package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.model.SwapResult
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Execute swap with quote expiration check.
 */
class ExecuteSwapUseCase @Inject constructor(
    private val swapRepository: SwapRepository
) {
    suspend operator fun invoke(
        chain: String,
        quote: Quote,
        senderAddress: String
    ): Result<SwapResult> {
        // Check quote expiration
        if (quote.isExpired()) {
            return Result.Error("Quote expired, please refresh")
        }

        // Build swap transaction
        val buildResult = swapRepository.buildSwap(
            chain = chain,
            quote = quote,
            senderAddress = senderAddress
        )

        if (buildResult is Result.Error) {
            return Result.Error(buildResult.message)
        }

        val encodedSwap = (buildResult as Result.Success).data

        // Execute via Privy wallet
        return swapRepository.executeSwap(encodedSwap)
    }
}

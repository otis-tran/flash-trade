package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.domain.model.Quote
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.util.Result
import java.math.BigInteger
import javax.inject.Inject

/**
 * Get swap quote with validation.
 */
class GetSwapQuoteUseCase @Inject constructor(
    private val swapRepository: SwapRepository
) {
    suspend operator fun invoke(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger,
        slippageTolerance: Int = 10,
        userAddress: String? = null
    ): Result<Quote> {
        // Validate inputs
        if (amountIn <= BigInteger.ZERO) {
            return Result.Error("Amount must be greater than zero")
        }
        if (tokenIn.equals(tokenOut, ignoreCase = true)) {
            return Result.Error("Cannot swap same token")
        }

        return swapRepository.getQuote(
            chain = chain,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            slippageTolerance = slippageTolerance,
            userAddress = userAddress
        )
    }
}

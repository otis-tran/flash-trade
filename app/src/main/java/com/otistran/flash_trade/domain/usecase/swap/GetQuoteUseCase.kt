package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.SwapQuote
import com.otistran.flash_trade.domain.repository.SwapRepository
import com.otistran.flash_trade.presentation.feature.swap.SwapToken
import com.otistran.flash_trade.util.Result
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

/**
 * Use case for fetching swap quotes from Kyber API.
 */
class GetQuoteUseCase @Inject constructor(
    private val swapRepository: SwapRepository
) {
    suspend operator fun invoke(
        sellToken: SwapToken,
        buyToken: SwapToken,
        sellAmount: BigDecimal,
        network: NetworkMode
    ): Result<SwapQuote> {
        // Convert to wei
        val amountInWei = sellAmount
            .multiply(BigDecimal.TEN.pow(sellToken.decimals))
            .toBigInteger()

        if (amountInWei <= BigInteger.ZERO) {
            return Result.Error("Invalid amount")
        }

        return swapRepository.getQuote(
            chain = network.chainName,
            tokenIn = sellToken.address,
            tokenOut = buyToken.address,
            amountIn = amountInWei,
            tokenInDecimals = sellToken.decimals,
            tokenOutDecimals = buyToken.decimals
        )
    }
}

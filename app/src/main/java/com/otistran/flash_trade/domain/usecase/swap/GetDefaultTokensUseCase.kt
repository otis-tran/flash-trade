package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.core.util.StablecoinConstants
import com.otistran.flash_trade.core.util.StablecoinType
import com.otistran.flash_trade.core.util.TokenConstants
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.TokenRepository
import com.otistran.flash_trade.presentation.feature.swap.SwapToken
import com.otistran.flash_trade.presentation.feature.swap.toSwapToken
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Fetches default tokens (WETH sell, USDT buy) for swap screen.
 */
class GetDefaultTokensUseCase @Inject constructor(
    private val tokenRepository: TokenRepository
) {
    data class DefaultTokens(
        val sellToken: SwapToken,
        val buyToken: SwapToken
    )

    suspend operator fun invoke(network: NetworkMode): DefaultTokens {
        val wethAddress = TokenConstants.getWethAddress(network)
        val usdtAddress = StablecoinConstants.getAddress(StablecoinType.USDT, network)

        val (wethResult, usdtResult) = coroutineScope {
            val wethDeferred = async { tokenRepository.getTokenByAddress(wethAddress, network) }
            val usdtDeferred = async { tokenRepository.getTokenByAddress(usdtAddress, network) }
            Pair(wethDeferred.await(), usdtDeferred.await())
        }

        val wethToken = (wethResult as? Result.Success)?.data?.toSwapToken()
            ?: createFallbackWeth(wethAddress)

        val usdtToken = (usdtResult as? Result.Success)?.data?.toSwapToken()
            ?: createFallbackUsdt(usdtAddress)

        return DefaultTokens(sellToken = wethToken, buyToken = usdtToken)
    }

    private fun createFallbackWeth(address: String) = SwapToken(
        address = address,
        symbol = "WETH",
        name = "Wrapped Ether",
        decimals = 18,
        logoUrl = null,
        balance = BigDecimal.ZERO
    )

    private fun createFallbackUsdt(address: String) = SwapToken(
        address = address,
        symbol = "USDT",
        name = "Tether USD",
        decimals = 6,
        logoUrl = null,
        balance = BigDecimal.ZERO
    )
}

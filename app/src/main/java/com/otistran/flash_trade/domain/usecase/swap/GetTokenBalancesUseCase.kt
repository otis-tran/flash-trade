package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.BalanceRepository
import com.otistran.flash_trade.presentation.feature.swap.SwapToken
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Fetches balances for sell and buy tokens.
 */
class GetTokenBalancesUseCase @Inject constructor(
    private val balanceRepository: BalanceRepository,
    private val privyAuthService: PrivyAuthService
) {
    data class TokenBalances(
        val sellBalance: BigDecimal,
        val buyBalance: BigDecimal
    )

    suspend operator fun invoke(
        sellToken: SwapToken?,
        buyToken: SwapToken?,
        network: NetworkMode
    ): TokenBalances? {
        val wallet = privyAuthService.getUser()
            ?.embeddedEthereumWallets?.firstOrNull()?.address
            ?: return null

        if (sellToken == null && buyToken == null) return null

        return coroutineScope {
            val sellBalanceDeferred = sellToken?.let {
                async { fetchBalance(wallet, it.address, it.decimals, network) }
            }
            val buyBalanceDeferred = buyToken?.let {
                async { fetchBalance(wallet, it.address, it.decimals, network) }
            }

            TokenBalances(
                sellBalance = sellBalanceDeferred?.await() ?: BigDecimal.ZERO,
                buyBalance = buyBalanceDeferred?.await() ?: BigDecimal.ZERO
            )
        }
    }

    private suspend fun fetchBalance(
        wallet: String,
        tokenAddress: String,
        decimals: Int,
        network: NetworkMode
    ): BigDecimal {
        return balanceRepository.getTokenBalance(
            walletAddress = wallet,
            tokenAddress = tokenAddress,
            tokenDecimals = decimals,
            network = network
        ).getOrDefault(BigDecimal.ZERO)
    }
}

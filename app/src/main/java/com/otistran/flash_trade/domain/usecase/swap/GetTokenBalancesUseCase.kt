package com.otistran.flash_trade.domain.usecase.swap

import com.otistran.flash_trade.core.util.TokenConstants
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.AlchemyPortfolioRepository
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.presentation.feature.swap.SwapToken
import com.otistran.flash_trade.util.Result
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Fetches balances for sell and buy tokens using Alchemy Portfolio API.
 * This ensures consistency with Portfolio screen which uses the same API.
 */
class GetTokenBalancesUseCase @Inject constructor(
    private val alchemyPortfolioRepository: AlchemyPortfolioRepository,
    private val authRepository: AuthRepository
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
        // Get wallet address from AuthRepository (same as Portfolio)
        val wallet = authRepository.getUserAuthState().walletAddress
            ?: run {
                Timber.w("[GetTokenBalances] No wallet found in auth state")
                return null
            }

        if (sellToken == null && buyToken == null) {
            Timber.w("[GetTokenBalances] Both tokens null")
            return null
        }

        Timber.d("[GetTokenBalances] Fetching balances for wallet=$wallet, network=$network")
        Timber.d("[GetTokenBalances] sellToken=${sellToken?.symbol} (${sellToken?.address})")
        Timber.d("[GetTokenBalances] buyToken=${buyToken?.symbol} (${buyToken?.address})")

        // Fetch all token holdings from Alchemy (same API as Portfolio)
        val holdingsResult = alchemyPortfolioRepository.getTokenHoldings(wallet, network)
        
        return when (holdingsResult) {
            is Result.Success -> {
                val holdings = holdingsResult.data
                Timber.d("[GetTokenBalances] Received ${holdings.size} holdings")

                val sellBalance = sellToken?.let { token ->
                    findTokenBalance(holdings, token.address)
                } ?: BigDecimal.ZERO

                val buyBalance = buyToken?.let { token ->
                    findTokenBalance(holdings, token.address)
                } ?: BigDecimal.ZERO

                Timber.i("[GetTokenBalances] Results: sell=${sellToken?.symbol}=$sellBalance, buy=${buyToken?.symbol}=$buyBalance")

                TokenBalances(
                    sellBalance = sellBalance,
                    buyBalance = buyBalance
                )
            }
            is Result.Error -> {
                Timber.e("[GetTokenBalances] Failed to fetch holdings: ${holdingsResult.message}")
                null
            }
            Result.Loading -> null
        }
    }

    /**
     * Find balance for a specific token address from holdings list.
     * Native token (ETH) has null address in TokenHolding.
     */
    private fun findTokenBalance(
        holdings: List<com.otistran.flash_trade.presentation.feature.portfolio.TokenHolding>,
        tokenAddress: String
    ): BigDecimal {
        val isNativeToken = TokenConstants.isNativeToken(tokenAddress)
        
        Timber.d("[GetTokenBalances] Looking for token: $tokenAddress, isNative: $isNativeToken")
        Timber.d("[GetTokenBalances] Available holdings: ${holdings.map { "${it.symbol}(${it.address}):${it.balance}" }}")
        
        val holding = holdings.find { h ->
            if (isNativeToken) {
                // Native token has null address in TokenHolding
                h.address == null
            } else {
                h.address?.equals(tokenAddress, ignoreCase = true) == true
            }
        }

        val balance = holding?.let { BigDecimal.valueOf(it.balance) } ?: BigDecimal.ZERO
        Timber.d("[GetTokenBalances] Found holding: ${holding?.symbol}, balance: $balance")
        return balance
    }
}

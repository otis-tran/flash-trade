package com.otistran.flash_trade.presentation.feature.swap.manager

import android.util.Log
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.usecase.swap.GetTokenBalancesUseCase
import com.otistran.flash_trade.domain.usecase.swap.GetTokenPricesUseCase
import com.otistran.flash_trade.presentation.feature.swap.SwapToken
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal

/**
 * Result of token data fetch operation.
 */
data class TokenDataResult(
    val sellBalance: BigDecimal? = null,
    val buyBalance: BigDecimal? = null,
    val sellPrice: Double? = null,
    val buyPrice: Double? = null
)

/**
 * Manages token data fetching (balances and prices) for swap screen.
 * Consolidates multiple async operations into single callback.
 */
class TokenDataManager(
    private val getTokenBalancesUseCase: GetTokenBalancesUseCase,
    private val getTokenPricesUseCase: GetTokenPricesUseCase,
    private val coroutineScope: CoroutineScope
) {
    /**
     * Fetch balances and prices for both tokens in parallel.
     *
     * @param sellToken Sell token (nullable)
     * @param buyToken Buy token (nullable)
     * @param network Current network
     * @param onResult Callback with fetched data
     */
    fun fetchTokenData(
        sellToken: SwapToken?,
        buyToken: SwapToken?,
        network: NetworkMode,
        onResult: (TokenDataResult) -> Unit
    ) {
        coroutineScope.launch {
            val balancesDeferred = async {
                getTokenBalancesUseCase(sellToken, buyToken, network)
            }

            val pricesDeferred = if (sellToken != null && buyToken != null) {
                async {
                    getTokenPricesUseCase(sellToken.address, buyToken.address, network)
                }
            } else null

            val balances = balancesDeferred.await()
            val prices = pricesDeferred?.await()

            val result = TokenDataResult(
                sellBalance = balances?.sellBalance,
                buyBalance = balances?.buyBalance,
                sellPrice = (prices as? Result.Success)?.data?.sellPrice,
                buyPrice = (prices as? Result.Success)?.data?.buyPrice
            )

            onResult(result)
        }
    }

    /**
     * Fetch balances only (no prices).
     */
    fun fetchBalancesOnly(
        sellToken: SwapToken?,
        buyToken: SwapToken?,
        network: NetworkMode,
        onResult: (sellBalance: BigDecimal?, buyBalance: BigDecimal?) -> Unit
    ) {
        coroutineScope.launch {
            val balances = getTokenBalancesUseCase(sellToken, buyToken, network)
            onResult(balances?.sellBalance, balances?.buyBalance)
        }
    }
}

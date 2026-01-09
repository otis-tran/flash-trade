package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.data.remote.dto.kyber.RouteSummary
import com.otistran.flash_trade.domain.model.EncodedRouteResponse
import com.otistran.flash_trade.domain.model.RouteSummaryResponse
import com.otistran.flash_trade.domain.model.SwapQuote
import com.otistran.flash_trade.util.Result
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import java.math.BigInteger

/**
 * Repository for swap operations.
 */
interface SwapRepository {
    suspend fun getRoutes(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger
    ): Result<RouteSummaryResponse>

    suspend fun getQuote(
        chain: String,
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger,
        tokenInDecimals: Int,
        tokenOutDecimals: Int
    ): Result<SwapQuote>

    suspend fun buildEncodedRoute(
        chain: String,
        routeSummary: RouteSummaryResponse,
        senderAddress: String,
        slippageTolerance: Int,
        recipientAddress: String? = null,
        permit: String? = null,
        deadline: Long? = null
    ): Result<EncodedRouteResponse>

    suspend fun signTransaction(
        wallet: EmbeddedEthereumWallet,
        encodedRoute: EncodedRouteResponse,
        chainId: String,
        senderAddress: String
    ): Result<String>
}


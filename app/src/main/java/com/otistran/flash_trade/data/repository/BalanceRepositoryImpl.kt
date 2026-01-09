package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.remote.api.AlchemyDataApiService
import com.otistran.flash_trade.data.remote.dto.alchemy.AlchemyTokenBalancesRequestDto
import com.otistran.flash_trade.data.remote.dto.alchemy.BalanceAddressDto
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.model.toAlchemyNetwork
import com.otistran.flash_trade.domain.repository.BalanceRepository
import com.otistran.flash_trade.util.Result
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BalanceRepository using Alchemy Data API.
 * Uses the same API as Portfolio for consistency.
 */
@Singleton
class BalanceRepositoryImpl @Inject constructor(
    private val alchemyApi: AlchemyDataApiService
) : BalanceRepository {

    companion object {
        private const val NATIVE_TOKEN_ADDRESS = "0x0000000000000000000000000000000000000000"
        private const val ETH_DECIMALS = 18
    }

    /**
     * Parse hex balance string (0x...) to BigDecimal.
     * Alchemy returns balances as hex strings like "0x3f4a0066418896d1ecbe".
     */
    private fun parseHexBalance(hexBalance: String, decimals: Int): BigDecimal {
        return try {
            val cleanHex = hexBalance.removePrefix("0x").removePrefix("0X")
            if (cleanHex.isEmpty() || cleanHex.all { it == '0' }) {
                return BigDecimal.ZERO
            }
            val rawBalance = BigInteger(cleanHex, 16).toBigDecimal()
            rawBalance.divide(BigDecimal.TEN.pow(decimals), 6, RoundingMode.DOWN)
        } catch (e: Exception) {
            Timber.w(e, "[BalanceRepo] Failed to parse hex balance: $hexBalance")
            BigDecimal.ZERO
        }
    }

    override suspend fun getNativeBalance(
        walletAddress: String,
        network: NetworkMode
    ): Result<BigDecimal> {
        return try {
            val request = AlchemyTokenBalancesRequestDto(
                addresses = listOf(
                    BalanceAddressDto(
                        address = walletAddress,
                        networks = listOf(network.toAlchemyNetwork())
                    )
                ),
                withMetadata = false,
                withPrices = false,
                includeNativeTokens = true
            )

            val response = alchemyApi.getTokensByWallet(request)
            
            // Find native token (tokenAddress is null for native tokens)
            val nativeToken = response.data.tokens.find { 
                it.tokenAddress == null && it.error == null 
            }

            val balance = nativeToken?.let {
                parseHexBalance(it.tokenBalance, ETH_DECIMALS)
            } ?: BigDecimal.ZERO

            Timber.d("[BalanceRepo] Native balance for $walletAddress: $balance")
            Result.success(balance)
        } catch (e: Exception) {
            Timber.e(e, "[BalanceRepo] Failed to fetch native balance")
            Result.error("Failed to fetch native balance: ${e.message}", e)
        }
    }

    override suspend fun getTokenBalance(
        walletAddress: String,
        tokenAddress: String,
        tokenDecimals: Int,
        network: NetworkMode
    ): Result<BigDecimal> {
        // Native token check
        if (tokenAddress == NATIVE_TOKEN_ADDRESS) {
            return getNativeBalance(walletAddress, network)
        }

        return try {
            val request = AlchemyTokenBalancesRequestDto(
                addresses = listOf(
                    BalanceAddressDto(
                        address = walletAddress,
                        networks = listOf(network.toAlchemyNetwork())
                    )
                ),
                withMetadata = false,
                withPrices = false,
                includeNativeTokens = false
            )

            val response = alchemyApi.getTokensByWallet(request)
            
            // Log all tokens returned
            Timber.d("[BalanceRepo] Searching for token $tokenAddress in ${response.data.tokens.size} tokens")
            response.data.tokens.forEach { token ->
                Timber.d("[BalanceRepo] Found token: ${token.tokenAddress} balance=${token.tokenBalance} error=${token.error}")
            }
            
            // Find the specific token by address
            val token = response.data.tokens.find { 
                it.tokenAddress?.equals(tokenAddress, ignoreCase = true) == true && it.error == null 
            }

            val balance = token?.let {
                parseHexBalance(it.tokenBalance, tokenDecimals)
            } ?: BigDecimal.ZERO

            Timber.d("[BalanceRepo] Token $tokenAddress balance for $walletAddress: $balance (found=${token != null})")
            Result.success(balance)
        } catch (e: Exception) {
            Timber.e(e, "[BalanceRepo] Failed to fetch token balance")
            Result.error("Failed to fetch token balance: ${e.message}", e)
        }
    }
}

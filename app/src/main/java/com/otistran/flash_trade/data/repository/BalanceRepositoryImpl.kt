package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.remote.api.EtherscanApiService
import com.otistran.flash_trade.domain.model.NetworkMode
import com.otistran.flash_trade.domain.repository.BalanceRepository
import com.otistran.flash_trade.util.Result
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Implementation of BalanceRepository using Etherscan API.
 */
@Singleton
class BalanceRepositoryImpl @Inject constructor(
    private val etherscanApi: EtherscanApiService,
    @Named("etherscanApiKey") private val apiKey: String
) : BalanceRepository {

    override suspend fun getNativeBalance(
        walletAddress: String,
        network: NetworkMode
    ): Result<BigDecimal> {
        return try {
            val response = etherscanApi.getBalance(
                chainId = network.chainId,
                address = walletAddress,
                apiKey = apiKey
            )
            if (response.isSuccess && response.result != null) {
                val resultStr = response.result as? String
                val wei = resultStr?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val balance = wei.divide(BigDecimal.TEN.pow(18), 6, RoundingMode.DOWN)
                Result.success(balance)
            } else {
                Timber.w("getNativeBalance failed: ${response.message}")
                Result.success(BigDecimal.ZERO)
            }
        } catch (e: Exception) {
            Timber.e("getNativeBalance error", e)
            Result.error("Failed to fetch native balance: ${e.message}", e)
        }
    }

    override suspend fun getTokenBalance(
        walletAddress: String,
        tokenAddress: String,
        tokenDecimals: Int,
        network: NetworkMode
    ): Result<BigDecimal> {
        // Native token check - delegate to getNativeBalance
        if (tokenAddress == "0x0000000000000000000000000000000000000000") {
            return getNativeBalance(walletAddress, network)
        }

        return try {
            val response = etherscanApi.getTokenBalance(
                chainId = network.chainId,
                contractAddress = tokenAddress,
                address = walletAddress,
                apiKey = apiKey
            )
            if (response.isSuccess && response.result != null) {
                val resultStr = response.result as? String
                val raw = resultStr?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val balance = raw.divide(
                    BigDecimal.TEN.pow(tokenDecimals),
                    6,
                    RoundingMode.DOWN
                )
                Result.success(balance)
            } else {
                Timber.w("getTokenBalance failed: ${response.message}")
                Result.success(BigDecimal.ZERO)
            }
        } catch (e: Exception) {
            Timber.e("getTokenBalance error", e)
            Result.error("Failed to fetch token balance: ${e.message}", e)
        }
    }
}

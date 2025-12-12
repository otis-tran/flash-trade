package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.Wallet
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for wallet operations.
 */
interface WalletRepository {
    suspend fun createWallet(): Result<Wallet>
    suspend fun getWallet(): Result<Wallet?>
    fun observeWallet(): Flow<Wallet?>
    suspend fun refreshBalance(): Result<Double>
}

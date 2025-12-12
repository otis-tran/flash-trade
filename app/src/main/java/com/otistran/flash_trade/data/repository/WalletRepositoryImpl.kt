package com.otistran.flash_trade.data.repository

import com.otistran.flash_trade.data.local.dao.WalletDao
import com.otistran.flash_trade.data.local.entity.WalletEntity
import com.otistran.flash_trade.di.IoDispatcher
import com.otistran.flash_trade.domain.model.Wallet
import com.otistran.flash_trade.domain.repository.WalletRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WalletRepositoryImpl @Inject constructor(
    private val walletDao: WalletDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WalletRepository {

    override suspend fun createWallet(): Result<Wallet> =
        withContext(ioDispatcher) {
            try {
                // TODO: Implement Privy TEE wallet creation
                val wallet = Wallet(
                    address = "", // Will be populated by Privy
                    chainId = 1,
                    balance = 0.0,
                    createdAt = System.currentTimeMillis()
                )
                walletDao.insert(WalletEntity.fromDomain(wallet))
                Result.Success(wallet)
            } catch (e: Exception) {
                Result.Error("Failed to create wallet: ${e.message}", e)
            }
        }

    override suspend fun getWallet(): Result<Wallet?> =
        withContext(ioDispatcher) {
            try {
                Result.Success(walletDao.getWallet()?.toDomain())
            } catch (e: Exception) {
                Result.Error("Failed to get wallet: ${e.message}", e)
            }
        }

    override fun observeWallet(): Flow<Wallet?> =
        walletDao.observeWallet().map { it?.toDomain() }

    override suspend fun refreshBalance(): Result<Double> =
        withContext(ioDispatcher) {
            try {
                // TODO: Fetch balance from blockchain
                Result.Success(0.0)
            } catch (e: Exception) {
                Result.Error("Failed to refresh balance: ${e.message}", e)
            }
        }
}

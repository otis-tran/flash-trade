package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.BalanceRepositoryImpl
import com.otistran.flash_trade.domain.repository.BalanceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for balance repository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BalanceModule {

    @Binds
    @Singleton
    abstract fun bindBalanceRepository(
        impl: BalanceRepositoryImpl
    ): BalanceRepository
}

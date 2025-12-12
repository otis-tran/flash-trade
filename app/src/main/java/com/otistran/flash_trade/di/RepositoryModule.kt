package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.TradeRepositoryImpl
import com.otistran.flash_trade.data.repository.WalletRepositoryImpl
import com.otistran.flash_trade.domain.repository.TradeRepository
import com.otistran.flash_trade.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTradeRepository(
        impl: TradeRepositoryImpl
    ): TradeRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        impl: WalletRepositoryImpl
    ): WalletRepository
}

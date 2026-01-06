package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.PurchaseRepositoryImpl
import com.otistran.flash_trade.domain.repository.PurchaseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PurchaseModule {

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(
        impl: PurchaseRepositoryImpl
    ): PurchaseRepository
}

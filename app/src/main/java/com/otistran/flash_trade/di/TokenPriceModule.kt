package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.TokenPriceRepositoryImpl
import com.otistran.flash_trade.domain.repository.TokenPriceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for token price dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TokenPriceModule {

    @Binds
    abstract fun bindTokenPriceRepository(
        impl: TokenPriceRepositoryImpl
    ): TokenPriceRepository
}

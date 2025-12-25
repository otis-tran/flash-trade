package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.PortfolioRepositoryImpl
import com.otistran.flash_trade.domain.repository.PortfolioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for portfolio-related components.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PortfolioModule {

    @Binds
    @Singleton
    abstract fun bindPortfolioRepository(
        impl: PortfolioRepositoryImpl
    ): PortfolioRepository
}

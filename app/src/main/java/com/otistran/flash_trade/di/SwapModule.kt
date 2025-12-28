package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.SwapRepositoryImpl
import com.otistran.flash_trade.domain.repository.SwapRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SwapModule {

    @Binds
    @Singleton
    abstract fun bindSwapRepository(
        impl: SwapRepositoryImpl
    ): SwapRepository
}

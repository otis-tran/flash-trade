package com.otistran.flash_trade.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.privy.sdk.Privy
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrivyModule {

    @Provides
    @Singleton
    fun providePrivy(): Privy {
        return PrivyProvider.getInstance()
    }
}
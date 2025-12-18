package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.remote.api.KyberApiService
import com.otistran.flash_trade.data.repository.TokenRepositoryImpl
import com.otistran.flash_trade.domain.repository.TokenRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TokenModule {

    @Binds
    @Singleton
    abstract fun bindTokenRepository(
        impl: TokenRepositoryImpl
    ): TokenRepository
}

@Module
@InstallIn(SingletonComponent::class)
object KyberApiModule {

    @Provides
    @Singleton
    fun provideKyberApiService(
        @Named("kyber") retrofit: Retrofit
    ): KyberApiService {
        return retrofit.create(KyberApiService::class.java)
    }
}
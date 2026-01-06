package com.otistran.flash_trade.di.network

import com.otistran.flash_trade.core.network.interceptor.AlchemyAuthInterceptor
import com.otistran.flash_trade.core.network.interceptor.AuthInterceptor
import com.otistran.flash_trade.core.network.interceptor.ClientIdInterceptor
import com.otistran.flash_trade.core.network.interceptor.NetworkInterceptor
import com.otistran.flash_trade.core.network.interceptor.RateLimitInterceptor
import com.otistran.flash_trade.core.network.interceptor.ResponseInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HttpClientModule {

    @Provides
    @Singleton
    fun provideHttpClientFactory(): HttpClientFactory = HttpClientFactory()

    @Provides
    @Singleton
    fun provideDefaultOkHttpClient(
        factory: HttpClientFactory,
        networkInterceptor: NetworkInterceptor,
        authInterceptor: AuthInterceptor,
        responseInterceptor: ResponseInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return factory.create(HttpClientConfig.Default(
            interceptors = listOf(
                networkInterceptor,
                authInterceptor,
                responseInterceptor,
                loggingInterceptor
            )
        ))
    }

    @Provides
    @Singleton
    @Named("kyberClient")
    fun provideKyberOkHttpClient(
        factory: HttpClientFactory,
        networkInterceptor: NetworkInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return factory.create(HttpClientConfig.Kyber(
            interceptors = listOf(networkInterceptor, loggingInterceptor)
        ))
    }

    @Provides
    @Singleton
    @Named("kyberSwapClient")
    fun provideKyberSwapOkHttpClient(
        factory: HttpClientFactory,
        networkInterceptor: NetworkInterceptor,
        clientIdInterceptor: ClientIdInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return factory.create(HttpClientConfig.KyberSwap(
            interceptors = listOf(networkInterceptor, clientIdInterceptor, loggingInterceptor)
        ))
    }

    @Provides
    @Singleton
    @Named("etherscanClient")
    fun provideEtherscanOkHttpClient(
        factory: HttpClientFactory,
        networkInterceptor: NetworkInterceptor,
        rateLimitInterceptor: RateLimitInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return factory.create(HttpClientConfig.Etherscan(
            interceptors = listOf(networkInterceptor, rateLimitInterceptor, loggingInterceptor)
        ))
    }

    @Provides
    @Singleton
    @Named("alchemyClient")
    fun provideAlchemyOkHttpClient(
        factory: HttpClientFactory,
        networkInterceptor: NetworkInterceptor,
        alchemyAuthInterceptor: AlchemyAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return factory.create(HttpClientConfig.Alchemy(
            interceptors = listOf(networkInterceptor, alchemyAuthInterceptor, loggingInterceptor)
        ))
    }
}

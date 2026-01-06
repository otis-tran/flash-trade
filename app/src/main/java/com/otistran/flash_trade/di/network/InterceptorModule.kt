package com.otistran.flash_trade.di.network

import android.content.Context
import com.otistran.flash_trade.BuildConfig
import com.otistran.flash_trade.core.datastore.UserPreferences
import com.otistran.flash_trade.core.network.interceptor.AlchemyAuthInterceptor
import com.otistran.flash_trade.core.network.interceptor.AuthInterceptor
import com.otistran.flash_trade.core.network.interceptor.ClientIdInterceptor
import com.otistran.flash_trade.core.network.interceptor.NetworkInterceptor
import com.otistran.flash_trade.core.network.interceptor.RateLimitInterceptor
import com.otistran.flash_trade.core.network.interceptor.ResponseInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InterceptorModule {

    @Provides
    @Singleton
    fun provideNetworkInterceptor(
        @ApplicationContext context: Context
    ): NetworkInterceptor = NetworkInterceptor(context)

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        userPreferences: UserPreferences
    ): AuthInterceptor = AuthInterceptor(userPreferences)

    @Provides
    @Singleton
    fun provideResponseInterceptor(): ResponseInterceptor = ResponseInterceptor()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideClientIdInterceptor(): ClientIdInterceptor = ClientIdInterceptor(BuildConfig.KYBER_CLIENT_ID)

    @Provides
    @Singleton
    fun provideRateLimitInterceptor(): RateLimitInterceptor = RateLimitInterceptor(callsPerSecond = 3)

    @Provides
    @Singleton
    fun provideAlchemyAuthInterceptor(
        @Named("alchemyApiKey") apiKey: String
    ): AlchemyAuthInterceptor = AlchemyAuthInterceptor(apiKey)

    @Provides
    @Named("alchemyApiKey")
    fun provideAlchemyApiKey(): String = BuildConfig.ALCHEMY_API_KEY

    @Provides
    @Named("etherscanApiKey")
    fun provideEtherscanApiKey(): String = BuildConfig.ETHERSCAN_API_KEY
}

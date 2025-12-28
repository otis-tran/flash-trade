package com.otistran.flash_trade.di

import android.content.Context
import com.otistran.flash_trade.BuildConfig
import com.otistran.flash_trade.core.datastore.UserPreferences
import com.otistran.flash_trade.core.network.ApiService
import com.otistran.flash_trade.core.network.interceptor.AuthInterceptor
import com.otistran.flash_trade.core.network.interceptor.NetworkInterceptor
import com.otistran.flash_trade.core.network.interceptor.ClientIdInterceptor
import com.otistran.flash_trade.core.network.interceptor.ResponseInterceptor
import com.otistran.flash_trade.data.remote.api.KyberSwapApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ==================== Constants ====================

    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    private const val BASE_URL = "https://api.example.com/"
    private const val KYBER_MAINNET_URL = "https://kd-market-service-api.kyberengineering.io/ethereum/"
    private const val KYBER_TESTNET_URL = "https://kd-market-service-api.kyberengineering.io/sepolia/"
    private const val KYBER_AGGREGATOR_URL = "https://aggregator-api.kyberswap.com/"

    // Client ID for KyberSwap Aggregator (elevated rate limits)
    private const val KYBER_CLIENT_ID = "FlashTrade"

    // ==================== Interceptors ====================

    @Provides
    @Singleton
    fun provideNetworkInterceptor(
        @ApplicationContext context: Context
    ): NetworkInterceptor {
        return NetworkInterceptor(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        userPreferences: UserPreferences
    ): AuthInterceptor {
        return AuthInterceptor(userPreferences)
    }

    @Provides
    @Singleton
    fun provideResponseInterceptor(): ResponseInterceptor {
        return ResponseInterceptor()
    }

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

    // ==================== OkHttpClient ====================

    @Provides
    @Singleton
    fun provideOkHttpClient(
        networkInterceptor: NetworkInterceptor,
        authInterceptor: AuthInterceptor,
        responseInterceptor: ResponseInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(networkInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(responseInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Lightweight OkHttpClient for Kyber API (no auth needed).
     */
    @Provides
    @Singleton
    @Named("kyberClient")
    fun provideKyberOkHttpClient(
        networkInterceptor: NetworkInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(networkInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // ==================== Moshi ====================

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    // ==================== Retrofit Instances ====================

    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): String = BASE_URL

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        @BaseUrl baseUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * Kyber API Retrofit instance.
     */
    @Provides
    @Singleton
    @Named("kyber")
    fun provideKyberRetrofit(
        @Named("kyberClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(KYBER_MAINNET_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // ==================== API Services ====================

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    // ==================== KyberSwap Aggregator API ====================

    /**
     * ClientIdInterceptor adds x-client-id header for rate limit elevation.
     */
    @Provides
    @Singleton
    fun provideClientIdInterceptor(): ClientIdInterceptor {
        return ClientIdInterceptor(KYBER_CLIENT_ID)
    }

    /**
     * OkHttpClient for KyberSwap Aggregator API with client-id header.
     */
    @Provides
    @Singleton
    @Named("kyberSwapClient")
    fun provideKyberSwapOkHttpClient(
        networkInterceptor: NetworkInterceptor,
        clientIdInterceptor: ClientIdInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(networkInterceptor)
            .addInterceptor(clientIdInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Retrofit instance for KyberSwap Aggregator V1 API.
     * Used for swap quotes and execution.
     */
    @Provides
    @Singleton
    @Named("kyberSwap")
    fun provideKyberSwapRetrofit(
        @Named("kyberSwapClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(KYBER_AGGREGATOR_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * KyberSwap Aggregator API service for token swaps.
     */
    @Provides
    @Singleton
    fun provideKyberSwapApiService(
        @Named("kyberSwap") retrofit: Retrofit
    ): KyberSwapApiService {
        return retrofit.create(KyberSwapApiService::class.java)
    }
}
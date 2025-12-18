package com.otistran.flash_trade.di

import android.content.Context
import com.otistran.flash_trade.BuildConfig
import com.otistran.flash_trade.core.datastore.UserPreferences
import com.otistran.flash_trade.core.network.ApiService
import com.otistran.flash_trade.core.network.interceptor.AuthInterceptor
import com.otistran.flash_trade.core.network.interceptor.NetworkInterceptor
import com.otistran.flash_trade.core.network.interceptor.ResponseInterceptor
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

    // TODO: Move to BuildConfig or remote config
    private const val BASE_URL = "https://api.example.com/"

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
            // Interceptors order matters!
            // 1. Check network connectivity first
            .addInterceptor(networkInterceptor)
            // 2. Add authentication headers
            .addInterceptor(authInterceptor)
            // 3. Handle/log responses
            .addInterceptor(responseInterceptor)
            // 4. Log full request/response (debug only)
            .addInterceptor(loggingInterceptor)

            // Timeouts
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)

            // Retry on connection failure
            .retryOnConnectionFailure(true)

            .build()
    }

    // ==================== Moshi ====================

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            // KotlinJsonAdapterFactory for reflection-based parsing
            // Note: For production, prefer @JsonClass(generateAdapter = true) with moshi-codegen
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    // ==================== Retrofit ====================

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

    // ==================== API Services ====================

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
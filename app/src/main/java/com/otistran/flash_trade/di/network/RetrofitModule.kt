package com.otistran.flash_trade.di.network

import com.otistran.flash_trade.data.remote.api.AlchemyDataApiService
import com.otistran.flash_trade.data.remote.api.AlchemyPriceApiService
import com.otistran.flash_trade.data.remote.api.EtherscanApiService
import com.otistran.flash_trade.data.remote.api.KyberSwapApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    private const val KYBER_MAINNET_URL = "https://kd-market-service-api.kyberengineering.io/ethereum/"
    private const val KYBER_AGGREGATOR_URL = "https://aggregator-api.kyberswap.com/"
    private const val ETHERSCAN_API_URL = "https://api.etherscan.io/"
    private const val ALCHEMY_PRICE_API_URL = "https://api.g.alchemy.com/prices/v1/"
    private const val ALCHEMY_DATA_API_URL = "https://api.g.alchemy.com/data/v1/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    @Named("kyber")
    fun provideKyberRetrofit(
        @Named("kyberClient") client: OkHttpClient,
        moshi: Moshi
    ): Retrofit = createRetrofit(KYBER_MAINNET_URL, client, moshi)

    @Provides
    @Singleton
    @Named("kyberSwap")
    fun provideKyberSwapRetrofit(
        @Named("kyberSwapClient") client: OkHttpClient,
        moshi: Moshi
    ): Retrofit = createRetrofit(KYBER_AGGREGATOR_URL, client, moshi)

    @Provides
    @Singleton
    fun provideKyberSwapApiService(@Named("kyberSwap") retrofit: Retrofit): KyberSwapApiService =
        retrofit.create(KyberSwapApiService::class.java)

    @Provides
    @Singleton
    @Named("etherscan")
    fun provideEtherscanRetrofit(
        @Named("etherscanClient") client: OkHttpClient,
        moshi: Moshi
    ): Retrofit = createRetrofit(ETHERSCAN_API_URL, client, moshi)

    @Provides
    @Singleton
    fun provideEtherscanApiService(@Named("etherscan") retrofit: Retrofit): EtherscanApiService =
        retrofit.create(EtherscanApiService::class.java)

    @Provides
    @Singleton
    @Named("alchemy")
    fun provideAlchemyRetrofit(
        @Named("alchemyClient") client: OkHttpClient,
        moshi: Moshi
    ): Retrofit = createRetrofit(ALCHEMY_PRICE_API_URL, client, moshi)

    @Provides
    @Singleton
    fun provideAlchemyPriceApiService(@Named("alchemy") retrofit: Retrofit): AlchemyPriceApiService =
        retrofit.create(AlchemyPriceApiService::class.java)

    @Provides
    @Singleton
    @Named("alchemyData")
    fun provideAlchemyDataRetrofit(
        @Named("alchemyClient") client: OkHttpClient,
        moshi: Moshi
    ): Retrofit = createRetrofit(ALCHEMY_DATA_API_URL, client, moshi)

    @Provides
    @Singleton
    fun provideAlchemyDataApiService(@Named("alchemyData") retrofit: Retrofit): AlchemyDataApiService =
        retrofit.create(AlchemyDataApiService::class.java)

    private fun createRetrofit(baseUrl: String, client: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}

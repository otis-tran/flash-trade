package com.otistran.flash_trade.di.network

import okhttp3.Interceptor
import java.util.concurrent.TimeUnit

/**
 * Configuration for OkHttpClient creation.
 * Each API has specific interceptor requirements.
 */
sealed class HttpClientConfig(
    val connectTimeout: Long = 60L,
    val readTimeout: Long = 60L,
    val writeTimeout: Long = 60L,
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
    val retryOnConnectionFailure: Boolean = true
) {
    abstract val interceptors: List<Interceptor>

    /** Default API client with auth */
    data class Default(
        override val interceptors: List<Interceptor>
    ) : HttpClientConfig()

    /** Kyber Token List API (no auth) */
    data class Kyber(
        override val interceptors: List<Interceptor>
    ) : HttpClientConfig()

    /** KyberSwap Aggregator API (client-id header) */
    data class KyberSwap(
        override val interceptors: List<Interceptor>
    ) : HttpClientConfig()

    /** Etherscan API (rate-limited) */
    data class Etherscan(
        override val interceptors: List<Interceptor>
    ) : HttpClientConfig()

    /** Alchemy API (Bearer auth) */
    data class Alchemy(
        override val interceptors: List<Interceptor>
    ) : HttpClientConfig()
}

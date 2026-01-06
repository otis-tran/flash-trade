package com.otistran.flash_trade.di.network

import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating OkHttpClient instances.
 * Eliminates duplicate builder code across providers.
 */
@Singleton
class HttpClientFactory @Inject constructor() {

    fun create(config: HttpClientConfig): OkHttpClient {
        return OkHttpClient.Builder().apply {
            connectTimeout(config.connectTimeout, config.timeUnit)
            readTimeout(config.readTimeout, config.timeUnit)
            writeTimeout(config.writeTimeout, config.timeUnit)
            retryOnConnectionFailure(config.retryOnConnectionFailure)

            config.interceptors.forEach { interceptor ->
                addInterceptor(interceptor)
            }

            // Etherscan needs redirect support
            if (config is HttpClientConfig.Etherscan) {
                followRedirects(true)
                followSslRedirects(true)
            }
        }.build()
    }
}

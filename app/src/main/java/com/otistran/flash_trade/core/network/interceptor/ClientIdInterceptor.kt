package com.otistran.flash_trade.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds x-client-id header to all KyberSwap Aggregator API requests.
 * Required for elevated rate limits (50-100 req/sec vs 10 req/sec without).
 */
class ClientIdInterceptor(private val clientId: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("x-client-id", clientId)
            .addHeader("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}

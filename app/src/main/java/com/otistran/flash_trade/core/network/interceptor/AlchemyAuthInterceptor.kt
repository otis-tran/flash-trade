package com.otistran.flash_trade.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds Bearer token authentication for Alchemy API.
 */
class AlchemyAuthInterceptor(
    private val apiKey: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val authenticatedRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()
        return chain.proceed(authenticatedRequest)
    }
}

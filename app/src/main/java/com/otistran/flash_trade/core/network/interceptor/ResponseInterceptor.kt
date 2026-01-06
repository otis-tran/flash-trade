package com.otistran.flash_trade.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor for handling and logging API responses.
 * 
 * Features:
 * - Logs response time
 * - Can be extended to handle specific response codes globally
 */
@Singleton
class ResponseInterceptor @Inject constructor() : Interceptor {

    companion object {
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val response = chain.proceed(request)

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Log response info
        Timber.d(buildString {
            append("[${response.code}] ")
            append("${request.method} ${request.url.encodedPath} ")
            append("(${duration}ms)")
        })

        // Handle specific response codes globally if needed
        when (response.code) {
            401 -> {
                // Token expired - could trigger token refresh or logout
                Timber.w("Unauthorized response - token may be expired")
            }
            403 -> {
                Timber.w("Forbidden response - access denied")
            }
            in 500..599 -> {
                Timber.e("Server error: ${response.code}")
            }
        }

        return response
    }
}
package com.otistran.flash_trade.core.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
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
        private const val TAG = "API_RESPONSE"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val response = chain.proceed(request)

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Log response info
        Log.d(TAG, buildString {
            append("[${response.code}] ")
            append("${request.method} ${request.url.encodedPath} ")
            append("(${duration}ms)")
        })

        // Handle specific response codes globally if needed
        when (response.code) {
            401 -> {
                // Token expired - could trigger token refresh or logout
                Log.w(TAG, "Unauthorized response - token may be expired")
            }
            403 -> {
                Log.w(TAG, "Forbidden response - access denied")
            }
            in 500..599 -> {
                Log.e(TAG, "Server error: ${response.code}")
            }
        }

        return response
    }
}
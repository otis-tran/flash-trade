package com.otistran.flash_trade.core.network.interceptor

import com.otistran.flash_trade.core.datastore.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that adds authentication token to requests.
 *
 * Features:
 * - Automatically adds Bearer token to all requests
 * - Skips auth for requests with "No-Auth" header
 * - Adds common headers (Content-Type, Accept)
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences
) : Interceptor {

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_NO_AUTH = "No-Auth"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val HEADER_ACCEPT = "Accept"
        const val CONTENT_TYPE_JSON = "application/json"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for requests marked with No-Auth header
        if (originalRequest.header(HEADER_NO_AUTH) != null) {
            val newRequest = originalRequest.newBuilder()
                .removeHeader(HEADER_NO_AUTH)
                .build()
            return chain.proceed(newRequest)
        }

        // Get token from DataStore (blocking call - acceptable in interceptor)
        val token = runBlocking {
            userPreferences.authToken.first()
        }

        // Build new request with auth header
        val newRequest = originalRequest.newBuilder().apply {
            // Add auth token if available
            if (!token.isNullOrBlank()) {
                header(HEADER_AUTHORIZATION, "Bearer $token")
            }

            // Add common headers
            header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
            header(HEADER_ACCEPT, CONTENT_TYPE_JSON)
        }.build()

        return chain.proceed(newRequest)
    }
}

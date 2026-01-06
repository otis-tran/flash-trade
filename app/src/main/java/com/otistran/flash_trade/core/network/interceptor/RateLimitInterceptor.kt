package com.otistran.flash_trade.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicLong

/**
 * Rate limiter interceptor for Etherscan API.
 * Enforces 3 calls/second limit to prevent "Max calls per sec rate limit reached" errors.
 */
class RateLimitInterceptor(
    private val callsPerSecond: Int = 3
) : Interceptor {

    private val lastCallTime = AtomicLong(0)
    private val minInterval = 1000L / callsPerSecond

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        val now = System.currentTimeMillis()
        val elapsed = now - lastCallTime.get()

        if (elapsed < minInterval) {
            val sleepTime = minInterval - elapsed
            Thread.sleep(sleepTime)
        }

        lastCallTime.set(System.currentTimeMillis())
        return chain.proceed(chain.request())
    }
}

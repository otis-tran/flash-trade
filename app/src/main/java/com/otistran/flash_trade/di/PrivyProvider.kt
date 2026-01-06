package com.otistran.flash_trade.di

import android.annotation.SuppressLint
import android.content.Context
import com.otistran.flash_trade.BuildConfig
import io.privy.logging.PrivyLogLevel
import io.privy.sdk.Privy
import io.privy.sdk.PrivyConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton provider for Privy SDK with lazy initialization.
 * Ensures Privy is only created when first needed, not at app startup.
 */
@SuppressLint("StaticFieldLeak")
object PrivyProvider {

    @Volatile
    private var _instance: Privy? = null
    private var _context: Context? = null
    private val initMutex = Mutex()

    /**
     * Set application context for lazy initialization.
     * Call this early in MainActivity.onCreate() - instant, no blocking.
     */
    fun setContext(context: Context) {
        _context = context.applicationContext
    }

    /**
     * Get Privy instance if already initialized.
     * Returns null if not ready - use for non-blocking checks.
     */
    fun getOrNull(): Privy? = _instance

    /**
     * Check if Privy is ready.
     */
    fun isReady(): Boolean = _instance != null

    /**
     * Get or create Privy instance (suspend, thread-safe).
     * This is the primary way to access Privy.
     */
    suspend fun getInstance(): Privy {
        // Fast path - already initialized
        _instance?.let { return it }

        // Slow path - need to initialize (thread-safe)
        initMutex.withLock {
            // Double-check after acquiring lock
            _instance?.let { return it }

            val context = _context
                ?: throw IllegalStateException(
                    "Context not set. Call PrivyProvider.setContext() in MainActivity first."
                )

            val privy = createPrivyInstance(context)
            _instance = privy
            return privy
        }
    }

    /**
     * Blocking get - use only when you're certain Privy is initialized.
     * Prefer getInstance() suspend function instead.
     */
    fun getInstanceBlocking(): Privy {
        return _instance ?: throw IllegalStateException(
            "Privy not initialized. Use getInstance() suspend function for safe access."
        )
    }

    private fun createPrivyInstance(context: Context): Privy {
        return Privy.init(
            context = context,
            config = PrivyConfig(
                appId = BuildConfig.PRIVY_APP_ID,
                appClientId = BuildConfig.PRIVY_APP_CLIENT_ID,
                logLevel = if (BuildConfig.DEBUG) PrivyLogLevel.VERBOSE else PrivyLogLevel.NONE
            )
        )
    }

    /**
     * Reset for testing only.
     */
    internal fun reset() {
        _instance = null
        _context = null
    }
}
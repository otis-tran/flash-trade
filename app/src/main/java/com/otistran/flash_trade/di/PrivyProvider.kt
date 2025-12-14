package com.otistran.flash_trade.di

import io.privy.sdk.Privy

/**
 * Singleton object to hold the Privy instance.
 * This ensures a single instance of Privy is used throughout the app.
 */
object PrivyProvider {
    @Volatile
    private var _instance: Privy? = null

    /**
     * Initialize the Privy instance.
     * Should be called once in the Application class.
     */
    fun initialize(privy: Privy) {
        if (_instance == null) {
            synchronized(this) {
                if (_instance == null) {
                    _instance = privy
                }
            }
        }
    }

    /**
     * Get the Privy instance.
     * Throws IllegalStateException if not initialized.
     */
    fun getInstance(): Privy {
        return _instance ?: throw IllegalStateException(
            "Privy instance not initialized. Make sure to call PrivyProvider.initialize() in your Application class."
        )
    }
}

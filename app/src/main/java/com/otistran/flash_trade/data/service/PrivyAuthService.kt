package com.otistran.flash_trade.data.service

import android.content.Context
import android.util.Log
import com.otistran.flash_trade.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.privy.auth.AuthState as PrivyAuthState
import io.privy.auth.PrivyUser
import io.privy.logging.PrivyLogLevel
import io.privy.sdk.Privy
import io.privy.sdk.PrivyConfig
import io.privy.auth.oAuth.OAuthProvider as PrivyOAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper service for Privy SDK operations.
 * Isolates SDK calls for easier testing and error handling.
 */
@Singleton
class PrivyAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val privy: Privy by lazy {
        Privy.init(
            context = context,
            config = PrivyConfig(
                appId = BuildConfig.PRIVY_APP_ID,
                appClientId = BuildConfig.PRIVY_APP_CLIENT_ID, // Same for basic setup
                logLevel = if (BuildConfig.DEBUG) PrivyLogLevel.VERBOSE else PrivyLogLevel.NONE
            )
        )
    }

    /**
     * Observe auth state changes.
     */
    val authStateFlow: Flow<PrivyAuthState> = privy.authState

    /**
     * Get current auth state (one-time).
     */
    suspend fun getAuthState(): PrivyAuthState {
        privy.awaitReady()
        return privy.authState.value
    }

    /**
     * Login with passkey.
     * @param relyingParty Domain hosting Digital Asset Links
     */
    suspend fun loginWithPasskey(relyingParty: String): Result<PrivyUser> {
        privy.awaitReady()
        Log.d("loginWithPasskey", "loginWithPasskey() called with: relyingParty = $relyingParty")
        return privy.passkey.login(relyingParty = relyingParty)
    }

    /**
     * Signup with passkey (creates new credential).
     * @param relyingParty Domain hosting Digital Asset Links
     */
    suspend fun signupWithPasskey(relyingParty: String): Result<PrivyUser> {
        privy.awaitReady()
        return privy.passkey.signup(relyingParty = relyingParty)
    }

    /**
     * Login with OAuth provider.
     * @param provider OAuth provider (Google, Apple, etc.)
     * @param appUrlScheme Custom URL scheme for callback
     */
    suspend fun loginWithOAuth(
        provider: PrivyOAuthProvider,
        appUrlScheme: String
    ): Result<PrivyUser> {
        privy.awaitReady()
        Log.d(
            "PrivyUser",
            "loginWithOAuth() called with: provider = $provider, appUrlScheme = $appUrlScheme"
        )
        return privy.oAuth.login(
            oAuthProvider = provider,
            appUrlScheme = "$appUrlScheme://oauth/callback"
        )
    }

    /**
     * Logout current user.
     */
    suspend fun logout(): Result<Unit> {
        return try {
            privy.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

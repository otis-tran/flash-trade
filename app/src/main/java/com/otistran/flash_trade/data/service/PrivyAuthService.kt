package com.otistran.flash_trade.data.service

import android.util.Log
import com.otistran.flash_trade.di.PrivyProvider
import io.privy.auth.PrivyUser
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import io.privy.auth.AuthState as PrivyAuthState
import io.privy.auth.oAuth.OAuthProvider as PrivyOAuthProvider

private const val TAG = "PrivyAuthService"

/**
 * Wrapper service for Privy SDK operations.
 * Uses lazy Privy initialization via PrivyProvider.
 */
@Singleton
class PrivyAuthService @Inject constructor() {

    /**
     * Observe auth state changes.
     */
    val authStateFlow: Flow<PrivyAuthState> = flow {
        val privy = PrivyProvider.getInstance()
        privy.authState.collect { emit(it) }
    }

    /**
     * Get current auth state (one-time).
     * Uses new getAuthState() which auto-awaits ready.
     */
    suspend fun getAuthState(): PrivyAuthState {
        val privy = PrivyProvider.getInstance()
        return privy.getAuthState()
    }

    /**
     * Get current authenticated user if exists.
     */
    suspend fun getUser(): PrivyUser? {
        val privy = PrivyProvider.getInstance()
        return privy.getUser()
    }

    /**
     * Check if user has persisted credentials (for offline scenarios).
     */
    suspend fun hasPersistedCredentials(): Boolean {
        val privy = PrivyProvider.getInstance()
        return privy.hasPersistedAuthCredentials()
    }

    /**
     * Login with passkey.
     * @param relyingParty Domain hosting Digital Asset Links
     */
    suspend fun loginWithPasskey(relyingParty: String): Result<PrivyUser> {
        val privy = PrivyProvider.getInstance()
        privy.getAuthState()
        Log.d(TAG, "loginWithPasskey() called with: relyingParty = $relyingParty")
        return privy.passkey.login(relyingParty = relyingParty)
    }

    /**
     * Signup with passkey (creates new credential).
     * @param relyingParty Domain hosting Digital Asset Links
     */
    suspend fun signupWithPasskey(relyingParty: String): Result<PrivyUser> {
        val privy = PrivyProvider.getInstance()
        privy.getAuthState()
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
        val privy = PrivyProvider.getInstance()
        privy.getAuthState()
        Log.d(
            TAG,
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
            val privy = PrivyProvider.getInstance()
            privy.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            Result.failure(e)
        }
    }

    /**
     * Called when network is restored to refresh auth state.
     */
    suspend fun onNetworkRestored() {
        val privy = PrivyProvider.getInstance()
        privy.onNetworkRestored()
    }

    /**
     * Create Ethereum wallet for user if not exists.
     * Returns existing wallet if already created.
     */
    suspend fun ensureEthereumWallet(user: PrivyUser): Result<EmbeddedEthereumWallet> {
        return try {
            // Check if user already has a wallet
            val existingWallet = user.embeddedEthereumWallets.firstOrNull()
            if (existingWallet != null) {
                Log.d(TAG, "User already has Ethereum wallet: ${existingWallet.address}")
                return Result.success(existingWallet)
            }

            // Create new wallet
            Log.d(TAG, "Creating new Ethereum wallet for user: ${user.id}")
            val result = user.createEthereumWallet(allowAdditional = false)
            result.onSuccess { wallet ->
                Log.d(TAG, "Ethereum wallet created: ${wallet.address}")
            }.onFailure { error ->
                Log.e(TAG, "Failed to create Ethereum wallet", error)
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating Ethereum wallet", e)
            Result.failure(e)
        }
    }

    /**
     * Ensure both Ethereum wallet exist (parallel creation).
     * Returns pair of (ethAddress, solAddress) - either can be null if creation failed.
     */
    suspend fun ensureWallets(user: PrivyUser): WalletCreationResult = coroutineScope {
        val ethDeferred = async { ensureEthereumWallet(user) }

        val ethResult = ethDeferred.await()

        WalletCreationResult(
            ethereumAddress = ethResult.getOrNull()?.address,
            ethereumError = ethResult.exceptionOrNull()
        )
    }
}

/**
 * Result of wallet creation attempt.
 */
data class WalletCreationResult(
    val ethereumAddress: String? = null,
    val ethereumError: Throwable? = null
) {
    private val hasEthereumWallet: Boolean get() = ethereumAddress != null
    val hasAnyWallet: Boolean get() = hasEthereumWallet
}
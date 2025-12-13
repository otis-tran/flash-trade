package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.AuthState
import com.otistran.flash_trade.domain.model.OAuthProvider
import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for authentication operations.
 * Abstracts Privy SDK implementation.
 */
interface AuthRepository {
    /**
     * Observe current authentication state.
     * Emits on every state change.
     */
    fun observeAuthState(): Flow<AuthState>

    /**
     * Get current auth state (one-time).
     */
    suspend fun getAuthState(): AuthState

    /**
     * Login with passkey authentication.
     *
     * @param relyingParty Domain hosting Digital Asset Links
     * @return Result containing authenticated user or error
     */
    suspend fun loginWithPasskey(relyingParty: String): Result<User>

    /**
     * Login with OAuth provider.
     *
     * @param provider OAuth provider (Google, Apple, etc.)
     * @param appUrlScheme Custom URL scheme for callback
     * @return Result containing authenticated user or error
     */
    suspend fun loginWithOAuth(
        provider: OAuthProvider,
        appUrlScheme: String
    ): Result<User>

    /**
     * Signup with passkey (creates new account).
     *
     * @param relyingParty Domain hosting Digital Asset Links
     * @return Result containing new user or error
     */
    suspend fun signupWithPasskey(relyingParty: String): Result<User>

    /**
     * Logout current user.
     */
    suspend fun logout(): Result<Unit>
}

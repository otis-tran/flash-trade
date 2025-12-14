package com.otistran.flash_trade.data.repository

import android.util.Log
import com.otistran.flash_trade.data.local.datastore.UserPreferences
import com.otistran.flash_trade.data.mapper.toUser
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.model.AuthState
import com.otistran.flash_trade.domain.model.OAuthProvider
import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.domain.model.UserAuthState
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import io.privy.auth.AuthState as PrivyAuthState
import io.privy.auth.oAuth.OAuthProvider as PrivyOAuthProvider

private const val TAG = "AuthRepositoryImpl"

/**
 * Implementation of AuthRepository using Privy SDK.
 */
class AuthRepositoryImpl @Inject constructor(
    private val privyAuthService: PrivyAuthService,
    private val userPreferences: UserPreferences
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthState> {
        return privyAuthService.authStateFlow.map { it.toDomain() }
    }

    override fun observeUserAuthState(): Flow<UserAuthState> {
        return userPreferences.authStateFlow
    }

    override suspend fun getAuthState(): AuthState {
        return privyAuthService.getAuthState().toDomain()
    }

    override suspend fun getUserAuthState(): UserAuthState {
        return userPreferences.authStateFlow.first()
    }

    override suspend fun hasValidSession(): Boolean {
        val userAuthState = getUserAuthState()
        return userAuthState.isSessionValid
    }

    override suspend fun loginWithPasskey(relyingParty: String): Result<User> {
        return try {
            val result = privyAuthService.loginWithPasskey(relyingParty)
            result.fold(
                onSuccess = { privyUser ->
                    val user = privyUser.toUser()
                    Log.d(TAG, "Passkey login success: ${user.id}")
                    // Save login state to DataStore
                    userPreferences.saveLoginState(user)
                    Result.Success(user)
                },
                onFailure = { error ->
                    Log.e(TAG, "Passkey login failed", error)
                    Result.Error(mapError(error), error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Passkey login exception", e)
            Result.Error(mapError(e), e)
        }
    }

    override suspend fun signupWithPasskey(relyingParty: String): Result<User> {
        return try {
            val result = privyAuthService.signupWithPasskey(relyingParty)
            result.fold(
                onSuccess = { privyUser ->
                    val user = privyUser.toUser()
                    Log.d(TAG, "Passkey signup success: ${user.id}")
                    // Save login state to DataStore
                    userPreferences.saveLoginState(user)
                    Result.Success(user)
                },
                onFailure = { error ->
                    Log.e(TAG, "Passkey signup failed", error)
                    Result.Error(mapError(error), error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Passkey signup exception", e)
            Result.Error(mapError(e), e)
        }
    }

    override suspend fun loginWithOAuth(
        provider: OAuthProvider,
        appUrlScheme: String
    ): Result<User> {
        return try {
            val privyProvider = provider.toPrivyProvider()
            val result = privyAuthService.loginWithOAuth(privyProvider, appUrlScheme)
            result.fold(
                onSuccess = { privyUser ->
                    val user = privyUser.toUser()
                    Log.d(TAG, "OAuth login success: ${user.id}, user: $user")
                    // Save login state to DataStore
                    userPreferences.saveLoginState(user)
                    Result.Success(user)
                },
                onFailure = { error ->
                    Log.e(TAG, "OAuth login failed", error)
                    Result.Error(mapError(error), error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "OAuth login exception", e)
            Result.Error(mapError(e), e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val result = privyAuthService.logout()
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Logout success")
                    // Save login state to DataStore
                    userPreferences.clearLoginState()
                    Result.Success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Logout failed", error)
                    Result.Error("Logout failed", error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Logout exception", e)
            Result.Error("Logout failed", e)
        }
    }

    /**
     * Map exceptions to user-friendly messages.
     */
    private fun mapError(exception: Throwable): String {
        return when {
            exception.message?.contains("cancel", ignoreCase = true) == true ->
                "Login cancelled"

            exception.message?.contains("network", ignoreCase = true) == true ->
                "Network error. Check your connection."

            exception.message?.contains("timeout", ignoreCase = true) == true ->
                "Connection timed out. Please try again."

            exception is java.net.UnknownHostException ->
                "Network error. Check your connection."

            exception is java.net.SocketTimeoutException ->
                "Connection timed out. Please try again."

            else ->
                exception.message ?: "Authentication failed"
        }
    }

    /**
     * Convert Privy AuthState to domain AuthState.
     */
    private fun PrivyAuthState.toDomain(): AuthState = when (this) {
        is PrivyAuthState.Authenticated -> AuthState.AUTHENTICATED
        PrivyAuthState.Unauthenticated -> AuthState.UNAUTHENTICATED
        PrivyAuthState.NotReady -> AuthState.NOT_READY
        is PrivyAuthState.AuthenticatedUnverified -> AuthState.AUTHENTICATED_UNVERIFIED
    }

    /**
     * Convert domain OAuthProvider to Privy OAuthProvider.
     */
    private fun OAuthProvider.toPrivyProvider(): PrivyOAuthProvider = when (this) {
        OAuthProvider.GOOGLE -> PrivyOAuthProvider.Google
        OAuthProvider.TWITTER -> PrivyOAuthProvider.Twitter
        OAuthProvider.DISCORD -> PrivyOAuthProvider.Discord
    }
}

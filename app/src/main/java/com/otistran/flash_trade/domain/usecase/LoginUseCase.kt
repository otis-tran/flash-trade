package com.otistran.flash_trade.domain.usecase

import android.util.Patterns
import com.otistran.flash_trade.domain.model.AuthMethod
import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Use case for login operations.
 * Encapsulates business rules for authentication.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Execute login with specified method.
     *
     * @param method Authentication method (Passkey or OAuth)
     * @param isSignup True if creating new account
     * @return Result containing authenticated user
     */
    suspend operator fun invoke(
        method: AuthMethod,
        isSignup: Boolean = false
    ): Result<User> {
        // Validate input
        if (method is AuthMethod.Passkey) {
            if (method.relyingParty.isBlank()) {
                return Result.Error("Relying party cannot be empty")
            }
            if (!Patterns.WEB_URL.matcher(method.relyingParty).matches()) {
                return Result.Error("Invalid relying party URL")
            }
        }

        if (method is AuthMethod.OAuth && method.scheme.isBlank()) {
            return Result.Error("OAuth scheme cannot be empty")
        }

        // Execute login based on method
        return when (method) {
            is AuthMethod.Passkey -> {
                if (isSignup) {
                    authRepository.signupWithPasskey(method.relyingParty)
                } else {
                    authRepository.loginWithPasskey(method.relyingParty)
                }
            }
            is AuthMethod.OAuth -> {
                // OAuth doesn't distinguish signup/login
                authRepository.loginWithOAuth(method.provider, method.scheme)
            }
        }
    }

    /**
     * Get current authentication state.
     */
    suspend fun getAuthState() = authRepository.getAuthState()
}

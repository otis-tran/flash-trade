package com.otistran.flash_trade.domain.usecase

import com.otistran.flash_trade.domain.model.UserAuthState
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckLoginStatusUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : NoParamsUseCase<UserAuthState> {

    override suspend fun invoke(): Result<UserAuthState> {
        return try {
            val userAuthState = authRepository.getUserAuthState()

            // If user was logged in but session expired, clear it
            if (userAuthState.isLoggedIn && !userAuthState.isSessionValid) {
                authRepository.logout()
                Result.Success(UserAuthState()) // Empty state
            } else {
                Result.Success(userAuthState)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to check login status")
        }
    }

    fun observeLoginStatus(): Flow<UserAuthState> {
        return authRepository.observeUserAuthState()
    }
}
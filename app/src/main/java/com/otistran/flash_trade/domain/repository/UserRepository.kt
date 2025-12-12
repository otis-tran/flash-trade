package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user operations.
 */
interface UserRepository {
    suspend fun getCurrentUser(): Result<User?>
    fun observeUser(): Flow<User?>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun setOnboarded(onboarded: Boolean): Result<Unit>
}

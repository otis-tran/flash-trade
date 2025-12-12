package com.otistran.flash_trade.domain.usecase

import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Base use case interface for single business operations.
 *
 * Usage:
 * class GetUserUseCase @Inject constructor(
 *     private val repo: UserRepository
 * ) : UseCase<Unit, User> {
 *     override suspend fun invoke(params: Unit): Result<User> = repo.getCurrentUser()
 * }
 */
interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Result<R>
}

/**
 * Use case without parameters.
 */
interface NoParamsUseCase<out R> {
    suspend operator fun invoke(): Result<R>
}

/**
 * Use case that returns Flow.
 */
interface FlowUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}

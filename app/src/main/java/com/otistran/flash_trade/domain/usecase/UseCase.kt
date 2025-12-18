package com.otistran.flash_trade.domain.usecase

import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Base use case interface for single business operations with parameters.
 *
 * @param P Parameters type
 * @param R Result data type
 *
 * Usage:
 * ```
 * class GetUserByIdUseCase @Inject constructor(
 *     private val repo: UserRepository
 * ) : UseCase<String, User> {
 *     override suspend fun invoke(params: String): Result<User> =
 *         repo.getUserById(params)
 * }
 * ```
 */
interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Result<R>
}

/**
 * Use case without parameters.
 *
 * Usage:
 * ```
 * class GetCurrentUserUseCase @Inject constructor(
 *     private val repo: UserRepository
 * ) : NoParamsUseCase<User> {
 *     override suspend fun invoke(): Result<User> = repo.getCurrentUser()
 * }
 * ```
 */
interface NoParamsUseCase<out R> {
    suspend operator fun invoke(): Result<R>
}

/**
 * Use case that returns Flow with parameters.
 * Useful for observing data changes.
 *
 * Usage:
 * ```
 * class ObserveUserUseCase @Inject constructor(
 *     private val repo: UserRepository
 * ) : FlowUseCase<String, User> {
 *     override fun invoke(params: String): Flow<User> =
 *         repo.observeUser(params)
 * }
 * ```
 */
interface FlowUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}

/**
 * Use case that returns Flow without parameters.
 *
 * Usage:
 * ```
 * class ObserveSettingsUseCase @Inject constructor(
 *     private val repo: SettingsRepository
 * ) : NoParamsFlowUseCase<Settings> {
 *     override fun invoke(): Flow<Settings> = repo.observeSettings()
 * }
 * ```
 */
interface NoParamsFlowUseCase<out R> {
    operator fun invoke(): Flow<R>
}

/**
 * Use case that returns Flow of Result.
 * Combines streaming with error handling.
 *
 * Usage:
 * ```
 * class ObserveUsersUseCase @Inject constructor(
 *     private val repo: UserRepository
 * ) : FlowResultUseCase<Unit, List<User>> {
 *     override fun invoke(params: Unit): Flow<Result<List<User>>> =
 *         repo.observeUsers()
 * }
 * ```
 */
interface FlowResultUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<Result<R>>
}

/**
 * Use case that returns Flow of Result without parameters.
 */
interface NoParamsFlowResultUseCase<out R> {
    operator fun invoke(): Flow<Result<R>>
}
package com.otistran.flash_trade.util

/**
 * Result wrapper for domain/business operations.
 *
 * Use this in:
 * - UseCase return types
 * - Repository interface return types (domain layer)
 * - ViewModel state handling
 *
 * For network-specific results, see [com.otistran.flash_trade.core.network.NetworkResult]
 */
sealed class Result<out T> {

    /**
     * Successful result with data
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Error result with message and optional cause
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : Result<Nothing>()

    /**
     * Loading state (useful for UI)
     */
    data object Loading : Result<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this is Loading

    /**
     * Get data or null
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Get error message or null
     */
    fun errorOrNull(): String? = (this as? Error)?.message

    /**
     * Get data or default value
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * Get data or compute default
     */
    inline fun getOrElse(default: () -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default()
    }

    /**
     * Fold result into single value
     */
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (String, Throwable?) -> R,
        onLoading: () -> R = { throw IllegalStateException("Unexpected Loading state") }
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(message, cause)
        is Loading -> onLoading()
    }

    companion object {
        /**
         * Create success result
         */
        fun <T> success(data: T): Result<T> = Success(data)

        /**
         * Create error result
         */
        fun <T> error(message: String, cause: Throwable? = null): Result<T> =
            Error(message, cause)

        /**
         * Create loading result
         */
        fun <T> loading(): Result<T> = Loading
    }
}

/**
 * Transform success data
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Transform success data with suspend function
 */
suspend inline fun <T, R> Result<T>.mapSuspend(
    crossinline transform: suspend (T) -> R
): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * FlatMap for chaining operations
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Execute action on success
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/**
 * Execute action on error
 */
inline fun <T> Result<T>.onError(action: (String, Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) action(message, cause)
    return this
}

/**
 * Execute action on loading
 */
inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) action()
    return this
}

/**
 * Convert nullable to Result
 */
fun <T> T?.toResult(errorMessage: String = "Data is null"): Result<T> =
    this?.let { Result.Success(it) } ?: Result.Error(errorMessage)

/**
 * Run catching and convert to Result
 */
inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e.message ?: "Unknown error", e)
}

/**
 * Run catching with suspend function
 */
suspend inline fun <T> resultOfSuspend(
    crossinline block: suspend () -> T
): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e.message ?: "Unknown error", e)
}
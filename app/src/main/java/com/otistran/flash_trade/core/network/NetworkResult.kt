package com.otistran.flash_trade.core.network

/**
 * Sealed class representing the result of a network operation.
 *
 * Use this for API calls in Repository layer.
 * Convert to domain Result in UseCase if needed.
 *
 * @param T The type of data on success
 */
sealed class NetworkResult<out T> {

    /**
     * Successful result with data
     */
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * Error result with typed exception
     */
    data class Error(val exception: AppException) : NetworkResult<Nothing>()

    /**
     * Check if result is success
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Check if result is error
     */
    val isError: Boolean get() = this is Error

    /**
     * Get data or null if error
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Get exception or null if success
     */
    fun exceptionOrNull(): AppException? = (this as? Error)?.exception

    /**
     * Get data or throw exception
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }

    /**
     * Get data or default value
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }

    /**
     * Get data or compute default value
     */
    inline fun getOrElse(default: (AppException) -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default(exception)
    }

    /**
     * Transform success data
     */
    inline fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Transform success data with suspend function
     */
    suspend inline fun <R> mapSuspend(crossinline transform: suspend (T) -> R): NetworkResult<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }

    /**
     * FlatMap for chaining operations
     */
    inline fun <R> flatMap(transform: (T) -> NetworkResult<R>): NetworkResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }

    /**
     * Execute action on success
     */
    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Execute action on error
     */
    inline fun onError(action: (AppException) -> Unit): NetworkResult<T> {
        if (this is Error) action(exception)
        return this
    }

    /**
     * Fold result into single value
     */
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (AppException) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(exception)
    }

    companion object {
        /**
         * Create success result
         */
        fun <T> success(data: T): NetworkResult<T> = Success(data)

        /**
         * Create error result
         */
        fun <T> error(exception: AppException): NetworkResult<T> = Error(exception)

        /**
         * Create error result from message
         */
        fun <T> error(message: String): NetworkResult<T> =
            Error(AppException.Unknown(message))
    }
}

/**
 * Convert NetworkResult to domain Result
 */
fun <T> NetworkResult<T>.toResult(): com.otistran.flash_trade.util.Result<T> = when (this) {
    is NetworkResult.Success -> com.otistran.flash_trade.util.Result.Success(data)
    is NetworkResult.Error -> com.otistran.flash_trade.util.Result.Error(
        message = exception.message,
        cause = exception
    )
}
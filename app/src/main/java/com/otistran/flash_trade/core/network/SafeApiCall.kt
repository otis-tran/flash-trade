package com.otistran.flash_trade.core.network

import android.util.Log
import com.otistran.flash_trade.core.network.interceptor.NoConnectivityException
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

private const val TAG = "SafeApiCall"

/**
 * Safe wrapper for API calls with comprehensive error handling.
 *
 * Usage:
 * ```
 * suspend fun getUsers(): NetworkResult<List<UserDto>> = safeApiCall {
 *     apiService.getUsers()
 * }
 * ```
 *
 * @param apiCall The suspend function that makes the API call
 * @return [NetworkResult] with either Success or Error
 */
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): NetworkResult<T> {
    return withContext(Dispatchers.IO) {
        try {
            NetworkResult.Success(apiCall())
        } catch (e: Exception) {
            Log.e(TAG, "API call failed: ${e.message}", e)
            NetworkResult.Error(e.toAppException())
        }
    }
}

/**
 * Safe wrapper for API calls that return Response<T>.
 * Handles both HTTP errors and empty body responses.
 *
 * Usage:
 * ```
 * suspend fun deleteUser(id: Int): NetworkResult<Unit> = safeApiCallWithResponse {
 *     apiService.deleteUser(id)
 * }
 * ```
 */
suspend fun <T> safeApiCallWithResponse(
    apiCall: suspend () -> Response<T>
): NetworkResult<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall()
            handleResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "API call failed: ${e.message}", e)
            NetworkResult.Error(e.toAppException())
        }
    }
}

/**
 * Handle Retrofit Response and convert to NetworkResult
 */
private fun <T> handleResponse(response: Response<T>): NetworkResult<T> {
    return if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            NetworkResult.Success(body)
        } else {
            // For 204 No Content or successful response with empty body
            @Suppress("UNCHECKED_CAST")
            NetworkResult.Success(Unit as T)
        }
    } else {
        val errorBody = response.errorBody()?.string()
        val exception = parseHttpError(response.code(), errorBody)
        NetworkResult.Error(exception)
    }
}

/**
 * Convert Throwable to AppException
 */
fun Throwable.toAppException(): AppException {
    // Don't wrap CancellationException - let it propagate for proper coroutine cancellation
    if (this is CancellationException) throw this

    return when (this) {
        // Already an AppException - return as-is
        is AppException -> this

        // Custom no connectivity exception
        is NoConnectivityException -> AppException.NoConnection(message ?: "Không có kết nối mạng")

        // Network errors
        is UnknownHostException -> AppException.NoConnection(
            "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng."
        )

        is SocketTimeoutException -> AppException.Timeout(
            "Kết nối quá thời gian chờ. Vui lòng thử lại."
        )

        is SSLException -> AppException.SSLError(
            "Lỗi bảo mật kết nối. Vui lòng kiểm tra ngày giờ thiết bị."
        )

        is IOException -> AppException.NoConnection(
            message ?: "Lỗi kết nối mạng"
        )

        // HTTP errors from Retrofit
        is HttpException -> parseHttpError(code(), response()?.errorBody()?.string())

        // JSON parsing errors from Moshi
        is JsonDataException -> AppException.ParseError(
            "Lỗi xử lý dữ liệu: ${message}",
            this
        )

        is JsonEncodingException -> AppException.ParseError(
            "Lỗi mã hóa dữ liệu: ${message}",
            this
        )

        // Unknown errors
        else -> AppException.Unknown(
            message ?: "Đã xảy ra lỗi không xác định",
            this
        )
    }
}

/**
 * Parse HTTP error response to AppException
 */
private fun parseHttpError(code: Int, errorBody: String?): AppException {
    val message = parseErrorMessage(errorBody)
    return AppException.fromHttpCode(code, message)
}

/**
 * Parse error message from error body.
 * Supports common error response formats:
 * - {"message": "..."}
 * - {"error": "..."}
 * - {"errors": ["..."]}
 * - {"detail": "..."}
 */
private fun parseErrorMessage(errorBody: String?): String? {
    if (errorBody.isNullOrBlank()) return null

    return try {
        // Try common JSON error formats
        listOf(
            """"message"\s*:\s*"([^"]+)"""",
            """"error"\s*:\s*"([^"]+)"""",
            """"detail"\s*:\s*"([^"]+)"""",
            """"errors"\s*:\s*\[\s*"([^"]+)""""
        ).firstNotNullOfOrNull { pattern ->
            pattern.toRegex().find(errorBody)?.groupValues?.getOrNull(1)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse error body: $errorBody", e)
        null
    }
}

/**
 * Extension to run multiple API calls in parallel and combine results.
 *
 * Usage:
 * ```
 * val result = combineApiCalls(
 *     call1 = { apiService.getUser(id) },
 *     call2 = { apiService.getUserPosts(id) }
 * ) { user, posts ->
 *     UserWithPosts(user, posts)
 * }
 * ```
 */
suspend fun <T1, T2, R> combineApiCalls(
    call1: suspend () -> T1,
    call2: suspend () -> T2,
    combine: (T1, T2) -> R
): NetworkResult<R> {
    val result1 = safeApiCall { call1() }
    if (result1 is NetworkResult.Error) return result1

    val result2 = safeApiCall { call2() }
    if (result2 is NetworkResult.Error) return result2

    return NetworkResult.Success(
        combine(
            (result1 as NetworkResult.Success).data,
            (result2 as NetworkResult.Success).data
        )
    )
}

/**
 * Extension to run API call with retry logic.
 *
 * @param times Number of retry attempts
 * @param initialDelay Initial delay before first retry (ms)
 * @param factor Multiplier for exponential backoff
 * @param apiCall The API call to execute
 */
suspend fun <T> safeApiCallWithRetry(
    times: Int = 3,
    initialDelay: Long = 100,
    factor: Double = 2.0,
    apiCall: suspend () -> T
): NetworkResult<T> {
    var currentDelay = initialDelay
    repeat(times - 1) { attempt ->
        val result = safeApiCall(apiCall)
        if (result is NetworkResult.Success) {
            return result
        }

        // Only retry on network/timeout errors
        if (result is NetworkResult.Error) {
            val exception = result.exception
            if (exception !is AppException.NoConnection &&
                exception !is AppException.Timeout
            ) {
                return result
            }
        }

        Log.d(TAG, "Retry attempt ${attempt + 1}/$times after ${currentDelay}ms")
        kotlinx.coroutines.delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong()
    }

    return safeApiCall(apiCall)
}
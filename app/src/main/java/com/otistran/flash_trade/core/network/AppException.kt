package com.otistran.flash_trade.core.network

/**
 * Sealed hierarchy of application exceptions.
 * Provides type-safe error handling with user-friendly messages.
 */
sealed class AppException(
    override val message: String,
    val code: Int? = null,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    // ==================== HTTP Errors ====================

    /**
     * 400 Bad Request - Invalid request parameters
     */
    class BadRequest(
        message: String = "Invalid request"
    ) : AppException(message, 400)

    /**
     * 401 Unauthorized - Authentication required or token expired
     */
    class Unauthorized(
        message: String = "Session expired, please login again"
    ) : AppException(message, 401)

    /**
     * 403 Forbidden - No permission to access resource
     */
    class Forbidden(
        message: String = "Access denied"
    ) : AppException(message, 403)

    /**
     * 404 Not Found - Resource not found
     */
    class NotFound(
        message: String = "Data not found"
    ) : AppException(message, 404)

    /**
     * 409 Conflict - Resource conflict (duplicate, etc.)
     */
    class Conflict(
        message: String = "Data conflict"
    ) : AppException(message, 409)

    /**
     * 422 Unprocessable Entity - Validation error
     */
    class ValidationError(
        message: String = "Invalid data",
        val errors: Map<String, String>? = null
    ) : AppException(message, 422)

    /**
     * 429 Too Many Requests - Rate limited
     */
    class RateLimited(
        message: String = "Too many requests, please try again later"
    ) : AppException(message, 429)

    /**
     * 5xx Server Error
     */
    class ServerError(
        message: String = "Server error, please try again later",
        code: Int = 500
    ) : AppException(message, code)

    /**
     * Other HTTP errors
     */
    class HttpError(
        code: Int,
        message: String = "HTTP error $code"
    ) : AppException(message, code)

    // ==================== Network Errors ====================

    /**
     * No internet connection
     */
    class NoConnection(
        message: String = "No internet connection"
    ) : AppException(message)

    /**
     * Connection timeout
     */
    class Timeout(
        message: String = "Connection timeout"
    ) : AppException(message)

    /**
     * SSL/TLS error
     */
    class SSLError(
        message: String = "SSL connection error"
    ) : AppException(message)

    // ==================== Data Errors ====================

    /**
     * JSON parsing error
     */
    class ParseError(
        message: String = "Data processing error",
        cause: Throwable? = null
    ) : AppException(message, cause = cause)

    /**
     * Empty response body
     */
    class EmptyResponse(
        message: String = "No data returned"
    ) : AppException(message)

    // ==================== Business Logic Errors ====================

    /**
     * Business logic error from domain layer
     */
    class BusinessError(
        message: String,
        val errorCode: String? = null
    ) : AppException(message)

    // ==================== Unknown Errors ====================

    /**
     * Unknown/unexpected error
     */
    class Unknown(
        message: String = "An unknown error occurred",
        cause: Throwable? = null
    ) : AppException(message, cause = cause)

    companion object {
        /**
         * Create AppException from HTTP status code
         */
        fun fromHttpCode(code: Int, message: String? = null): AppException {
            return when (code) {
                400 -> BadRequest(message ?: "Invalid request")
                401 -> Unauthorized(message ?: "Session expired")
                403 -> Forbidden(message ?: "Access denied")
                404 -> NotFound(message ?: "Data not found")
                409 -> Conflict(message ?: "Data conflict")
                422 -> ValidationError(message ?: "Invalid data")
                429 -> RateLimited(message ?: "Too many requests")
                in 500..599 -> ServerError(message ?: "Server error", code)
                else -> HttpError(code, message ?: "HTTP error $code")
            }
        }
    }
}

/**
 * Extension to get user-displayable message from AppException
 */
val AppException.displayMessage: String
    get() = message

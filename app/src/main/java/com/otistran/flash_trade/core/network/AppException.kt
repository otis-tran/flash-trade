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
        message: String = "Yêu cầu không hợp lệ"
    ) : AppException(message, 400)

    /**
     * 401 Unauthorized - Authentication required or token expired
     */
    class Unauthorized(
        message: String = "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại"
    ) : AppException(message, 401)

    /**
     * 403 Forbidden - No permission to access resource
     */
    class Forbidden(
        message: String = "Bạn không có quyền truy cập"
    ) : AppException(message, 403)

    /**
     * 404 Not Found - Resource not found
     */
    class NotFound(
        message: String = "Không tìm thấy dữ liệu"
    ) : AppException(message, 404)

    /**
     * 409 Conflict - Resource conflict (duplicate, etc.)
     */
    class Conflict(
        message: String = "Dữ liệu bị xung đột"
    ) : AppException(message, 409)

    /**
     * 422 Unprocessable Entity - Validation error
     */
    class ValidationError(
        message: String = "Dữ liệu không hợp lệ",
        val errors: Map<String, String>? = null
    ) : AppException(message, 422)

    /**
     * 429 Too Many Requests - Rate limited
     */
    class RateLimited(
        message: String = "Quá nhiều yêu cầu, vui lòng thử lại sau"
    ) : AppException(message, 429)

    /**
     * 5xx Server Error
     */
    class ServerError(
        message: String = "Lỗi máy chủ, vui lòng thử lại sau",
        code: Int = 500
    ) : AppException(message, code)

    /**
     * Other HTTP errors
     */
    class HttpError(
        code: Int,
        message: String = "Lỗi HTTP $code"
    ) : AppException(message, code)

    // ==================== Network Errors ====================

    /**
     * No internet connection
     */
    class NoConnection(
        message: String = "Không có kết nối mạng"
    ) : AppException(message)

    /**
     * Connection timeout
     */
    class Timeout(
        message: String = "Kết nối quá thời gian chờ"
    ) : AppException(message)

    /**
     * SSL/TLS error
     */
    class SSLError(
        message: String = "Lỗi bảo mật kết nối"
    ) : AppException(message)

    // ==================== Data Errors ====================

    /**
     * JSON parsing error
     */
    class ParseError(
        message: String = "Lỗi xử lý dữ liệu",
        cause: Throwable? = null
    ) : AppException(message, cause = cause)

    /**
     * Empty response body
     */
    class EmptyResponse(
        message: String = "Không có dữ liệu trả về"
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
        message: String = "Đã xảy ra lỗi không xác định",
        cause: Throwable? = null
    ) : AppException(message, cause = cause)

    companion object {
        /**
         * Create AppException from HTTP status code
         */
        fun fromHttpCode(code: Int, message: String? = null): AppException {
            return when (code) {
                400 -> BadRequest(message ?: "Yêu cầu không hợp lệ")
                401 -> Unauthorized(message ?: "Phiên đăng nhập hết hạn")
                403 -> Forbidden(message ?: "Không có quyền truy cập")
                404 -> NotFound(message ?: "Không tìm thấy dữ liệu")
                409 -> Conflict(message ?: "Dữ liệu bị xung đột")
                422 -> ValidationError(message ?: "Dữ liệu không hợp lệ")
                429 -> RateLimited(message ?: "Quá nhiều yêu cầu")
                in 500..599 -> ServerError(message ?: "Lỗi máy chủ", code)
                else -> HttpError(code, message ?: "Lỗi HTTP $code")
            }
        }
    }
}

/**
 * Extension to get user-displayable message from AppException
 */
val AppException.displayMessage: String
    get() = message

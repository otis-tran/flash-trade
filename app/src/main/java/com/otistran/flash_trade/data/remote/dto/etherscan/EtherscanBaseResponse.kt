package com.otistran.flash_trade.data.remote.dto.etherscan

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Base response wrapper for Etherscan V2 API.
 * All endpoints return this structure.
 *
 * Handles both success responses (result is array/data) and error responses (result is error string).
 */
@JsonClass(generateAdapter = true)
data class EtherscanBaseResponse<T>(
    val status: String,     // "1" = success, "0" = error
    val message: String,    // "OK" or error message
    @Json(name = "result") val rawResult: Any? = null
) {
    /**
     * Result data if successful, null if error.
     * Cast to expected type (e.g., List<TxDto>) when used.
     */
    val result: T?
        get() = if (isSuccess) rawResult as? T else null

    /**
     * Error message if API returned an error.
     */
    val errorMessage: String?
        get() = if (isError) rawResult as? String else null

    val isSuccess: Boolean
        get() = status == "1" && message == "OK"

    val isError: Boolean
        get() = status == "0"
}

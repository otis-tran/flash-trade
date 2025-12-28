package com.otistran.flash_trade.data.remote.dto.etherscan

import com.squareup.moshi.JsonClass

/**
 * Base response wrapper for Etherscan V2 API.
 * All endpoints return this structure.
 */
@JsonClass(generateAdapter = true)
data class EtherscanBaseResponse<T>(
    val status: String,     // "1" = success, "0" = error
    val message: String,    // "OK" or error message
    val result: T?          // Actual data or error string
) {
    val isSuccess: Boolean
        get() = status == "1" && message == "OK"

    val isError: Boolean
        get() = status == "0"
}

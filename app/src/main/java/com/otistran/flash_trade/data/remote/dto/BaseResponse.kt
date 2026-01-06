package com.otistran.flash_trade.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base wrapper for API responses.
 * Use this for APIs that return standard format.
 */
@Serializable
data class ApiResponse<T>(
    @SerialName("success")
    val success: Boolean = true,

    @SerialName("data")
    val data: T? = null,

    @SerialName("error")
    val error: ErrorDto? = null,

    @SerialName("message")
    val message: String? = null
)

@Serializable
data class ErrorDto(
    @SerialName("code")
    val code: String? = null,

    @SerialName("message")
    val message: String? = null,

    @SerialName("details")
    val details: Map<String, String>? = null
)
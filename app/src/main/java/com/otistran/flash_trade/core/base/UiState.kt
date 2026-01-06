package com.otistran.flash_trade.core.base

/**
 * Marker interface for MVI states.
 *
 * All feature states should implement this interface.
 * State should be immutable (use data class).
 *
 * Example:
 * ```
 * data class LoginState(
 *     val isLoading: Boolean = false,
 *     val email: String = "",
 *     val error: String? = null
 * ) : UiState
 * ```
 */
interface UiState

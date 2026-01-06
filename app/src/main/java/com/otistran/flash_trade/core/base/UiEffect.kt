package com.otistran.flash_trade.core.base

/**
 * Marker interface for MVI side effects.
 *
 * Effects are one-time events that should not be replayed on configuration changes.
 * Examples: navigation, toasts, snackbars, dialogs.
 *
 * Example:
 * ```
 * sealed interface LoginEffect : UiEffect {
 *     data object NavigateToHome : LoginEffect
 *     data class ShowError(val message: String) : LoginEffect
 * }
 * ```
 */
interface UiEffect

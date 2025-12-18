package com.otistran.flash_trade.core.base

/**
 * Marker interface for MVI events (also called Intents).
 *
 * Events represent user actions or system events that trigger state changes.
 * Use sealed class/interface for type-safe event handling.
 *
 * Example:
 * ```
 * sealed interface LoginEvent : UiEvent {
 *     data class EmailChanged(val email: String) : LoginEvent
 *     data class PasswordChanged(val password: String) : LoginEvent
 *     data object LoginClicked : LoginEvent
 * }
 * ```
 */
interface UiEvent

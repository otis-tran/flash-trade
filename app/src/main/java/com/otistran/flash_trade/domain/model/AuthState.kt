package com.otistran.flash_trade.domain.model

/**
 * Represents current authentication state.
 * Maps to Privy SDK AuthState.
 */
enum class AuthState {
    /** User authenticated with valid session. */
    AUTHENTICATED,

    /** User not authenticated (logged out or first launch). */
    UNAUTHENTICATED,

    /** SDK initializing, state unknown. */
    NOT_READY,

    /** Session exists but needs network verification. */
    AUTHENTICATED_UNVERIFIED
}

/**
 * Authentication method for login.
 */
sealed class AuthMethod {
    data class Passkey(val relyingParty: String) : AuthMethod()
    data class OAuth(val provider: OAuthProvider, val scheme: String) : AuthMethod()
}

/**
 * OAuth provider type.
 */
enum class OAuthProvider {
    GOOGLE,
    TWITTER,
    DISCORD
}

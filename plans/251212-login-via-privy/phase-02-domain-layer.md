# Phase 02: Domain Layer

**Status:** ⏳ Pending
**Priority:** P0 (Blocking)
**Estimated Time:** 1 hour
**Dependencies:** Phase 01 (Configuration)

---

## Context

- [Phase 01: Configuration](./phase-01-configuration.md)
- [Main Plan](./plan.md)
- [Existing User Model](D:/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/domain/model/User.kt)
- [Existing Result Wrapper](D:/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/util/Result.kt)

---

## Overview

Define authentication contracts (repository interface), business models (AuthState, AuthResult), and use case for login operations. Pure Kotlin, no Android dependencies.

**Principle:** Domain layer is the source of truth for business logic.

---

## Key Insights

1. **Repository interface** abstracts auth implementation (Privy SDK)
2. **AuthState enum** represents current authentication status
3. **LoginUseCase** encapsulates business rules for login
4. **Result wrapper** provides consistent error handling
5. **Domain models** map Privy SDK responses to app models

---

## Requirements

### Functional Requirements
- Repository interface defines passkey + OAuth login contracts
- Use case validates input and coordinates auth flow
- AuthState enum tracks authentication lifecycle
- Error states mapped to domain-specific errors

### Non-Functional Requirements
- Zero Android framework dependencies
- Pure Kotlin (testable without instrumentation)
- <200 LOC per file
- Immutable data structures

---

## Architecture

```
┌─────────────────────────────────────┐
│         DOMAIN LAYER (Pure)         │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   LoginUseCase               │  │
│  │  - invoke(method, params)    │  │
│  └───────────┬──────────────────┘  │
│              │                      │
│              ▼                      │
│  ┌──────────────────────────────┐  │
│  │   AuthRepository Interface   │  │
│  │  - loginWithPasskey()        │  │
│  │  - loginWithOAuth()          │  │
│  │  - getAuthState()            │  │
│  └──────────────────────────────┘  │
│              ▲                      │
│              │                      │
│  ┌──────────────────────────────┐  │
│  │   Domain Models              │  │
│  │  - AuthState (enum)          │  │
│  │  - AuthMethod (sealed)       │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
         │
         │ Implemented by
         ▼
    Data Layer (Phase 03)
```

---

## Related Code Files

### Files to Create
- `domain/model/auth-state.kt` - Authentication state enum
- `domain/repository/auth-repository.kt` - Repository interface
- `domain/usecase/login-use-case.kt` - Login business logic

### Files to Reference
- `domain/model/User.kt` - Existing user model
- `domain/repository/UserRepository.kt` - Pattern reference
- `util/Result.kt` - Error handling wrapper

---

## Implementation Steps

### Step 1: Create AuthState Model

**File:** `app/src/main/java/com/otistran/flash_trade/domain/model/auth-state.kt`

```kotlin
package com.otistran.flash_trade.domain.model

/**
 * Represents current authentication state.
 * Maps to Privy SDK AuthState.
 */
enum class AuthState {
    /**
     * User authenticated with valid session.
     */
    AUTHENTICATED,

    /**
     * User not authenticated (logged out or first launch).
     */
    UNAUTHENTICATED,

    /**
     * SDK initializing, state unknown.
     */
    NOT_READY,

    /**
     * Session exists but needs network verification.
     */
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
    APPLE,
    TWITTER,
    DISCORD
}
```

**Lines:** ~40

---

### Step 2: Define AuthRepository Interface

**File:** `app/src/main/java/com/otistran/flash_trade/domain/repository/auth-repository.kt`

```kotlin
package com.otistran.flash_trade.domain.repository

import com.otistran.flash_trade.domain.model.AuthMethod
import com.otistran.flash_trade.domain.model.AuthState
import com.otistran.flash_trade.domain.model.OAuthProvider
import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for authentication operations.
 * Abstracts Privy SDK implementation.
 */
interface AuthRepository {
    /**
     * Observe current authentication state.
     * Emits on every state change.
     */
    fun observeAuthState(): Flow<AuthState>

    /**
     * Get current auth state (one-time).
     */
    suspend fun getAuthState(): AuthState

    /**
     * Login with passkey authentication.
     *
     * @param relyingParty Domain hosting Digital Asset Links
     * @return Result containing authenticated user or error
     */
    suspend fun loginWithPasskey(relyingParty: String): Result<User>

    /**
     * Login with OAuth provider.
     *
     * @param provider OAuth provider (Google, Apple, etc.)
     * @param appUrlScheme Custom URL scheme for callback
     * @return Result containing authenticated user or error
     */
    suspend fun loginWithOAuth(
        provider: OAuthProvider,
        appUrlScheme: String
    ): Result<User>

    /**
     * Signup with passkey (creates new account).
     *
     * @param relyingParty Domain hosting Digital Asset Links
     * @return Result containing new user or error
     */
    suspend fun signupWithPasskey(relyingParty: String): Result<User>

    /**
     * Logout current user.
     */
    suspend fun logout(): Result<Unit>
}
```

**Lines:** ~60

---

### Step 3: Implement LoginUseCase

**File:** `app/src/main/java/com/otistran/flash_trade/domain/usecase/login-use-case.kt`

```kotlin
package com.otistran.flash_trade.domain.usecase

import com.otistran.flash_trade.domain.model.AuthMethod
import com.otistran.flash_trade.domain.model.OAuthProvider
import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.util.Result
import javax.inject.Inject

/**
 * Use case for login operations.
 * Encapsulates business rules for authentication.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Execute login with specified method.
     *
     * @param method Authentication method (Passkey or OAuth)
     * @param isSignup True if creating new account
     * @return Result containing authenticated user
     */
    suspend operator fun invoke(
        method: AuthMethod,
        isSignup: Boolean = false
    ): Result<User> {
        // Validate input
        if (method is AuthMethod.Passkey && method.relyingParty.isBlank()) {
            return Result.Error("Relying party cannot be empty")
        }

        if (method is AuthMethod.OAuth && method.scheme.isBlank()) {
            return Result.Error("OAuth scheme cannot be empty")
        }

        // Execute login based on method
        return when (method) {
            is AuthMethod.Passkey -> {
                if (isSignup) {
                    authRepository.signupWithPasskey(method.relyingParty)
                } else {
                    authRepository.loginWithPasskey(method.relyingParty)
                }
            }
            is AuthMethod.OAuth -> {
                // OAuth doesn't distinguish signup/login
                authRepository.loginWithOAuth(method.provider, method.scheme)
            }
        }
    }

    /**
     * Get current authentication state.
     */
    suspend fun getAuthState() = authRepository.getAuthState()
}
```

**Lines:** ~55

---

## Todo List

- [ ] Create `domain/model/auth-state.kt`
- [ ] Define AuthState enum (4 states)
- [ ] Define AuthMethod sealed class (Passkey, OAuth)
- [ ] Define OAuthProvider enum
- [ ] Create `domain/repository/auth-repository.kt`
- [ ] Define repository interface methods
- [ ] Add KDoc comments for all public methods
- [ ] Create `domain/usecase/login-use-case.kt`
- [ ] Implement invoke operator function
- [ ] Add input validation
- [ ] Add getAuthState helper
- [ ] Verify no Android framework imports
- [ ] Build project to verify compilation

---

## Success Criteria

- [ ] All files compile without errors
- [ ] Zero Android framework dependencies
- [ ] AuthRepository interface defines all auth operations
- [ ] LoginUseCase validates input before calling repository
- [ ] AuthState enum covers all Privy states
- [ ] Code follows naming conventions (kebab-case files)
- [ ] KDoc present for public APIs
- [ ] Files under 200 LOC each

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Missing auth state | Low | Medium | Reference Privy docs for complete state list |
| Use case too complex | Low | Low | Keep logic minimal - just validation + delegation |
| Interface too broad | Low | Medium | Only expose methods needed for login flow |

---

## Security Considerations

- **No credential storage** in domain layer
- **Validation only** - no actual auth logic here
- **Immutable data structures** prevent state mutation
- **Error messages** generic (no sensitive details)

---

## Testing Recommendations

**Not required per spec, but useful:**

```kotlin
@Test
fun `should return error when passkey relying party is blank`() = runTest {
    // Given
    val useCase = LoginUseCase(mockAuthRepository)
    val method = AuthMethod.Passkey(relyingParty = "")

    // When
    val result = useCase(method)

    // Then
    assertTrue(result is Result.Error)
    assertEquals("Relying party cannot be empty", result.errorOrNull())
}
```

---

## Next Steps

After domain layer complete:
→ **Phase 03: Data Layer** - Implement AuthRepository with Privy SDK

---

## Unresolved Questions

1. Should we add `refreshSession()` to AuthRepository for token refresh?
2. Does Privy SDK support concurrent login attempts? (e.g., user taps both buttons)
3. Should AuthMethod include retry count for failed attempts?

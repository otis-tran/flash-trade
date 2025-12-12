# Phase 03: Data Layer

**Status:** ⏳ Pending
**Priority:** P0 (Blocking)
**Estimated Time:** 1.5 hours
**Dependencies:** Phase 02 (Domain Layer)

---

## Context

- [Phase 02: Domain Layer](./phase-02-domain-layer.md)
- [Research: Passkey API](./research/researcher-01-privy-passkey.md)
- [Research: OAuth API](./research/researcher-02-privy-oauth.md)
- [Main Plan](./plan.md)

---

## Overview

Implement AuthRepository interface using Privy SDK. Create wrapper service to isolate SDK calls, map Privy models to domain models, handle errors, and configure Hilt DI modules.

**Critical:** This layer bridges domain logic with Privy SDK.

---

## Key Insights

1. **Wrapper service** isolates Privy SDK for easier testing/mocking
2. **Mapper functions** convert Privy User → domain User
3. **Error mapping** translates SDK exceptions to Result.Error
4. **Hilt modules** provide singleton instances
5. **StateFlow** bridges Privy callbacks to coroutine flows
6. **Context required** for CredentialManager (injected via Hilt)

---

## Requirements

### Functional Requirements
- PrivyAuthService wraps all SDK calls
- AuthRepositoryImpl implements domain interface
- Error mapping covers all Privy exceptions
- User mapper converts Privy models to domain models
- AuthModule provides DI bindings

### Non-Functional Requirements
- Thread-safe state management
- Proper coroutine scope handling
- <200 LOC per file
- SDK errors logged for debugging

---

## Architecture

```
┌────────────────────────────────────────────────┐
│             DATA LAYER                         │
│                                                │
│  ┌──────────────────────────────────────────┐ │
│  │   AuthRepositoryImpl                     │ │
│  │   (implements AuthRepository)            │ │
│  └────────────┬─────────────────────────────┘ │
│               │                                │
│               ▼                                │
│  ┌──────────────────────────────────────────┐ │
│  │   PrivyAuthService (SDK Wrapper)         │ │
│  │   - passkey.login()                      │ │
│  │   - passkey.signup()                     │ │
│  │   - oAuth.login()                        │ │
│  │   - getAuthState()                       │ │
│  └────────────┬─────────────────────────────┘ │
│               │                                │
│               ▼                                │
│  ┌──────────────────────────────────────────┐ │
│  │   Privy SDK                              │ │
│  │   + CredentialManager (for passkey)      │ │
│  └──────────────────────────────────────────┘ │
│                                                │
│  ┌──────────────────────────────────────────┐ │
│  │   UserMapper                             │ │
│  │   - toUser(PrivyUser): User              │ │
│  └──────────────────────────────────────────┘ │
└────────────────────────────────────────────────┘
         │
         │ Injected via
         ▼
┌────────────────────────────────────────────────┐
│   AuthModule (Hilt)                            │
│   - @Provides PrivyAuthService                 │
│   - @Binds AuthRepository                      │
└────────────────────────────────────────────────┘
```

---

## Related Code Files

### Files to Create
- `data/service/privy-auth-service.kt` - Privy SDK wrapper
- `data/repository/auth-repository-impl.kt` - Repository implementation
- `data/mapper/user-mapper.kt` - Privy User → domain User
- `di/auth-module.kt` - Hilt DI configuration

### Files to Reference
- `domain/repository/AuthRepository.kt` - Interface to implement
- `domain/model/User.kt` - Target model
- Phase 01 config for OAuth scheme and Privy App ID

---

## Implementation Steps

### Step 1: Create PrivyAuthService Wrapper

**File:** `app/src/main/java/com/otistran/flash_trade/data/service/privy-auth-service.kt`

```kotlin
package com.otistran.flash_trade.data.service

import android.content.Context
import androidx.credentials.CredentialManager
import com.otistran.flash_trade.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.privy.sdk.Privy
import io.privy.sdk.PrivyUser
import io.privy.sdk.auth.AuthState as PrivyAuthState
import io.privy.sdk.oauth.OAuthProvider as PrivyOAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper service for Privy SDK operations.
 * Isolates SDK calls for easier testing and error handling.
 */
@Singleton
class PrivyAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val privy: Privy by lazy {
        Privy.initialize(context, BuildConfig.PRIVY_APP_ID)
    }

    private val credentialManager by lazy {
        CredentialManager.create(context)
    }

    private val _authStateFlow = MutableStateFlow(PrivyAuthState.NotReady)
    val authStateFlow: Flow<PrivyAuthState> = _authStateFlow

    init {
        // Observe Privy auth state
        privy.observeAuthState { state ->
            _authStateFlow.value = state
        }
    }

    /**
     * Get current auth state (one-time).
     */
    fun getAuthState(): PrivyAuthState = privy.getAuthState()

    /**
     * Login with passkey.
     * Suspends until callback completes.
     */
    suspend fun loginWithPasskey(relyingParty: String): PrivyUser {
        return suspendCancellableCoroutine { continuation ->
            privy.passkey.login(
                relyingParty = relyingParty,
                onSuccess = { user -> continuation.resume(user) },
                onError = { error -> continuation.resumeWithException(error) }
            )
        }
    }

    /**
     * Signup with passkey.
     */
    suspend fun signupWithPasskey(relyingParty: String): PrivyUser {
        return suspendCancellableCoroutine { continuation ->
            privy.passkey.signup(
                relyingParty = relyingParty,
                onSuccess = { user -> continuation.resume(user) },
                onError = { error -> continuation.resumeWithException(error) }
            )
        }
    }

    /**
     * Login with OAuth provider.
     */
    suspend fun loginWithOAuth(
        provider: PrivyOAuthProvider,
        appUrlScheme: String
    ): PrivyUser {
        return suspendCancellableCoroutine { continuation ->
            privy.oAuth.login(
                provider = provider,
                appUrlScheme = appUrlScheme
            ).onSuccess { user ->
                continuation.resume(user)
            }.onFailure { error ->
                continuation.resumeWithException(error)
            }
        }
    }

    /**
     * Logout current user.
     */
    suspend fun logout() {
        privy.logout()
    }
}
```

**Lines:** ~95

**Key Patterns:**
- `suspendCancellableCoroutine` bridges callbacks to coroutines
- `lazy` initialization prevents cold start delay
- StateFlow for reactive auth state
- Singleton scope via Hilt

---

### Step 2: Create User Mapper

**File:** `app/src/main/java/com/otistran/flash_trade/data/mapper/user-mapper.kt`

```kotlin
package com.otistran.flash_trade.data.mapper

import com.otistran.flash_trade.domain.model.User
import io.privy.sdk.PrivyUser
import io.privy.sdk.linkedAccounts.LinkedAccountType

/**
 * Maps Privy SDK User to domain User.
 */
fun PrivyUser.toUser(): User {
    // Extract email from linked accounts
    val email = linkedAccounts
        .find { it.type == LinkedAccountType.Email }
        ?.email

    // Extract Google display name/avatar
    val googleAccount = linkedAccounts
        .find { it.type == LinkedAccountType.Google }

    val displayName = googleAccount?.name ?: email?.substringBefore('@')
    val avatarUrl = googleAccount?.profilePictureUrl

    // Extract wallet address (first embedded wallet)
    val walletAddress = linkedAccounts
        .find { it.type == LinkedAccountType.Wallet }
        ?.address

    return User(
        id = this.id,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        walletAddress = walletAddress,
        isOnboarded = true // Authenticated means onboarded
    )
}
```

**Lines:** ~40

---

### Step 3: Implement AuthRepositoryImpl

**File:** `app/src/main/java/com/otistran/flash_trade/data/repository/auth-repository-impl.kt`

```kotlin
package com.otistran.flash_trade.data.repository

import android.util.Log
import com.otistran.flash_trade.data.mapper.toUser
import com.otistran.flash_trade.data.service.PrivyAuthService
import com.otistran.flash_trade.domain.model.AuthState
import com.otistran.flash_trade.domain.model.OAuthProvider
import com.otistran.flash_trade.domain.model.User
import com.otistran.flash_trade.domain.repository.AuthRepository
import com.otistran.flash_trade.util.Result
import io.privy.sdk.auth.AuthState as PrivyAuthState
import io.privy.sdk.oauth.OAuthProvider as PrivyOAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val TAG = "AuthRepositoryImpl"

/**
 * Implementation of AuthRepository using Privy SDK.
 */
class AuthRepositoryImpl @Inject constructor(
    private val privyAuthService: PrivyAuthService
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthState> {
        return privyAuthService.authStateFlow.map { it.toDomain() }
    }

    override suspend fun getAuthState(): AuthState {
        return privyAuthService.getAuthState().toDomain()
    }

    override suspend fun loginWithPasskey(relyingParty: String): Result<User> {
        return try {
            val privyUser = privyAuthService.loginWithPasskey(relyingParty)
            val user = privyUser.toUser()
            Log.d(TAG, "Passkey login success: ${user.id}")
            Result.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Passkey login failed", e)
            Result.Error(mapError(e), e)
        }
    }

    override suspend fun signupWithPasskey(relyingParty: String): Result<User> {
        return try {
            val privyUser = privyAuthService.signupWithPasskey(relyingParty)
            val user = privyUser.toUser()
            Log.d(TAG, "Passkey signup success: ${user.id}")
            Result.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Passkey signup failed", e)
            Result.Error(mapError(e), e)
        }
    }

    override suspend fun loginWithOAuth(
        provider: OAuthProvider,
        appUrlScheme: String
    ): Result<User> {
        return try {
            val privyProvider = provider.toPrivyProvider()
            val privyUser = privyAuthService.loginWithOAuth(privyProvider, appUrlScheme)
            val user = privyUser.toUser()
            Log.d(TAG, "OAuth login success: ${user.id}")
            Result.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "OAuth login failed", e)
            Result.Error(mapError(e), e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            privyAuthService.logout()
            Log.d(TAG, "Logout success")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            Result.Error("Logout failed", e)
        }
    }

    /**
     * Map Privy exceptions to user-friendly messages.
     */
    private fun mapError(exception: Exception): String {
        return when (exception) {
            is io.privy.sdk.oauth.OAuth.InvalidSchemeException ->
                "OAuth configuration error. Please contact support."
            is io.privy.sdk.oauth.OAuth.UserCancelledException ->
                "Login cancelled"
            is androidx.credentials.exceptions.CreateCredentialCancellationException ->
                "Passkey creation cancelled"
            is androidx.credentials.exceptions.GetCredentialCancellationException ->
                "Passkey login cancelled"
            is java.net.UnknownHostException, is java.net.SocketTimeoutException ->
                "Network error. Check your connection."
            else ->
                exception.message ?: "Authentication failed"
        }
    }

    /**
     * Convert Privy AuthState to domain AuthState.
     */
    private fun PrivyAuthState.toDomain(): AuthState = when (this) {
        PrivyAuthState.Authenticated -> AuthState.AUTHENTICATED
        PrivyAuthState.Unauthenticated -> AuthState.UNAUTHENTICATED
        PrivyAuthState.NotReady -> AuthState.NOT_READY
        PrivyAuthState.AuthenticatedUnverified -> AuthState.AUTHENTICATED_UNVERIFIED
    }

    /**
     * Convert domain OAuthProvider to Privy OAuthProvider.
     */
    private fun OAuthProvider.toPrivyProvider(): PrivyOAuthProvider = when (this) {
        OAuthProvider.GOOGLE -> PrivyOAuthProvider.Google
        OAuthProvider.APPLE -> PrivyOAuthProvider.Apple
        OAuthProvider.TWITTER -> PrivyOAuthProvider.Twitter
        OAuthProvider.DISCORD -> PrivyOAuthProvider.Discord
    }
}
```

**Lines:** ~130

**Key Features:**
- Comprehensive error mapping
- Logging for debugging
- Extension functions for enum conversion
- Try-catch on all SDK calls

---

### Step 4: Create AuthModule for Hilt DI

**File:** `app/src/main/java/com/otistran/flash_trade/di/auth-module.kt`

```kotlin
package com.otistran.flash_trade.di

import com.otistran.flash_trade.data.repository.AuthRepositoryImpl
import com.otistran.flash_trade.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for authentication dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}
```

**Lines:** ~25

**Note:** PrivyAuthService auto-injected via `@Inject` constructor.

---

## Todo List

- [ ] Create `data/service/privy-auth-service.kt`
- [ ] Initialize Privy SDK with BuildConfig.PRIVY_APP_ID
- [ ] Implement passkey login/signup with suspendCancellableCoroutine
- [ ] Implement OAuth login
- [ ] Add auth state observation
- [ ] Create `data/mapper/user-mapper.kt`
- [ ] Map PrivyUser to domain User
- [ ] Extract email, displayName, avatarUrl, walletAddress
- [ ] Create `data/repository/auth-repository-impl.kt`
- [ ] Implement all AuthRepository methods
- [ ] Add comprehensive error mapping
- [ ] Add logging for debugging
- [ ] Create `di/auth-module.kt`
- [ ] Bind AuthRepository to implementation
- [ ] Build project and verify compilation

---

## Success Criteria

- [ ] PrivyAuthService compiles without errors
- [ ] AuthRepositoryImpl implements all interface methods
- [ ] Error mapping covers common exceptions
- [ ] User mapper extracts all fields from PrivyUser
- [ ] AuthModule provides correct DI bindings
- [ ] No memory leaks (coroutines cancelled properly)
- [ ] Logging present for all auth operations
- [ ] Files under 200 LOC each

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Privy SDK version mismatch | Low | High | Verify imports against SDK 0.8.0 docs |
| Context leak in service | Medium | Medium | Use ApplicationContext, not Activity |
| Coroutine cancellation not handled | Low | Low | Use suspendCancellableCoroutine |
| Missing error cases | Medium | Medium | Add catch-all in mapError |
| StateFlow not updated | Low | Medium | Verify Privy callback registration |

---

## Security Considerations

- **ApplicationContext only** - prevents activity leaks
- **Error messages sanitized** - no sensitive data exposed
- **Logging excludes credentials** - only log user IDs
- **BuildConfig obfuscated** - ProGuard applied in release
- **Privy handles token storage** - no manual token management

---

## Testing Recommendations

**Manual testing:**
1. Trigger passkey login, verify CredentialManager UI
2. Trigger OAuth, verify browser redirect
3. Cancel auth flow, verify error handling
4. Disconnect network, verify timeout error

**Unit tests (optional):**
```kotlin
@Test
fun `should map user cancelled exception to friendly message`() {
    val exception = OAuth.UserCancelledException()
    val message = mapError(exception)
    assertEquals("Login cancelled", message)
}
```

---

## Next Steps

After data layer complete:
→ **Phase 04: Presentation Layer** - Build MVI components and UI

---

## Unresolved Questions

1. Does Privy SDK need explicit initialization in Application class?
2. Should we cache auth state in DataStore for offline access?
3. How to handle multiple concurrent auth attempts?
4. Should we add retry logic for network errors?

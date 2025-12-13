# Privy SDK Google OAuth Authentication for Android

**Date:** 2025-12-12
**Status:** Research Complete

## Overview
Privy SDK provides OAuth authentication for Android via `privy.oAuth.login()` with URL scheme-based redirect handling. Integration requires Privy Dashboard configuration, AndroidManifest setup, and proper error handling.

## Setup Requirements & Prerequisites

### Mandatory Pre-Steps
1. **Privy Dashboard Configuration**: Register custom URL scheme in "Authentication Settings"
   - OAuth will NOT work without this configuration
   - Scheme must be unique app-specific identifier
   - Dashboard validates and activates the scheme

2. **Android SDK Integration**
   - SDK must be initialized before OAuth login attempts
   - AuthState checking via `privy.getAuthState()` determines if user authenticated

3. **Gradle Dependencies**
   - Privy SDK already in project (`gradle/libs.versions.toml`)
   - Requires Android API 28+ (project target: 36)

## AndroidManifest.xml Configuration

Add redirect handler activity to `AndroidManifest.xml`:

```xml
<activity
    android:name="io.privy.sdk.oAuth.PrivyRedirectActivity"
    android:exported="true"
    android:launchMode="singleTask">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="com.otistran.flashtrade.privy" />
    </intent-filter>
</activity>
```

**Key Details:**
- `PrivyRedirectActivity` handles OAuth callback from Google/browser
- `exported="true"` allows Android system to route OAuth return
- `singleTask` launch mode prevents duplicate instances
- `android:autoVerify="true"` enables App Links validation
- Scheme format: `com.{company}.{appname}.{provider}` (recommended)

## Code Implementation

### 1. OAuth Login API Usage
```kotlin
// In LoginViewModel/Intent Handler
privy.oAuth.login(
    provider = OAuthProvider.Google,
    appUrlScheme = "com.otistran.flashtrade.privy"
).onSuccess { privyUser ->
    // User authenticated - retrieve linked accounts
    val googleAccount = privyUser.linkedAccounts.find {
        it.type == LinkedAccountType.Google
    }
    // Proceed to wallet creation/dashboard
}.onFailure { error ->
    // Handle error (user cancelled, network failure, etc.)
    handleOAuthError(error)
}
```

### 2. Authentication State Check
```kotlin
// Determine if user already authenticated
val authState = privy.getAuthState()
when (authState) {
    AuthState.Authenticated -> {
        // User logged in - skip login screen
        navigateToDashboard()
    }
    AuthState.Unauthenticated -> {
        // Show login options
        showLoginScreen()
    }
    AuthState.NotReady -> {
        // SDK still initializing
        showLoadingScreen()
    }
    AuthState.AuthenticatedUnverified -> {
        // Prior session, verify with network
        verifySession()
    }
}
```

### 3. URL Scheme Configuration (Code-side)
```kotlin
// Store in BuildConfig or secure config
object PrivyConfig {
    const val PRIVY_APP_ID = BuildConfig.PRIVY_APP_ID // from local.properties
    const val OAUTH_SCHEME = "com.otistran.flashtrade.privy"
}
```

## Error Handling Patterns

### Common Errors & Recovery

| Error | Cause | Recovery |
|-------|-------|----------|
| `OAuth scheme not configured` | Missing Dashboard setup | Verify Privy Dashboard has scheme registered |
| `InvalidSchemeException` | Scheme mismatch manifest vs code | Ensure manifest data android:scheme matches API call parameter |
| `NetworkError` | No internet/timeout | Retry or show offline message |
| `UserCancelledException` | User rejected OAuth consent | Return to login screen, allow retry |
| `InvalidStateException` | Session expired during flow | Request new login |

### Implementation
```kotlin
privy.oAuth.login(OAuthProvider.Google, "com.otistran.flashtrade.privy")
    .onFailure { exception ->
        when (exception) {
            is OAuth.InvalidSchemeException -> {
                emitError("Privy not configured. Contact support.")
            }
            is OAuth.UserCancelledException -> {
                emitError("Login cancelled. Try again.")
            }
            else -> {
                emitError("Authentication failed: ${exception.message}")
            }
        }
    }
```

## Best Practices

1. **One-Tap Flow**: Check `authState` on app launch - skip login if `Authenticated`
2. **Scheme Uniqueness**: Use reverse domain + provider pattern to prevent conflicts
3. **Error UX**: Differentiate user cancellation from system errors in UI feedback
4. **Session Management**: Pair OAuth with embedded wallet creation for complete onboarding
5. **URL Scheme Whitelist**: Only register OAuth schemes in Dashboard, avoid test/dev variants in production
6. **Timeout Handling**: Implement 30-60s timeout for OAuth flow to gracefully fail on network issues

## Integration Checklist

- [ ] Register OAuth scheme in Privy Developer Dashboard (prerequisites)
- [ ] Add `PrivyRedirectActivity` to `AndroidManifest.xml`
- [ ] Import `OAuthProvider.Google` and `privy.oAuth`
- [ ] Implement `privy.oAuth.login()` in login intent handler
- [ ] Add `AuthState` checking on app initialization
- [ ] Implement error handling with user-facing messages
- [ ] Test OAuth flow with physical device (scheme routing requires real APK/device)
- [ ] Pair with wallet creation for complete signup flow

## Unresolved Questions

1. Does Privy support multiple OAuth providers simultaneously (Google + Apple)?
2. What's the exact timeout for OAuth flow completion?
3. Are there rate limits for failed OAuth attempts?
4. How to handle OAuth during airplane mode / offline scenarios?

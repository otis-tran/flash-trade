# Privy SDK Passkey Authentication for Android - Research Report

**Date:** 2025-12-12
**Status:** Complete
**Scope:** Android passkey setup, configuration, API usage

---

## 1. Prerequisites & Setup Requirements

### Dependencies
```kotlin
implementation("androidx.credentials:credentials:1.6.0-beta03")
implementation("androidx.credentials:credentials-play-services-auth:1.6.0-beta03")
```

### Platform Requirements
- Android 9+ (API 28+)
- Digital Asset Links configured
- Privy Dashboard: passkey authentication enabled
- Optional: Biometric permission for bio-linked passkeys

### Certificate Configuration
1. **Get SHA256 fingerprint:**
   ```bash
   keytool -list -v -keystore <path-to-keystore> | grep SHA256
   ```

2. **Register in Privy Dashboard:** Settings → Android key hashes → add SHA256 fingerprint

3. **Permissions (AndroidManifest.xml):**
   ```xml
   <uses-permission android:name="android.permission.USE_BIOMETRIC" />
   ```

---

## 2. Digital Asset Links (assetlinks.json)

**Location:** `https://<your-domain>/.well-known/assetlinks.json`

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.your.package",
      "sha256_cert_fingerprints": ["AA:BB:CC:..."]
    }
  }
]
```

This establishes trust between app and domain for passkey verification.

---

## 3. CredentialManager Integration

### Instantiate Manager
```kotlin
private val credentialManager = CredentialManager.create(context)
```

### Passkey Signup Flow
```kotlin
val createRequest = CreatePublicKeyCredentialRequest(
    requestJson = credentialCreationOptions, // from server
    preferImmediatelyAvailableCredentials = false,
    isConditional = false
)

try {
    val credential = credentialManager.createCredential(context, createRequest)
    val publicKeyCredential = credential as PublicKeyCredential
    // Send to server: id, type, rawId, attestationObject, clientDataJSON
} catch (e: CreateCredentialException) {
    handleSignupError(e)
}
```

### Passkey Login Flow
```kotlin
val getRequest = GetPublicKeyCredentialOption(
    requestJson = credentialRequestOptions // from server
)

try {
    val credential = credentialManager.getCredential(
        context = context,
        request = GetCredentialRequest(listOf(getRequest))
    ) as PublicKeyCredential
    // Send to server: id, type, rawId, signature, clientDataJSON
} catch (e: GetCredentialException) {
    handleLoginError(e)
}
```

---

## 4. Privy API Methods

**Note:** Privy provides higher-level SDK methods that abstract CredentialManager details.

### Privy Passkey Signup
```kotlin
privy.passkey.signup(
    relyingParty = "https://your-domain.com",
    onSuccess = { user ->
        // Handle authenticated user
    },
    onError = { error ->
        // Handle error
    }
)
```

### Privy Passkey Login
```kotlin
privy.passkey.login(
    relyingParty = "https://your-domain.com",
    onSuccess = { user ->
        // Handle authenticated user
    },
    onError = { error ->
        // Handle error
    }
)
```

**Required Parameter:** `relyingParty` = domain hosting Digital Asset Links

---

## 5. Error Handling Patterns

```kotlin
when (e: CreateCredentialException) {
    is CreatePublicKeyCredentialDomException -> {
        // WebAuthn spec errors: invalid domain, duplicate credential, etc.
    }
    is CreateCredentialCancellationException -> {
        // User cancelled passkey creation
    }
    is CreateCredentialInterruptedException -> {
        // Retryable error: device state changed, timeout
    }
    is CreateCredentialProviderConfigurationException -> {
        // Missing Play Services or credential provider
    }
    is CreateCredentialCustomException -> {
        // 3rd-party SDK errors (Privy-specific)
    }
}
```

Similar exception hierarchy for login via `GetCredentialException`.

---

## 6. Server-Side Verification

After passkey response from client:

1. **Verify origin:** Extract client data, validate app SHA-256 fingerprint matches:
   - Format: `android:apk-key-hash:<base64url-SHA256>`

2. **Validate challenge:** Ensure response challenge matches sent challenge

3. **Verify signature:** Validate credentialPublicKey against client assertion

4. **Save AAGUID:** Store authenticator ID for future passkey management

---

## 7. Best Practices

- **Conditional UI:** Use `isConditional=true` for autofill suggestions
- **Immediate availability:** Set `preferImmediatelyAvailableCredentials=false` for explicit prompts
- **Biometric binding:** Users can optionally link passkeys to fingerprint/face unlock
- **Error recovery:** Implement fallback login methods (email/password)
- **User education:** Explain passkey benefits vs traditional auth
- **Testing:** Test on physical devices; emulator support limited
- **Restore credentials:** Implement device restore flow for account recovery

---

## 8. Configuration Checklist

- [ ] Dependencies added (credentials 1.6.0-beta03+)
- [ ] SHA256 fingerprint obtained from keystore
- [ ] Privy Dashboard: passkey enabled & SHA256 registered
- [ ] assetlinks.json hosted at `.well-known/` on relying party domain
- [ ] AndroidManifest.xml: `USE_BIOMETRIC` permission added
- [ ] CredentialManager instantiation in auth module
- [ ] Privy SDK initialized with `relyingParty` URL
- [ ] Error handlers implemented for all exception types
- [ ] Server-side signature verification implemented
- [ ] Tested on Android 9+ physical device

---

## Unresolved Questions

1. **Privy-specific error types:** Exact custom exceptions thrown by Privy SDK not fully documented; check Privy SDK source for `CreateCredentialCustomException` details.

2. **Shared device handling:** No guidance on managing passkeys on shared devices; verify with Privy team.

3. **Backup/sync options:** Android 14+ introduces credential sync; Privy support level unclear.

4. **Conditional UI timeouts:** Autofill dialog display duration configurable?

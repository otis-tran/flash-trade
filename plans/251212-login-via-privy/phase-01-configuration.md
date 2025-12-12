# Phase 01: Configuration

**Status:** ⏳ Pending
**Priority:** P0 (Blocking)
**Estimated Time:** 1 hour
**Dependencies:** None

---

## Context

- [Research: Passkey Setup](./research/researcher-01-privy-passkey.md)
- [Research: OAuth Setup](./research/researcher-02-privy-oauth.md)
- [Main Plan](./plan.md)

---

## Overview

Configure Android app and Privy Dashboard for passkey + OAuth authentication. Includes AndroidManifest setup, Digital Asset Links, SHA256 fingerprint registration, and OAuth scheme configuration.

**Critical:** This phase blocks all others - auth won't work without proper config.

---

## Key Insights

1. **Passkey requires SHA256 fingerprint** registered in Privy Dashboard
2. **OAuth requires unique URL scheme** configured both in Dashboard and manifest
3. **Digital Asset Links** must be hosted at `https://<domain>/.well-known/assetlinks.json`
4. **PrivyRedirectActivity** handles OAuth callbacks from browser
5. **App must be signed** to generate valid fingerprint (debug keystore OK for dev)

---

## Requirements

### Functional Requirements
- Privy SDK recognizes app as authorized client
- OAuth callback redirects to app (not browser)
- Passkey associates credentials with app domain
- App can make authenticated Privy API calls

### Non-Functional Requirements
- Zero cold start delay from config
- Config survives app updates
- Works on API 28+ devices

---

## Architecture Impact

```
[Privy Dashboard] ──────► [App Verification]
      │                          │
      ├─ SHA256 fingerprint      ├─ AndroidManifest
      ├─ OAuth scheme            ├─ App signature
      └─ App ID                  └─ Digital Asset Links

[Digital Asset Links JSON] ◄──── Hosted on domain
      │
      └─ Verifies app ownership
```

---

## Related Code Files

### Files to Modify
- `app/src/main/AndroidManifest.xml` - Add PrivyRedirectActivity, permissions
- `app/build.gradle.kts` - Add credentials dependencies

### Files to Reference
- `gradle/libs.versions.toml` - Check Privy SDK version

---

## Implementation Steps

### Step 1: Get App Signing Certificate Fingerprint

**Debug keystore (development):**
```bash
# Windows (MinGW)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA256

# Extract output format: AA:BB:CC:DD:...
```

**Release keystore (production):**
```bash
keytool -list -v -keystore /path/to/release.keystore -alias <your-alias> | grep SHA256
```

**Save fingerprint** for next step.

---

### Step 2: Configure Privy Dashboard

1. **Login to Privy Developer Console:** https://console.privy.io
2. **Navigate to:** Settings → Android Configuration
3. **Add SHA256 fingerprint** from Step 1
4. **Enable authentication methods:**
   - ✅ Passkeys
   - ✅ Google OAuth
5. **Register OAuth URL scheme:**
   - Scheme: `com.otistran.flashtrade.privy`
   - Format: `<scheme>://<host>` (host can be omitted)
6. **Copy Privy App ID** (needed for BuildConfig)

---

### Step 3: Add Dependencies to `app/build.gradle.kts`

Add after existing dependencies:

```kotlin
// Credentials API for passkey
implementation("androidx.credentials:credentials:1.6.0-beta03")
implementation("androidx.credentials:credentials-play-services-auth:1.6.0-beta03")
```

**Note:** Privy SDK 0.8.0 already in gradle - no change needed.

---

### Step 4: Update AndroidManifest.xml

**Location:** `app/src/main/AndroidManifest.xml`

#### 4A: Add Permissions (before `<application>`)

```xml
<!-- Biometric for passkey (optional enhancement) -->
<uses-permission android:name="android.permission.USE_BIOMETRIC" />

<!-- Internet (already present, verify) -->
<uses-permission android:name="android.permission.INTERNET" />
```

#### 4B: Add PrivyRedirectActivity (inside `<application>`)

```xml
<!-- OAuth Redirect Handler -->
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

**Key Attributes:**
- `exported="true"` - Allow system to launch for OAuth callback
- `launchMode="singleTask"` - Prevent duplicate instances
- `autoVerify="true"` - Enable App Links verification

---

### Step 5: Configure Privy App ID in BuildConfig

**Option A: local.properties (recommended for dev)**

Edit `local.properties`:
```properties
PRIVY_APP_ID=clxxx-your-app-id-here
```

Modify `app/build.gradle.kts` defaultConfig:
```kotlin
defaultConfig {
    // ... existing config

    // Load from local.properties
    val properties = Properties()
    properties.load(project.rootProject.file("local.properties").inputStream())
    buildConfigField("String", "PRIVY_APP_ID", "\"${properties.getProperty("PRIVY_APP_ID")}\"")
}

buildFeatures {
    buildConfig = true // Enable BuildConfig generation
}
```

**Option B: Environment variable (CI/CD)**
```kotlin
buildConfigField("String", "PRIVY_APP_ID", "\"${System.getenv("PRIVY_APP_ID") ?: ""}\"")
```

---

### Step 6: Digital Asset Links Configuration

**Required for passkey verification.**

#### 6A: Create assetlinks.json

**Location:** `https://<your-relying-party-domain>/.well-known/assetlinks.json`

**Content:**
```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.otistran.flash_trade",
      "sha256_cert_fingerprints": [
        "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"
      ]
    }
  }
]
```

**Replace:**
- `sha256_cert_fingerprints` with value from Step 1
- `package_name` with actual package (already correct)

#### 6B: Host File

Upload to web server or use GitHub Pages:
- URL must be HTTPS
- Must be publicly accessible
- File must return `Content-Type: application/json`

**Verify hosting:**
```bash
curl https://<your-domain>/.well-known/assetlinks.json
```

---

### Step 7: Verify Configuration

#### 7A: Build app
```bash
./gradlew assembleDebug
```

#### 7B: Install on device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### 7C: Verify App Links
```bash
adb shell pm get-app-links com.otistran.flash_trade

# Expected output:
# com.otistran.flash_trade:
#   ID: <uuid>
#   Signatures: [<your-domain>]
#   Domain verification state:
#     <your-domain>: verified
```

#### 7D: Test OAuth scheme
```bash
adb shell am start -a android.intent.action.VIEW -d "com.otistran.flashtrade.privy://callback"

# Should launch app (may show chooser if multiple handlers)
```

---

## Todo List

- [ ] Extract SHA256 fingerprint from keystore
- [ ] Register fingerprint in Privy Dashboard
- [ ] Enable Passkey + Google OAuth in Dashboard
- [ ] Register OAuth scheme in Dashboard
- [ ] Copy Privy App ID from Dashboard
- [ ] Add credentials dependencies to build.gradle.kts
- [ ] Add USE_BIOMETRIC permission to AndroidManifest
- [ ] Add PrivyRedirectActivity to AndroidManifest
- [ ] Configure PRIVY_APP_ID in local.properties
- [ ] Enable BuildConfig in gradle
- [ ] Create assetlinks.json with correct fingerprint
- [ ] Host assetlinks.json at relying party domain
- [ ] Verify App Links with adb pm get-app-links
- [ ] Test OAuth scheme with adb am start
- [ ] Sync gradle and rebuild project

---

## Success Criteria

- [ ] App builds without errors
- [ ] PrivyRedirectActivity present in AndroidManifest
- [ ] SHA256 fingerprint registered in Privy Dashboard
- [ ] OAuth scheme `com.otistran.flashtrade.privy` registered
- [ ] BuildConfig.PRIVY_APP_ID accessible in code
- [ ] assetlinks.json hosted and publicly accessible
- [ ] `adb pm get-app-links` shows domain as "verified"
- [ ] OAuth scheme test launches app

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Wrong fingerprint format | Medium | High | Use exact output from keytool, verify colon separators |
| assetlinks.json 404 | Medium | High | Test URL in browser before proceeding |
| OAuth scheme typo | Medium | Medium | Copy-paste from single source of truth |
| App Links not verified | Medium | High | Wait 20s after install, re-verify with adb |
| Privy App ID not found | Low | High | Double-check local.properties path, verify gradle sync |

---

## Security Considerations

- **DO NOT commit** `local.properties` to git (already in .gitignore)
- **DO NOT hardcode** Privy App ID in source code
- **DO use** release keystore for production builds
- **DO verify** assetlinks.json uses HTTPS only
- **DO rotate** keys if fingerprint exposed

---

## Next Steps

After configuration verified:
→ **Phase 02: Domain Layer** - Define auth contracts and use cases

---

## Unresolved Questions

1. **Relying party domain:** What's the actual domain for Digital Asset Links? (Needs backend team input)
2. **Multiple environments:** Should debug/release use different OAuth schemes?
3. **Privy SDK init:** Does SDK auto-initialize or require Application class setup?

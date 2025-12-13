# Phase 02: Splash Screen Design Implementation

**Phase:** 02 of 02
**Focus:** Android 12+ SplashScreen API integration
**Duration:** 20-30 min

## Objective

Implement modern splash screen using Android 12+ SplashScreen API with backward compatibility for API 28-30. Display Kyber-branded lightning bolt icon on KyberNavy background during app cold start, creating premium first impression.

## Design Rationale

### Visual Continuity
- Use same icon from Phase 01 (lightning bolt)
- Match background to app theme (KyberNavy)
- Seamless transition from splash to MainActivity
- No jarring color/style changes

### Performance
- Solid background color (no gradients/images)
- System-managed animation (fade-in)
- 1000ms max display time
- No custom animations initially (KISS principle)

### Platform Compliance
- Android 12+ (API 31+): Use SplashScreen API attributes
- Android 11- (API 28-30): Use legacy theme approach
- Single icon asset (no duplication)

## Technical Specifications

### Android 12+ Splash Screen API

**Key Attributes (API 31+):**
- `windowSplashScreenBackground` - Background color (KyberNavy)
- `windowSplashScreenAnimatedIcon` - Icon drawable (adaptive icon)
- `windowSplashScreenAnimationDuration` - Duration (1000ms)
- `windowSplashScreenIconBackgroundColor` - Optional icon background (transparent)

**Behavior:**
- System displays splash automatically during cold start
- Icon centered with 1/3 screen height max
- Fade-in animation (system default)
- Replaced by app content when ready

### Legacy Approach (API 28-30)

**Key Attributes (API 28-30):**
- `android:windowBackground` - Drawable with icon + background
- `android:windowDisablePreview` - false (show preview window)

**Implementation:**
- Create layer-list drawable combining background + icon
- Set as windowBackground in theme
- Remove in onCreate() of MainActivity

## Implementation Steps

### Step 1: Update Base Theme (API 28+)
**File:** `app/src/main/res/values/themes.xml`

**Actions:**
1. Replace existing minimal theme
2. Add splash screen base attributes
3. Configure for backward compatibility

**New Structure:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Base application theme (API 28+) -->
    <style name="Theme.FlashTrade" parent="Theme.Material3.Dark.NoActionBar">
        <!-- Primary brand color -->
        <item name="colorPrimary">#31CB9E</item>
        <item name="colorPrimaryVariant">#1FA77D</item>
        <item name="colorOnPrimary">#FFFFFF</item>

        <!-- Secondary brand color -->
        <item name="colorSecondary">#7B61FF</item>
        <item name="colorOnSecondary">#FFFFFF</item>

        <!-- Background colors -->
        <item name="android:colorBackground">#141927</item>
        <item name="colorSurface">#262D44</item>
        <item name="colorOnSurface">#FFFFFF</item>

        <!-- Status bar (API 28-30 fallback) -->
        <item name="android:statusBarColor">#141927</item>
        <item name="android:navigationBarColor">#141927</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>

    <!-- Splash screen theme (API 28-30 legacy) -->
    <style name="Theme.FlashTrade.Splash" parent="Theme.FlashTrade">
        <item name="android:windowBackground">@drawable/splash_background</item>
        <item name="android:windowDisablePreview">false</item>
    </style>
</resources>
```

### Step 2: Create Android 12+ Theme (API 31+)
**File:** `app/src/main/res/values-v31/themes.xml` (create new)

**Actions:**
1. Create values-v31 resource directory
2. Override splash attributes for Android 12+
3. Reference adaptive icon

**Structure:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Splash screen theme for Android 12+ (API 31+) -->
    <style name="Theme.FlashTrade.Splash" parent="Theme.FlashTrade">
        <!-- Splash screen background -->
        <item name="android:windowSplashScreenBackground">#141927</item>

        <!-- Splash screen icon (uses adaptive icon) -->
        <item name="android:windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>

        <!-- Icon background (transparent, icon has its own background) -->
        <item name="android:windowSplashScreenIconBackgroundColor">@android:color/transparent</item>

        <!-- Animation duration (1000ms) -->
        <item name="android:windowSplashScreenAnimationDuration">1000</item>

        <!-- Status/nav bar colors -->
        <item name="android:statusBarColor">#141927</item>
        <item name="android:navigationBarColor">#141927</item>
    </style>
</resources>
```

### Step 3: Create Legacy Splash Drawable (API 28-30)
**File:** `app/src/main/res/drawable/splash_background.xml` (create new)

**Actions:**
1. Create layer-list combining background + centered icon
2. Use same colors as Android 12+ version
3. Icon size: 288dp (matches Android 12 spec)

**Structure:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android"
    android:opacity="opaque">

    <!-- Background layer (KyberNavy) -->
    <item android:drawable="@color/kyber_navy" />

    <!-- Centered icon layer -->
    <item>
        <bitmap
            android:gravity="center"
            android:src="@mipmap/ic_launcher" />
    </item>
</layer-list>
```

**Note:** Uses mipmap icon (adaptive icon will be flattened for legacy devices).

### Step 4: Add Color Resource
**File:** `app/src/main/res/values/colors.xml`

**Actions:**
1. Add kyber_navy color resource (if not exists)
2. Ensures consistency across themes

**Add:**
```xml
<color name="kyber_navy">#141927</color>
```

### Step 5: Update AndroidManifest.xml
**File:** `app/src/main/AndroidManifest.xml`

**Actions:**
1. Set splash theme on <application> or MainActivity
2. Ensure theme switched to main theme in MainActivity.onCreate()

**Expected:**
```xml
<activity
    android:name=".MainActivity"
    android:theme="@style/Theme.FlashTrade.Splash"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### Step 6: Update MainActivity
**File:** `app/src/main/java/com/otistran/flash_trade/MainActivity.kt`

**Actions:**
1. Switch from splash theme to main theme before setContent()
2. Install SplashScreen API (handles both API levels)

**Add before setContent():**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // Install splash screen (handles API 28-36 automatically)
    installSplashScreen()

    // Switch to main theme
    setTheme(R.style.Theme_FlashTrade)

    super.onCreate(savedInstanceState)
    setContent {
        // Existing compose content
    }
}
```

**Import:**
```kotlin
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
```

## Dependency Requirements

### Add SplashScreen Library
**File:** `app/build.gradle.kts`

**Add to dependencies:**
```kotlin
dependencies {
    // Splash screen (backward compatible API)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Existing dependencies...
}
```

**Sync Gradle** after adding.

## Behavior by API Level

| API Level | Implementation | Icon Source | Animation |
|-----------|----------------|-------------|-----------|
| 28-30 | Legacy theme + drawable | Mipmap (flattened) | None (instant) |
| 31-36 | SplashScreen API | Adaptive icon | System fade-in |

## Testing Checklist

### API Level Testing
- [ ] Test on API 28 emulator (Android 9 Pie)
- [ ] Test on API 30 emulator (Android 11)
- [ ] Test on API 31 emulator (Android 12)
- [ ] Test on API 34 emulator (Android 14)
- [ ] Test on API 36 emulator (Android 16, if available)

### Visual Testing
- [ ] Splash background matches app background (no flash)
- [ ] Icon centered and appropriately sized
- [ ] Smooth transition to MainActivity
- [ ] No double-splash (legacy + new)

### Performance Testing
- [ ] Cold start displays splash immediately
- [ ] Splash dismisses when content ready (not fixed 1000ms)
- [ ] No delay after splash dismissed

### Theme Testing
- [ ] Status bar color matches background (#141927)
- [ ] Navigation bar color matches background
- [ ] Light status bar icons disabled (dark background)

## Success Criteria

1. **Visual consistency:** Splash matches app theme colors exactly
2. **Platform compliance:** Works on API 28-36 without errors
3. **Performance:** No perceived delay beyond necessary cold start time
4. **Smooth transition:** No color flash or jump between splash and app content

## Potential Issues & Solutions

| Issue | Solution |
|-------|----------|
| Double splash on API 31+ | Ensure only splash theme set, not windowBackground override |
| Icon too large on legacy | Use bitmap with explicit width/height in layer-list |
| Splash persists too long | Ensure installSplashScreen() called before super.onCreate() |
| Theme not switching | Verify setTheme() called before setContent() |

## Advanced Customization (Future)

### Custom Exit Animation (Optional)
```kotlin
installSplashScreen().setOnExitAnimationListener { splashScreenView ->
    // Custom fade-out or slide animation
    ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f).apply {
        duration = 300L
        doOnEnd { splashScreenView.remove() }
        start()
    }
}
```

### Keep Splash Longer (Conditional)
```kotlin
installSplashScreen().setKeepOnScreenCondition {
    // Keep splash while loading critical data
    viewModel.isLoadingInitialData.value
}
```

**Not in scope for initial implementation** (YAGNI principle).

## Deliverables

1. **values/themes.xml** - Base theme + legacy splash theme
2. **values-v31/themes.xml** - Android 12+ splash theme
3. **drawable/splash_background.xml** - Legacy splash drawable
4. **colors.xml** - kyber_navy color resource
5. **MainActivity.kt** - installSplashScreen() integration
6. **build.gradle.kts** - SplashScreen library dependency

## Completion Verification

After implementation:
1. Build app (./gradlew assembleDebug)
2. Install on API 28, 31, 34 emulators
3. Close app completely (swipe from recents)
4. Relaunch and observe splash screen
5. Verify smooth transition to MainActivity

## Next Steps

After Phase 02 complete:
1. Test on physical devices (if available)
2. Capture screenshots for documentation
3. Consider animated splash (future enhancement)
4. Update project README with new branding

# Phase 02: Theme Implementation

**Parent:** [plan.md](./plan.md)
**Dependencies:** Phase 01 (Color Palette)
**Status:** Pending
**Priority:** P0

---

## Overview

Update Theme.kt with Material3 color schemes using Kyber brand colors. Disable dynamic colors to maintain brand consistency.

---

## Key Insights

- Current Theme.kt uses default purple/pink from Android template
- Dynamic colors enabled (overrides brand on Android 12+)
- Need complete dark and light color schemes
- Material3 requires specific color roles

---

## Material3 Color Scheme Mapping

### Dark Color Scheme (Primary)

| M3 Role | Kyber Color | HEX |
|---------|-------------|-----|
| primary | KyberTeal | #31CB9E |
| onPrimary | OnKyberTeal | #FFFFFF |
| primaryContainer | KyberTealContainer | #0D3D2E |
| onPrimaryContainer | KyberTealLight | #6FDFBD |
| secondary | KyberPurple | #7B61FF |
| onSecondary | White | #FFFFFF |
| secondaryContainer | #2D2650 | #2D2650 |
| onSecondaryContainer | #D4CBFF | #D4CBFF |
| tertiary | KyberBlue | #4C9AFF |
| onTertiary | White | #FFFFFF |
| tertiaryContainer | InfoContainer | #0D2A4D |
| onTertiaryContainer | #B8D4FF | #B8D4FF |
| error | Error | #FF6B6B |
| onError | White | #FFFFFF |
| errorContainer | ErrorContainer | #3D1A1A |
| onErrorContainer | #FFB4B4 | #FFB4B4 |
| background | KyberNavy | #141927 |
| onBackground | TextPrimary | #FFFFFF |
| surface | KyberNavyLight | #1E2438 |
| onSurface | TextPrimary | #FFFFFF |
| surfaceVariant | KyberNavySurface | #262D44 |
| onSurfaceVariant | TextSecondary | #B8C1D6 |
| outline | KyberNavyBorder | #3A4461 |
| outlineVariant | #2D3548 | #2D3548 |
| inverseSurface | SurfaceLight | #F5F7FA |
| inverseOnSurface | TextLightPrimary | #141927 |
| inversePrimary | KyberTealDark | #1FA77D |
| surfaceTint | KyberTeal | #31CB9E |

### Light Color Scheme (Secondary)

| M3 Role | Kyber Color | HEX |
|---------|-------------|-----|
| primary | KyberTealDark | #1FA77D |
| onPrimary | White | #FFFFFF |
| primaryContainer | #A8F5D9 | #A8F5D9 |
| onPrimaryContainer | #00331F | #00331F |
| secondary | #5D4AB3 | #5D4AB3 |
| onSecondary | White | #FFFFFF |
| secondaryContainer | #E8E0FF | #E8E0FF |
| onSecondaryContainer | #1A0A4D | #1A0A4D |
| tertiary | #0066CC | #0066CC |
| onTertiary | White | #FFFFFF |
| tertiaryContainer | #D6E8FF | #D6E8FF |
| onTertiaryContainer | #001A33 | #001A33 |
| error | #C62828 | #C62828 |
| onError | White | #FFFFFF |
| errorContainer | #FFE0E0 | #FFE0E0 |
| onErrorContainer | #410000 | #410000 |
| background | SurfaceLight | #F5F7FA |
| onBackground | TextLightPrimary | #141927 |
| surface | White | #FFFFFF |
| onSurface | TextLightPrimary | #141927 |
| surfaceVariant | SurfaceLightVariant | #E8ECF2 |
| onSurfaceVariant | TextLightSecondary | #4A5568 |
| outline | #8E99A4 | #8E99A4 |
| outlineVariant | #C4CDD9 | #C4CDD9 |
| inverseSurface | KyberNavy | #141927 |
| inverseOnSurface | TextPrimary | #FFFFFF |
| inversePrimary | KyberTeal | #31CB9E |
| surfaceTint | KyberTealDark | #1FA77D |

---

## Theme.kt Implementation

```kotlin
package com.otistran.flash_trade.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = KyberTeal,
    onPrimary = OnKyberTeal,
    primaryContainer = KyberTealContainer,
    onPrimaryContainer = KyberTealLight,
    secondary = KyberPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D2650),
    onSecondaryContainer = Color(0xFFD4CBFF),
    tertiary = KyberBlue,
    onTertiary = Color.White,
    tertiaryContainer = InfoContainer,
    onTertiaryContainer = Color(0xFFB8D4FF),
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFFFFB4B4),
    background = KyberNavy,
    onBackground = TextPrimary,
    surface = KyberNavyLight,
    onSurface = TextPrimary,
    surfaceVariant = KyberNavySurface,
    onSurfaceVariant = TextSecondary,
    outline = KyberNavyBorder,
    outlineVariant = Color(0xFF2D3548),
    inverseSurface = SurfaceLight,
    inverseOnSurface = TextLightPrimary,
    inversePrimary = KyberTealDark,
    surfaceTint = KyberTeal
)

private val LightColorScheme = lightColorScheme(
    primary = KyberTealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8F5D9),
    onPrimaryContainer = Color(0xFF00331F),
    secondary = Color(0xFF5D4AB3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E0FF),
    onSecondaryContainer = Color(0xFF1A0A4D),
    tertiary = Color(0xFF0066CC),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E8FF),
    onTertiaryContainer = Color(0xFF001A33),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0E0),
    onErrorContainer = Color(0xFF410000),
    background = SurfaceLight,
    onBackground = TextLightPrimary,
    surface = Color.White,
    onSurface = TextLightPrimary,
    surfaceVariant = SurfaceLightVariant,
    onSurfaceVariant = TextLightSecondary,
    outline = Color(0xFF8E99A4),
    outlineVariant = Color(0xFFC4CDD9),
    inverseSurface = KyberNavy,
    inverseOnSurface = TextPrimary,
    inversePrimary = KyberTeal,
    surfaceTint = KyberTealDark
)

@Composable
fun FlashTradeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Disable dynamic colors to maintain Kyber brand
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Set status bar color
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## Related Code Files

| File | Action | Purpose |
|------|--------|---------|
| `ui/theme/Color.kt` | Modify | Add Kyber color definitions |
| `ui/theme/Theme.kt` | Modify | Update color schemes |

---

## Implementation Steps

1. Add all Kyber colors to Color.kt
2. Remove default purple/pink colors
3. Replace DarkColorScheme in Theme.kt
4. Replace LightColorScheme in Theme.kt
5. Remove dynamic color logic
6. Add status bar color handling
7. Rename theme function to FlashTradeTheme (consistent naming)

---

## Todo List

- [ ] Update Color.kt per Phase 01
- [ ] Update DarkColorScheme
- [ ] Update LightColorScheme
- [ ] Remove dynamicColor parameter
- [ ] Add status bar styling
- [ ] Test dark mode UI
- [ ] Test light mode UI
- [ ] Verify on physical device

---

## Success Criteria

- Theme applies Kyber colors consistently
- Dark mode is default and visually polished
- Light mode is functional
- Status bar matches theme
- No default Android colors remaining
- Material3 components styled correctly

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Colors not applying | High | Import Color.kt colors correctly |
| Status bar issues | Low | Use WindowCompat API |
| Typography contrast | Medium | Use TextPrimary/Secondary |

---

## Security Considerations

None - pure UI theming.

---

## Next Steps

1. Implement changes
2. Visual QA across all screens
3. Adjust if needed based on real UI testing

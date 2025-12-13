# Flash Trade App Icon & Splash Screen Design Plan

**Plan ID:** 251213-app-icon-splash-screen-design
**Created:** 2025-12-13
**Status:** Planning
**Owner:** Planner Agent

## Overview

Replace default Android robot icon with Kyber-branded Flash Trade visual identity. Implement premium Web3 aesthetic using Kyber brand colors, lightning bolt symbol for speed/flash trading concept, and Android 12+ SplashScreen API compliance.

## Problem Statement

Current state: Generic Android robot icon (#3DDC84 green) provides no brand recognition or connection to Kyber ecosystem. App lacks professional identity and fails to communicate its core value proposition (ultra-fast trading) visually.

## Goals

1. **Brand Alignment** - Integrate Kyber visual identity (teal/navy palette, premium aesthetic)
2. **Speed Symbolism** - Lightning bolt conveys "flash" trading concept instantly
3. **Platform Compliance** - Android adaptive icon specs (108dp canvas, 72dp safe zone)
4. **Modern Standards** - Android 12+ SplashScreen API with backward compatibility
5. **Professional Quality** - Premium Web3 look competitive with top crypto apps

## Design Strategy

### Core Symbol: Lightning Bolt
- Represents speed, power, instantaneous action
- Aligns with "Flash Trade" naming
- Minimal, recognizable at all scales (48x48dp to 108x108dp)
- Sharp angles for energy, rounded edges for approachability

### Color Palette
- **Background:** KyberNavy (#141927) - deep, OLED-friendly dark
- **Foreground:** KyberTeal gradient (#31CB9E → #6FDFBD) - brand primary, vibrant contrast
- **Accent (optional):** KyberPurple (#7B61FF) for glow effects

### Visual Direction
- Dark OLED Luxury + Cyberpunk edge
- No gradients on background (solid navy for performance)
- Clean vector shapes (no raster, infinite scalability)
- Works on light/dark launcher backgrounds

## Architecture

### Files to Modify
1. **Foreground Layer:** `app/src/main/res/drawable/ic_launcher_foreground.xml`
   - Current: Android robot pathData
   - New: Lightning bolt vector paths

2. **Background Layer:** `app/src/main/res/drawable/ic_launcher_background.xml`
   - Current: #3DDC84 green + grid overlay
   - New: Solid #141927 (KyberNavy)

3. **Theme Configuration:** `app/src/main/res/values/themes.xml`
   - Add splash screen attributes
   - Reference icon and background

4. **Theme v31+:** `app/src/main/res/values-v31/themes.xml` (create new)
   - Android 12+ specific attributes
   - windowSplashScreenAnimatedIcon, windowSplashScreenBackground

### Adaptive Icon Structure
```
108dp canvas (full area)
  ├─ 72dp safe zone (always visible)
  │    └─ Lightning bolt centered here
  └─ 36dp outer margin (may be masked by launcher)
```

## Technical Requirements

### Android Adaptive Icon Specs
- Canvas: 108x108dp
- Safe zone: 72x72dp (centered)
- Icon displayed: 48x48dp typical
- Formats: Vector XML (VectorDrawable)
- Mask shapes: Circle, square, rounded square (launcher decides)

### Android 12 Splash Screen API
- Duration: 1000ms max recommended
- Icon size: 288dp max (192dp recommended)
- Background: Solid color (no gradients for performance)
- Animation: Fade-in (system default, no custom animation initially)

### Color Accessibility
- KyberTeal (#31CB9E) on KyberNavy (#141927) → **9.24:1 contrast** (WCAG AAA)
- Safe for colorblind users (blue-green distinction preserved)

## Implementation Phases

### Phase 01: App Icon Design
**File:** `phase-01-app-icon-design.md`

**Scope:**
- Design lightning bolt vector paths
- Implement foreground layer with gradient
- Replace background with solid KyberNavy
- Ensure 72dp safe zone compliance
- Test on circle/square/rounded masks

**Deliverables:**
- `ic_launcher_foreground.xml` (lightning bolt)
- `ic_launcher_background.xml` (solid navy)

### Phase 02: Splash Screen Implementation
**File:** `phase-02-splash-screen-design.md`

**Scope:**
- Configure themes.xml for base splash
- Create themes-v31.xml for Android 12+
- Set splash duration and colors
- Test backward compatibility (API 28-30)
- Test Android 12+ behavior (API 31-36)

**Deliverables:**
- `values/themes.xml` (updated)
- `values-v31/themes.xml` (new)

## Success Criteria

### Visual Quality
- Lightning bolt recognizable at 48x48dp
- Clean rendering on all launcher mask shapes
- Premium aesthetic matching Kyber brand
- No pixelation or aliasing

### Technical Compliance
- Adaptive icon guidelines met (108dp canvas, 72dp safe zone)
- Android 12+ SplashScreen API working
- Backward compatibility (API 28-30)
- Vector-only (no raster fallbacks needed)

### Brand Consistency
- Kyber color palette used exclusively
- Matches existing theme in Color.kt
- Professional quality competitive with top crypto apps

## Dependencies

- Existing Color.kt values (already defined)
- No new libraries required (Android SDK only)
- No design assets from external teams

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Lightning bolt too complex for small sizes | Medium | Use minimal 3-segment bolt design |
| Gradient performance on splash | Low | Use solid colors on background |
| Launcher-specific rendering issues | Low | Test on Pixel, Samsung, OnePlus launchers |

## Testing Strategy

1. **Icon rendering:** Test on 3+ launcher apps (Pixel, Nova, Samsung)
2. **Size validation:** View at 48dp, 72dp, 108dp
3. **Mask shapes:** Circle, square, rounded square, squircle
4. **Splash screen:** API 28, 30, 31, 34, 36 emulators
5. **Light/dark mode:** Ensure readability on both

## Timeline Estimate

- Phase 01 (Icon): 30-45 min
- Phase 02 (Splash): 20-30 min
- Testing: 15-20 min
- **Total: 65-95 min**

## Notes

- Lightning bolt should be dynamic (angled, energetic) not static
- Avoid over-complication (KISS principle)
- Premium feel but minimal file size
- Future: Consider animated splash (not in scope)

## References

- Android Adaptive Icons: https://developer.android.com/develop/ui/views/launch/icon_design_adaptive
- Android 12 Splash Screen: https://developer.android.com/develop/ui/views/launch/splash-screen
- Kyber Brand: Color.kt (existing implementation)

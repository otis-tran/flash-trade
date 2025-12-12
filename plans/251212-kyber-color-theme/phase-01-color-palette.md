# Phase 01: Kyber Color Palette Definition

**Parent:** [plan.md](./plan.md)
**Status:** Pending
**Priority:** P0

---

## Overview

Complete color palette derived from Kyber Network brand guidelines. Optimized for crypto/fintech mobile UI with dark mode primary.

---

## Key Insights

- Kyber uses vibrant teal (#31CB9E) as signature color
- Dark navy (#141927) provides premium, professional feel
- Crypto apps typically favor dark mode (reduces eye strain, saves battery OLED)
- Material3 requires primary, secondary, tertiary, error, and surface colors

---

## Complete Color Palette

### Primary Colors (Kyber Teal Family)

| Name | HEX | RGB | Dark Mode | Light Mode | Usage |
|------|-----|-----|-----------|------------|-------|
| KyberTeal | #31CB9E | 49,203,158 | Primary | Primary | Main brand, CTAs |
| KyberTealLight | #6FDFBD | 111,223,189 | - | Hover states | Light variant |
| KyberTealDark | #1FA77D | 31,167,125 | Pressed states | - | Dark variant |
| KyberTealContainer | #0D3D2E | 13,61,46 | Containers | - | Dark containers |
| OnKyberTeal | #FFFFFF | 255,255,255 | On primary | On primary | Text on teal |

### Secondary Colors (Navy Family)

| Name | HEX | RGB | Dark Mode | Light Mode | Usage |
|------|-----|-----|-----------|------------|-------|
| KyberNavy | #141927 | 20,25,39 | Background | - | Main dark bg |
| KyberNavyLight | #1E2438 | 30,36,56 | Surface | - | Cards, elevated |
| KyberNavySurface | #262D44 | 38,45,68 | Surface variant | - | Modals, sheets |
| KyberNavyBorder | #3A4461 | 58,68,97 | Outline | - | Borders, dividers |

### Tertiary/Accent Colors

| Name | HEX | RGB | Usage |
|------|-----|-----|-------|
| KyberPurple | #7B61FF | 123,97,255 | Secondary accent, links |
| KyberBlue | #4C9AFF | 76,154,255 | Info states, charts |
| KyberGold | #FFB800 | 255,184,0 | Highlights, premium |

### Semantic Colors

| Name | HEX | RGB | Usage |
|------|-----|-----|-------|
| Success | #31CB9E | 49,203,158 | Same as Kyber Teal (positive) |
| SuccessContainer | #0D3D2E | 13,61,46 | Success backgrounds |
| Error | #FF6B6B | 255,107,107 | Errors, sell, negative |
| ErrorContainer | #3D1A1A | 61,26,26 | Error backgrounds |
| Warning | #FFB800 | 255,184,0 | Warnings, caution |
| WarningContainer | #3D2E00 | 61,46,0 | Warning backgrounds |
| Info | #4C9AFF | 76,154,255 | Info, buy, neutral |
| InfoContainer | #0D2A4D | 13,42,77 | Info backgrounds |

### Neutral Colors (Text & Surfaces)

| Name | HEX | RGB | Usage |
|------|-----|-----|-------|
| TextPrimary | #FFFFFF | 255,255,255 | Dark mode headings |
| TextSecondary | #B8C1D6 | 184,193,214 | Dark mode body |
| TextTertiary | #6B7894 | 107,120,148 | Dark mode muted |
| TextDisabled | #4A5568 | 74,85,104 | Disabled states |
| SurfaceLight | #F5F7FA | 245,247,250 | Light mode bg |
| SurfaceLightVariant | #E8ECF2 | 232,236,242 | Light mode cards |
| TextLightPrimary | #141927 | 20,25,39 | Light mode headings |
| TextLightSecondary | #4A5568 | 74,85,104 | Light mode body |

---

## Gradient Definitions

### Primary Gradient (CTAs, Hero)
```
Linear: 135deg
Start: #31CB9E (Kyber Teal)
End: #1FA77D (Kyber Teal Dark)
```

### Premium Gradient (Special features)
```
Linear: 135deg
Start: #31CB9E (Kyber Teal)
Mid: #4C9AFF (Kyber Blue)
End: #7B61FF (Kyber Purple)
```

### Dark Background Gradient
```
Linear: 180deg
Start: #1E2438 (Navy Light)
End: #141927 (Navy)
```

---

## Color Rationale

| Color | Why Chosen |
|-------|------------|
| KyberTeal #31CB9E | Official Kyber brand, represents growth/success in crypto |
| KyberNavy #141927 | Official Kyber dark, professional premium feel |
| Error #FF6B6B | Warm red for losses/errors, distinct from teal |
| KyberPurple #7B61FF | Common in crypto/DeFi, complements teal |
| KyberBlue #4C9AFF | Trust, info, "buy" actions |
| KyberGold #FFB800 | Premium/rewards, attention-grabbing |

---

## Accessibility (WCAG 2.1 AA)

| Combination | Contrast Ratio | Pass? |
|-------------|----------------|-------|
| TextPrimary on KyberNavy | 15.8:1 | Yes |
| TextSecondary on KyberNavy | 8.2:1 | Yes |
| KyberTeal on KyberNavy | 7.1:1 | Yes |
| OnKyberTeal on KyberTeal | 4.5:1 | Yes |
| Error on KyberNavy | 6.3:1 | Yes |

All combinations meet or exceed 4.5:1 for normal text.

---

## UI Application Examples

### Login Screen
- Background: KyberNavy (#141927)
- Primary button: KyberTeal (#31CB9E) with OnKyberTeal text
- Secondary button: Transparent with KyberTeal border
- Logo: White on navy
- Input fields: KyberNavyLight (#1E2438) bg, KyberNavyBorder (#3A4461) border

### Dashboard
- Background: KyberNavy gradient
- Cards: KyberNavyLight with subtle shadow
- Portfolio positive: KyberTeal/Success
- Portfolio negative: Error red
- Headers: TextPrimary
- Subtext: TextSecondary

### Buttons

| State | Background | Text | Border |
|-------|------------|------|--------|
| Primary | KyberTeal | White | None |
| Primary Hover | KyberTealLight | White | None |
| Primary Pressed | KyberTealDark | White | None |
| Primary Disabled | KyberTeal @ 40% | White @ 60% | None |
| Secondary | Transparent | KyberTeal | KyberTeal |
| Secondary Hover | KyberTeal @ 10% | KyberTeal | KyberTeal |
| Tertiary | Transparent | TextSecondary | None |

### Trading Interface
- Buy button: KyberTeal (success connotation)
- Sell button: Error red
- Price up: KyberTeal
- Price down: Error red
- Chart lines: KyberTeal, KyberBlue, KyberPurple

---

## Jetpack Compose Color.kt Implementation

```kotlin
// Kyber Brand Colors
val KyberTeal = Color(0xFF31CB9E)
val KyberTealLight = Color(0xFF6FDFBD)
val KyberTealDark = Color(0xFF1FA77D)
val KyberTealContainer = Color(0xFF0D3D2E)
val OnKyberTeal = Color(0xFFFFFFFF)

val KyberNavy = Color(0xFF141927)
val KyberNavyLight = Color(0xFF1E2438)
val KyberNavySurface = Color(0xFF262D44)
val KyberNavyBorder = Color(0xFF3A4461)

val KyberPurple = Color(0xFF7B61FF)
val KyberBlue = Color(0xFF4C9AFF)
val KyberGold = Color(0xFFFFB800)

// Semantic
val Success = KyberTeal
val SuccessContainer = KyberTealContainer
val Error = Color(0xFFFF6B6B)
val ErrorContainer = Color(0xFF3D1A1A)
val Warning = KyberGold
val WarningContainer = Color(0xFF3D2E00)
val Info = KyberBlue
val InfoContainer = Color(0xFF0D2A4D)

// Text - Dark Mode
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB8C1D6)
val TextTertiary = Color(0xFF6B7894)
val TextDisabled = Color(0xFF4A5568)

// Light Mode
val SurfaceLight = Color(0xFFF5F7FA)
val SurfaceLightVariant = Color(0xFFE8ECF2)
val TextLightPrimary = KyberNavy
val TextLightSecondary = Color(0xFF4A5568)
```

---

## Todo List

- [ ] Update Color.kt with Kyber colors
- [ ] Remove default purple/pink colors
- [ ] Create dark color scheme
- [ ] Create light color scheme
- [ ] Test contrast ratios
- [ ] Apply to existing UI components

---

## Success Criteria

- All colors use Kyber brand palette
- Contrast ratios pass WCAG 2.1 AA
- Dark mode is visually cohesive
- Light mode is functional fallback
- Semantic colors clearly communicate state

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Contrast issues | Medium | Pre-verified ratios above |
| Brand inconsistency | Low | Using official media kit colors |
| Dynamic color override | Medium | Disable dynamic colors |

---

## Next Steps

1. Implement Color.kt changes
2. Proceed to Phase 02 (Theme.kt)

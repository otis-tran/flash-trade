# Phase 01: App Icon Design Implementation

**Phase:** 01 of 02
**Focus:** Adaptive icon foreground/background layers
**Duration:** 30-45 min

## Objective

Replace default Android robot icon with Kyber-branded lightning bolt using KyberTeal gradient on KyberNavy background. Ensure compliance with adaptive icon specifications and visual quality at all display sizes.

## Design Rationale

### Lightning Bolt Symbol
- **Meaning:** Speed, power, instantaneous action (core to "Flash Trade" concept)
- **Style:** Geometric, angular for energy, slight curve for polish
- **Complexity:** 3-segment bolt (simple enough for 48dp clarity)
- **Orientation:** Diagonal (~15-20° from vertical) for dynamism

### Color Treatment
- **Foreground gradient:** KyberTeal (#31CB9E) → KyberTealLight (#6FDFBD)
  - Start at top-left (lighter)
  - End at bottom-right (darker)
  - Adds depth without complexity
- **Background:** Solid KyberNavy (#141927)
  - No grid overlay (clean, modern)
  - OLED-friendly (true black on AMOLED displays)

### Safe Zone Strategy
- Bolt positioned within 72dp safe zone
- 18dp padding from safe zone edges
- Actual bolt dimensions: ~36x48dp (centered in 72dp zone)
- Ensures visibility on all launcher mask shapes

## Technical Specifications

### Canvas Layout
```
108dp × 108dp total canvas
  ├─ 0-18dp: Outer margin (masked by some launchers)
  ├─ 18-90dp: 72dp safe zone
  │   └─ 30-66dp: Lightning bolt (36x48dp centered)
  └─ 90-108dp: Outer margin
```

### Lightning Bolt Path Design
Simplified 3-segment bolt:
1. **Top segment:** Wide diagonal from top-left to center
2. **Middle segment:** Narrow shaft (creates negative space)
3. **Bottom segment:** Wide diagonal from center to bottom-right

Path coordinates (conceptual, in 108dp space):
- Start point: (40, 28) - top of safe zone
- Zigzag vertices: (54, 42), (50, 54), (68, 80)
- End point: (54, 80) - bottom of safe zone

Actual implementation uses Android vector pathData syntax.

## Implementation Steps

### Step 1: Update Foreground Layer
**File:** `app/src/main/res/drawable/ic_launcher_foreground.xml`

**Actions:**
1. Replace existing Android robot pathData
2. Create lightning bolt vector path
3. Apply linear gradient (KyberTeal → KyberTealLight)
4. Set gradient angle: 135° (top-left to bottom-right)
5. Ensure viewportWidth/Height remain 108x108

**Vector Structure:**
```xml
<vector xmlns:android="..."
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <path android:pathData="[lightning bolt path]">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="40" android:startY="28"
                android:endX="68" android:endY="80"
                android:type="linear">
                <item android:color="#FF6FDFBD" android:offset="0.0" />
                <item android:color="#FF31CB9E" android:offset="1.0" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
```

**Path Design Notes:**
- Use moveTo (M), lineTo (L), close (Z) commands
- Keep path simple (8-12 vertices max)
- Sharp angles for energy, slight curves (cubic/quadratic Bezier) for polish
- Test rendering at 48dp before finalizing

### Step 2: Update Background Layer
**File:** `app/src/main/res/drawable/ic_launcher_background.xml`

**Actions:**
1. Remove existing grid overlay paths (lines 11-170)
2. Replace #3DDC84 with #141927 (KyberNavy)
3. Keep single rectangle path (0,0 to 108,108)
4. Simplify file to ~10 lines

**New Structure:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#141927"
        android:pathData="M0,0h108v108h-108z" />
</vector>
```

### Step 3: Verify Adaptive Icon Resources
**Files:**
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

**Actions:**
1. Confirm references to `@drawable/ic_launcher_foreground`
2. Confirm references to `@drawable/ic_launcher_background`
3. No changes needed if references correct

**Expected structure:**
```xml
<adaptive-icon xmlns:android="...">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

## Lightning Bolt PathData Specification

### Approach 1: Geometric (Recommended)
Angular, precise, minimal vertices:
```
M44,30 L52,30 L48,54 L56,54 L50,78 L46,78 L50,60 L42,60 Z
```
- 8 vertices
- Symmetric left/right
- Clear zigzag silhouette

### Approach 2: Organic
Slightly curved edges for premium feel:
```
M44,28 Q46,28 48,30 L52,30 Q54,32 52,34 L48,52
L56,52 Q58,54 56,56 L50,78 Q48,80 46,78 L48,62
L40,62 Q38,60 40,58 Z
```
- 12 vertices with cubic curves
- Smoother appearance
- More complex (test at 48dp)

### Final Choice
Use Approach 1 initially. If too harsh, refine with subtle curves (1-2dp radius on corners).

## Color Values Reference

From `Color.kt`:
- KyberTeal: `#FF31CB9E` (ARGB format for XML)
- KyberTealLight: `#FF6FDFBD`
- KyberNavy: `#FF141927`

Note: Android XML uses ARGB format (#AARRGGBB), not RGB. Add `FF` prefix for full opacity.

## Testing Checklist

### Visual Testing
- [ ] Render icon at 48dp (typical app drawer size)
- [ ] Render icon at 72dp (widget/settings size)
- [ ] Render icon at 108dp (full canvas preview)
- [ ] Check gradient smoothness (no banding)
- [ ] Verify bolt centered in safe zone

### Mask Shape Testing
- [ ] Circle mask (Google Pixel launcher)
- [ ] Rounded square mask (Samsung One UI)
- [ ] Square mask (older launchers)
- [ ] Squircle mask (OnePlus, MIUI)

### Launcher Testing
- [ ] Test on Pixel emulator (stock Android)
- [ ] Test on Samsung device/emulator (One UI)
- [ ] Test on custom launcher (Nova Launcher recommended)

### Contrast Testing
- [ ] Light background launcher (white/light gray)
- [ ] Dark background launcher (black/dark gray)
- [ ] Adaptive theme launchers (system light/dark)

## Success Criteria

1. **Recognizability:** Lightning bolt instantly identifiable at 48dp
2. **Brand consistency:** Colors match Kyber palette exactly
3. **Technical compliance:** No clipping on any mask shape
4. **Professional quality:** Competitive with top crypto apps (Uniswap, MetaMask, Phantom)

## Potential Issues & Solutions

| Issue | Solution |
|-------|----------|
| Bolt too thin at 48dp | Increase stroke width to 4-6dp |
| Gradient banding | Use more gradient stops (0.0, 0.5, 1.0) |
| Clipping on circle mask | Reduce bolt size by 10%, increase padding |
| Poor contrast on light backgrounds | Add subtle shadow/outline (optional, test first) |

## Deliverables

1. **ic_launcher_foreground.xml** - Lightning bolt with KyberTeal gradient
2. **ic_launcher_background.xml** - Solid KyberNavy background
3. **Test screenshots** - Icon on 3+ launcher types (for verification)

## Next Phase

After icon implementation complete, proceed to Phase 02: Splash Screen Design.

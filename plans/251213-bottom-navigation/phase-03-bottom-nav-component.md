# Phase 03: Bottom Navigation Component

**Priority:** High | **Status:** ✅ Complete | **Effort:** 2.5 hours

## Context

- [Research: UI/UX Design](./research/researcher-02-ui-ux-design.md)
- [Research: Material3 NavigationBar](./research/researcher-02-ui-ux-design.md#3-material-design-3-guidelines)
- [Kyber Brand Colors](../../../app/src/main/java/com/otistran/flash_trade/ui/theme/Color.kt)
- [Phase 02: AppState](./phase-02-root-scaffold.md)

## Overview

Create BottomNavBar composable with Material3 NavigationBar component. Apply Kyber brand colors (Teal #31CB9E primary, Navy #141927 background). Implement smooth 300-350ms transitions, icon state changes (outlined ↔ filled), and accessibility features.

## Key Insights from Research

- Material3 NavigationBar height: 80dp (includes safe area padding)
- Icon size: 24x24dp (can extend to 28dp for premium feel)
- Animation duration: 300-350ms optimal for trading interfaces
- Active indicator: 4dp height bar (Material 3 style)
- Color contrast ratio: 4.5:1 minimum for WCAG AA compliance
- Always show labels for trading apps (familiarity critical)
- Min touch target: 48x48dp for accessibility

## Requirements

### Visual Design
- **Dark theme (primary):** Navy background, Teal accent for selected items
- **Light theme:** Light background, Navy text/icons
- **Unselected state:** Gray (#808080 dark, #999999 light) with 0.6-0.7 opacity
- **Selected state:** KyberTeal with full opacity
- **Labels:** Always visible, 12sp medium (unselected) → 12sp bold (selected)
- **Icons:** Outlined (unselected) → Filled (selected)

### Animation & Transitions
- Icon/label color fade: 250-300ms
- Selection indicator: Spring animation (200ms)
- Ripple effect on touch: Material default
- No bounce or excessive scaling

### Accessibility
- Semantic labels for screen readers
- Color contrast meets WCAG AA (4.5:1)
- Touch targets ≥48x48dp
- Tab order left-to-right

## Architecture Decisions

1. **Component:** Use Material3 NavigationBar (not custom)
2. **Theming:** Extend MaterialTheme.colorScheme for bottom nav colors
3. **Icons:** Material Icons outlined/filled variants
4. **State:** Stateless component, receives current selection as parameter
5. **Styling:** Extract colors to theme, not hardcoded

## Related Code Files

- `app/src/main/java/com/otistran/flash_trade/presentation/navigation/BottomNavBar.kt` (create)
- `app/src/main/java/com/otistran/flash_trade/ui/theme/Color.kt` (reference)
- `app/src/main/java/com/otistran/flash_trade/ui/theme/Theme.kt` (may need to extend)
- `app/src/main/java/com/otistran/flash_trade/presentation/navigation/TopLevelDestination.kt` (reference)

## Implementation Steps

### Step 1: Create BottomNavBar.kt
Stateless composable with Material3 NavigationBar:
```kotlin
package com.otistran.flash_trade.presentation.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.otistran.flash_trade.ui.theme.*

@Composable
fun BottomNavBar(
    destinations: List<TopLevelDestination>,
    currentDestination: TopLevelDestination?,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        destinations.forEach { destination ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) {
                            destination.iconFilled
                        } else {
                            destination.iconOutlined
                        },
                        contentDescription = destination.label
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = if (isSelected) {
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        } else {
                            MaterialTheme.typography.labelMedium
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
```

### Step 2: Update Theme.kt for Bottom Nav Colors
Extend color scheme to use Kyber brand colors:
```kotlin
// In Theme.kt, update color schemes:

private val DarkColorScheme = darkColorScheme(
    primary = KyberTeal,              // Selected icon/label
    onPrimary = OnKyberTeal,
    primaryContainer = KyberTealContainer,  // Active indicator background
    surface = KyberNavy,              // Bottom nav background
    onSurface = TextPrimary,
    onSurfaceVariant = TextTertiary   // Unselected icon/label
)

private val LightColorScheme = lightColorScheme(
    primary = KyberTealDark,          // Selected icon/label (darker for contrast)
    onPrimary = Color.White,
    primaryContainer = KyberTealLight,
    surface = SurfaceLight,           // Bottom nav background
    onSurface = TextLightPrimary,
    onSurfaceVariant = TextLightSecondary
)
```

### Step 3: Test Color Contrast
Verify WCAG AA compliance (4.5:1 ratio):
- Dark theme: KyberTeal (#31CB9E) on KyberNavy (#141927)
- Light theme: KyberTealDark (#1FA77D) on SurfaceLight (#F5F7FA)
- Unselected: TextTertiary (#6B7894) on KyberNavy

Use online contrast checker: https://webaim.org/resources/contrastchecker/

### Step 4: Add Icon Resources (if needed)
Verify Material Icons available in project:
- Trading: Icons.Outlined.TrendingUp / Icons.Filled.TrendingUp
- Portfolio: Icons.Outlined.AccountBalanceWallet / Icons.Filled.AccountBalanceWallet
- Settings: Icons.Outlined.Settings / Icons.Filled.Settings

If missing, add Material Icons Extended dependency to `build.gradle.kts`:
```kotlin
dependencies {
    implementation("androidx.compose.material:material-icons-extended")
}
```

### Step 5: Add Animation Polish (Optional)
For smoother transitions, wrap icon in AnimatedContent:
```kotlin
AnimatedContent(
    targetState = isSelected,
    label = "icon_animation"
) { selected ->
    Icon(
        imageVector = if (selected) {
            destination.iconFilled
        } else {
            destination.iconOutlined
        },
        contentDescription = destination.label
    )
}
```

### Step 6: Preview Component
Add @Preview for rapid iteration:
```kotlin
@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun BottomNavBarPreview() {
    FlashTradeTheme {
        BottomNavBar(
            destinations = TopLevelDestination.entries,
            currentDestination = TopLevelDestination.TRADING,
            onNavigateToDestination = {}
        )
    }
}
```

## Success Criteria

- [ ] BottomNavBar.kt created with Material3 NavigationBar
- [ ] Kyber brand colors applied (Teal primary, Navy background)
- [ ] Icon transitions work (outlined ↔ filled)
- [ ] Label font weight changes on selection (medium → bold)
- [ ] Labels always visible (not icon-only)
- [ ] Color contrast meets WCAG AA (≥4.5:1)
- [ ] Touch targets ≥48x48dp
- [ ] Smooth transitions (~300ms)
- [ ] No layout jank or flickering
- [ ] Preview renders correctly in Android Studio
- [ ] Supports dark and light themes
- [ ] File size <200 lines

## Risk Assessment

**Low Risk:**
- Material3 NavigationBar is stable, well-tested component
- Kyber brand colors already defined in Color.kt
- Stateless component, no complex state management

**Mitigation:**
- Test on multiple device sizes (5" to 6.7" screens)
- Verify color contrast with automated tools
- Test with TalkBack for accessibility
- Ensure ripple effects work on all Android versions

## Testing Checklist

- Visual: Bottom bar renders with correct colors
- Visual: Icons change from outlined to filled on selection
- Visual: Labels change from medium to bold on selection
- Visual: Active indicator appears under selected item
- Interaction: Tap each tab, selection changes immediately
- Interaction: Ripple effect shows on tap
- Accessibility: TalkBack announces tab labels correctly
- Accessibility: Color contrast ≥4.5:1 (use contrast checker)
- Accessibility: Touch targets ≥48dp (use layout inspector)
- Theme: Dark theme uses Navy + Teal
- Theme: Light theme uses Light surface + Teal dark
- Performance: No jank during tab switches (use GPU profiling)

## Follow-up Phase

Phase 04: Navigation Implementation - Wire bottom bar to nested navigation graphs

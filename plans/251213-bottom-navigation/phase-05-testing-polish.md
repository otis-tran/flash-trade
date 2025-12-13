# Phase 05: Testing & Polish

**Priority:** Medium | **Status:** Pending | **Effort:** 2 hours

## Context

- [Code Standards: Testing](../../../docs/code-standards.md#testing-standards)
- [Research: Performance Best Practices](./research/researcher-01-navigation-compose.md#4-performance-best-practices)
- [Research: Accessibility](./research/researcher-02-ui-ux-design.md#3-material-design-3-guidelines)
- All previous phases: 01-04

## Overview

Comprehensive testing of bottom navigation implementation, performance optimization, accessibility verification, and final polish. Ensure production-ready quality with smooth animations, no memory leaks, and excellent user experience.

## Key Insights from Research

- Navigation state restoration critical for good UX (test with process death)
- Performance target: <16ms frame times during transitions
- Accessibility: WCAG AA compliance (4.5:1 contrast, semantic labels)
- Memory efficiency: Bottom nav caches screens, monitor for leaks
- Animation smoothness: 300-350ms transitions feel responsive

## Requirements

### Testing Coverage
- Unit tests for AppState route detection
- UI tests for navigation flows
- Accessibility tests (TalkBack, contrast)
- Performance tests (frame timing, memory)
- Edge case tests (rotation, process death, low memory)

### Performance Targets
- Cold start: <800ms (unchanged from baseline)
- Tab switch latency: <200ms
- Frame time during transitions: <16ms (60fps)
- Memory usage: No leaks, reasonable overhead for cached screens

### Accessibility Requirements
- Color contrast ≥4.5:1 (WCAG AA)
- TalkBack announces tabs correctly
- Touch targets ≥48x48dp
- Tab order logical (left-to-right)
- Focus indicators visible

### Polish Items
- Animation timing fine-tuning
- Ripple effect polish
- Safe area padding (notch devices)
- Dark/light theme verification
- Icon alignment pixel-perfect

## Architecture Decisions

1. **Testing Strategy:** Mix of unit, UI, and manual tests
2. **Performance:** Use Android Studio Profiler + GPU rendering
3. **Accessibility:** Automated + manual testing with TalkBack
4. **Polish:** Visual QA on multiple device sizes
5. **Documentation:** Update codebase-summary.md with implementation

## Related Code Files

- All files created/modified in phases 01-04
- `app/src/test/` (unit tests)
- `app/src/androidTest/` (UI tests)
- `docs/codebase-summary.md` (update)

## Implementation Steps

### Step 1: Unit Tests for AppState
Test route detection logic:
```kotlin
// AppStateTest.kt
package com.otistran.flash_trade.presentation.navigation

import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AppStateTest {

    private lateinit var navController: TestNavHostController
    private lateinit var appState: AppState

    @Before
    fun setup() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        appState = AppState(navController)
    }

    @Test
    fun shouldShowBottomBar_whenOnTradingScreen() {
        navController.navigate(TradingGraph)
        assertTrue(appState.shouldShowBottomBar)
    }

    @Test
    fun shouldHideBottomBar_whenOnWelcomeScreen() {
        navController.navigate(Welcome)
        assertFalse(appState.shouldShowBottomBar)
    }

    @Test
    fun shouldHideBottomBar_whenOnTradeDetails() {
        navController.navigate(TradeDetails(tradeId = "123"))
        assertFalse(appState.shouldShowBottomBar)
    }

    @Test
    fun currentTopLevelDestination_shouldBeTrading_whenOnTradingGraph() {
        navController.navigate(TradingGraph)
        assertEquals(TopLevelDestination.TRADING, appState.currentTopLevelDestination)
    }
}
```

### Step 2: UI Tests for Navigation Flows
Test tab switching and back stack preservation:
```kotlin
// BottomNavigationTest.kt
package com.otistran.flash_trade

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class BottomNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomBar_isVisible_onTradingScreen() {
        // Navigate to Trading (after login)
        // Verify bottom bar displayed
        composeTestRule.onNodeWithText("Trading").assertIsDisplayed()
        composeTestRule.onNodeWithText("Portfolio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun bottomBar_isHidden_onLoginScreen() {
        // Verify bottom bar not displayed on Login
        composeTestRule.onNodeWithText("Trading").assertDoesNotExist()
    }

    @Test
    fun tabSwitch_preservesState() {
        // Navigate Trading → Portfolio → Trading
        composeTestRule.onNodeWithText("Portfolio").performClick()
        composeTestRule.onNodeWithText("Trading").performClick()
        // Verify Trading screen state preserved (implementation-specific)
    }
}
```

### Step 3: Accessibility Testing
Verify TalkBack, contrast, and touch targets:
```kotlin
// Automated accessibility checks
@Test
fun bottomNavBar_meetsAccessibilityStandards() {
    composeTestRule.onNodeWithText("Trading")
        .assertHasClickAction()
        .assertIsSelectable()

    // Touch target size (48x48dp minimum)
    composeTestRule.onNodeWithText("Trading")
        .assertWidthIsAtLeast(48.dp)
        .assertHeightIsAtLeast(48.dp)
}

// Manual TalkBack testing:
// 1. Enable TalkBack: Settings → Accessibility → TalkBack
// 2. Navigate app with swipe gestures
// 3. Verify tabs announced correctly: "Trading, tab, 1 of 3"
// 4. Verify selection state announced: "Trading, tab, selected"
```

### Step 4: Performance Profiling
Use Android Studio Profiler:
```
1. CPU Profiler:
   - Record tab switch: Trading → Portfolio
   - Verify no excessive CPU usage during transition
   - Target: <200ms total time

2. Memory Profiler:
   - Navigate all tabs multiple times
   - Force garbage collection
   - Verify no memory leaks (heap should stabilize)
   - Check retained size of NavBackStackEntry

3. GPU Rendering Profiler:
   - Enable: Developer Options → Profile GPU Rendering → On screen as bars
   - Navigate tabs
   - Verify all frames <16ms (green line)
   - No red bars during transitions
```

### Step 5: Visual QA on Multiple Devices
Test on different screen sizes and OS versions:
```
Device Testing Matrix:
- Small phone (5"): Pixel 4a, Galaxy A52
- Medium phone (6.1"): Pixel 6, Galaxy S21
- Large phone (6.7"): Pixel 7 Pro, Galaxy S22 Ultra
- Tablet (if applicable): Pixel Tablet

OS Versions:
- Android 9 (API 28) - Minimum supported
- Android 12 (API 31) - Common
- Android 14 (API 34) - Latest stable
- Android 15 (API 36) - Target

Verify:
- Bottom bar not clipped on notch devices
- Safe area padding correct
- Icons aligned properly
- Text readable on all sizes
```

### Step 6: Animation Fine-Tuning
Polish transition timing:
```kotlin
// If animations feel sluggish, adjust in BottomNavBar.kt:

// Option 1: Add custom transition spec
NavigationBarItem(
    // ... other params
    interactionSource = remember { MutableInteractionSource() }
        .also { interactionSource ->
            // Custom ripple timing if needed
        }
)

// Option 2: Verify Material3 theme animations
// In Theme.kt, ensure default motion tokens not overridden
```

### Step 7: Dark/Light Theme Verification
Test both themes thoroughly:
```
Dark Theme Checklist:
- [ ] Bottom bar background: KyberNavy (#141927)
- [ ] Selected icon/label: KyberTeal (#31CB9E)
- [ ] Unselected icon/label: TextTertiary (#6B7894)
- [ ] Contrast ratio ≥4.5:1
- [ ] No color bleed or incorrect colors

Light Theme Checklist:
- [ ] Bottom bar background: SurfaceLight (#F5F7FA)
- [ ] Selected icon/label: KyberTealDark (#1FA77D)
- [ ] Unselected icon/label: TextLightSecondary
- [ ] Contrast ratio ≥4.5:1
- [ ] Readable in bright sunlight
```

### Step 8: Edge Case Testing
Test unusual scenarios:
```
Edge Cases:
1. Rapid tab switching (stress test):
   - Tap tabs quickly 20+ times
   - Verify no crashes or jank

2. Process death simulation:
   - Enable: Developer Options → Don't keep activities
   - Navigate to Portfolio
   - Press Home, then return to app
   - Verify Portfolio still selected

3. Low memory scenario:
   - Enable: Developer Options → Background process limit → No background processes
   - Navigate all tabs
   - Verify graceful degradation (no crashes)

4. Orientation changes:
   - Rotate device on each screen
   - Verify state preserved
   - Verify bottom bar re-renders correctly

5. System back gesture:
   - Use gesture navigation (Android 10+)
   - Verify back gesture works from screen edges
   - No conflicts with app navigation
```

### Step 9: Update Documentation
Document implementation in codebase-summary.md:
```markdown
## Bottom Navigation Implementation

**Status:** ✅ Complete

**Architecture:**
- Single NavHost with nested navigation graphs per tab
- AppState holder for centralized nav logic
- Root Scaffold at MainActivity level with conditional bottom bar

**Files:**
- `presentation/navigation/AppState.kt` - Navigation state management
- `presentation/navigation/BottomNavBar.kt` - Bottom bar component
- `presentation/navigation/TopLevelDestination.kt` - Tab configuration
- `presentation/navigation/NavGraph.kt` - Nested graph implementation
- `presentation/navigation/Screen.kt` - Type-safe routes (@Serializable)

**Features:**
- Independent back stacks per tab
- State preservation on tab switch
- Conditional visibility (hidden on auth/detail screens)
- Kyber brand styling (Teal + Navy)
- Material3 NavigationBar component
- Smooth 300ms transitions
- WCAG AA accessibility compliant
```

### Step 10: Final Checklist
Complete pre-merge checklist:
```
Code Quality:
- [ ] All files <200 lines
- [ ] No TODO comments without tickets
- [ ] No hardcoded strings (use resources if applicable)
- [ ] No console.log or debug prints
- [ ] Follow Kotlin code style
- [ ] All imports organized

Testing:
- [ ] Unit tests pass (./gradlew test)
- [ ] UI tests pass (./gradlew connectedAndroidTest)
- [ ] Manual testing complete
- [ ] No regressions in existing features

Performance:
- [ ] No memory leaks (Profiler verification)
- [ ] Frame times <16ms during transitions
- [ ] Cold start time unchanged from baseline

Accessibility:
- [ ] TalkBack announces correctly
- [ ] Color contrast ≥4.5:1
- [ ] Touch targets ≥48dp
- [ ] Keyboard navigation works (if applicable)

Visual:
- [ ] Dark theme correct
- [ ] Light theme correct
- [ ] Icons aligned properly
- [ ] No visual glitches or jank
- [ ] Tested on 3+ device sizes

Documentation:
- [ ] codebase-summary.md updated
- [ ] Code comments added where needed
- [ ] README updated if necessary
```

## Success Criteria

- [ ] All unit tests passing
- [ ] All UI tests passing
- [ ] Manual testing checklist complete
- [ ] Performance targets met (<16ms frames, no leaks)
- [ ] Accessibility standards met (WCAG AA)
- [ ] Dark/light themes verified
- [ ] Edge cases handled gracefully
- [ ] Documentation updated
- [ ] Code review ready (clean, commented, tested)
- [ ] No regressions in existing features

## Risk Assessment

**Low Risk:**
- Testing and polish phase, no structural changes
- Issues caught here prevent production bugs

**Mitigation:**
- Comprehensive testing checklist
- Automated tests catch regressions
- Visual QA on multiple devices
- Performance profiling before merge

## Testing Checklist

### Automated Tests
- [ ] AppStateTest: All tests pass
- [ ] BottomNavigationTest: All tests pass
- [ ] No test flakiness (run 3x to verify)

### Manual Tests
- [ ] All navigation flows work correctly
- [ ] State preservation verified
- [ ] Back press behavior correct
- [ ] No visual glitches
- [ ] Smooth animations

### Performance Tests
- [ ] CPU Profiler: Tab switch <200ms
- [ ] Memory Profiler: No leaks detected
- [ ] GPU Profiler: All frames <16ms

### Accessibility Tests
- [ ] TalkBack announces tabs correctly
- [ ] Color contrast verified (online checker)
- [ ] Touch targets ≥48dp (layout inspector)

### Device Matrix
- [ ] Tested on small phone (5")
- [ ] Tested on medium phone (6.1")
- [ ] Tested on large phone (6.7")
- [ ] Android 9 (API 28)
- [ ] Android 14 (API 34)

### Edge Cases
- [ ] Rapid tab switching: No crashes
- [ ] Process death: State restored
- [ ] Rotation: State preserved
- [ ] Low memory: Graceful handling

## Completion Criteria

All checkboxes above must be checked before merging to main branch. Any failing items must be documented as known issues with mitigation plan or fixed before merge.

## Unresolved Questions

None at this phase. All issues should be resolved or documented during testing.

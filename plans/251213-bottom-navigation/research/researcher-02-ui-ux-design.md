# Bottom Navigation UI/UX Design Research
**Date:** 2025-12-13 | **Target:** Premium Web3 Trading App

---

## 1. Visual Design Trends (2024-2025)

### Premium Web3 Trading App Aesthetics
- **Dark-first design** with accent neon/gradient elements (crypto aesthetic)
- Minimal, tech-forward interfaces emphasizing speed and clarity
- Gradient accents on active states (vibrant blues, purples, teals)
- High contrast for accessibility while maintaining premium feel
- Rounded corners (8-12dp) on interactive elements vs sharp lines on containers

### Icon Styles
- **Outlined icons** for unselected states (24x24dp, 2dp stroke)
- **Filled icons** for selected states with gradient overlays
- Consistent icon family (Material Symbols or custom crypto-themed)
- Smooth 300-350ms transitions between states
- Subtle scaling (105-110%) on selection without bounce

### Color Treatment
- **Unselected state:** Neutral gray (#666-#999) on dark bg / (#999-#CCC) on light
- **Selected state:** Brand gradient (blue→purple) or solid accent (#6C63FF)
- Badge animations for notifications (scale + fade 200ms)
- Ripple/splash effect on touch (50-100ms subtle scale)

### Label Visibility Pattern
- **Always visible** (recommended for trading apps) - familiarity critical
- Typography: 12sp medium weight (unselected), 12sp bold (selected)
- 4dp spacing below icon, centered alignment
- Animated fade between states (200ms)

---

## 2. Reference Apps Design Patterns

### Premium Trading App Common Features
1. **Bottom nav tabs (3-5 items max)**
   - Home/Dashboard → Portfolio view
   - Markets/Explore → Trending/watchlist
   - Trade → Quick execution + favorites
   - Wallet → Balance + assets
   - Settings/Profile → User controls

2. **Visual Hierarchy**
   - Dashboard (default/active) gets highest contrast
   - Trade tab often highlighted with accent gradient
   - Settings deprioritized (gray state)
   - Portfolio shows live balance badges

3. **Premium Indicators**
   - Animated balance ticker on Home tab
   - Red/green badges on Portfolio (P&L)
   - Notification dots on Settings (unread items)
   - Active indicator bar under selected tab (2-4dp height)

### Kyber Network App Inspiration
- Fast tap-to-trade accessibility
- Minimal visual noise, focus on data
- Quick state feedback (ripple effects ~100ms)
- Gradient accent on primary actions

---

## 3. Material Design 3 Guidelines

### NavigationBar Component Specs
```
Height:                 80dp (includes safe area padding)
Icon size:             24x24dp (can extend to 28dp for premium feel)
Icon top padding:      12dp from top
Label text:            12sp medium → 12sp bold on selection
Label bottom padding:  16dp from bottom
Min item width:        80dp
Max item width:        168dp
Active indicator:      4dp height bar (Material 3 style)
```

### Recommended Dimensions (Compose)
- **Total height:** 80-96dp (account for system nav)
- **Icon offset:** 8-12dp above label
- **Label margins:** 4dp top, 8dp bottom
- **Horizontal padding:** 8dp per item (auto-distributed)
- **Elevation:** 8dp (surface = background color with shadow)

### Accessibility Requirements
- Min touch target: 48x48dp (meets WCAG AA)
- Color contrast ratio: 4.5:1 for text
- Label always visible (avoid icon-only for primary nav)
- Semantic labels for screen readers
- Tab order: left-to-right, predictable

---

## 4. Animation & Micro-interactions

### Tab Switch Animations
- **Duration:** 300-350ms (optimal for trading interfaces)
- **Easing:** Material Easing (Fast 200ms + Slow 150ms curve)
- **Icon transition:** Scale 0.85 → 1.0 on activation
- **Color shift:** Fade/cross-dissolve 250ms between states
- **Label animation:** Fade + optional slight translate (±2dp)

### Icon State Transitions
```
State: Idle
  - Color: #808080
  - Scale: 1.0
  - Opacity: 0.7

State: Hover (desktop)
  - Color: #999999
  - Scale: 1.05
  - Opacity: 0.85

State: Active
  - Color: Brand accent (#6C63FF)
  - Scale: 1.0 (no hover state scaling)
  - Opacity: 1.0
  - Indicator: 4dp bar appears below
```

### Badge Animations
- **New notification:** Scale 0 → 1.0 (200ms, Spring easing)
- **Pulse effect:** Optional subtle scale loop (1.0 → 1.1 → 1.0) at 2s interval
- **Dismiss:** Scale & fade 150ms
- **Position:** Top-right of icon (+6dp offset)
- **Color:** Red/warning accent (#FF4444) with white text

---

## 5. Icon Suggestions for Flash Trade

### Trading App Tab Icons (Outlined → Filled)

#### Tab 1: Dashboard/Home
- **Outlined:** Grid icon (3x3 squares) or Dashboard symbol
- **Filled:** Solid grid with accent highlight
- **Rationale:** Overview, data summary, portfolio snapshot

#### Tab 2: Markets/Explore
- **Outlined:** Trending Up / Chart icon
- **Filled:** Trending line with gradient fill
- **Rationale:** Discovery, watchlist, market data
- **Badge use:** Green indicator for top gainers

#### Tab 3: Trade (PRIMARY)
- **Outlined:** Swap/Exchange arrows (circular)
- **Filled:** Swap with gradient background
- **Rationale:** Core action, highest visual priority
- **Default state:** Slight gradient hint (draw attention)

#### Tab 4: Wallet/Assets
- **Outlined:** Wallet/purse icon
- **Filled:** Wallet with balance indicator
- **Rationale:** Portfolio, holdings, balance
- **Badge use:** Red/green for net change

#### Tab 5: Settings/Profile
- **Outlined:** Gear/cog icon
- **Filled:** Gear with background
- **Rationale:** Configuration, account, preferences

**Icon source recommendations:**
- Google Material Symbols (open source, clean)
- Crypto-specific: Icon8 crypto icon set
- Custom illustrator work (premium feel)

---

## 6. Color Palette Specification

### Light Theme
```
Background:        #FFFFFF
Surface:           #F5F5F5
Unselected icon:   #999999 / opacity 0.7
Selected icon:     #5B5FFF (primary accent)
Active indicator:  #5B5FFF
Badge:            #FF4444 (warning red)
```

### Dark Theme (Recommended for trading)
```
Background:        #121212 / #0F0F0F
Surface:           #1E1E1E
Unselected icon:   #808080 / opacity 0.6
Selected icon:     #7C7FFF (brighter accent)
Active indicator:  #7C7FFF
Badge:            #FF6666 (warning red)
Gradient accent:   #7C7FFF → #A78BFF (premium feel)
```

### State Elevation
- Surface elevation: 8dp shadow (#000 20% opacity)
- Active indicator glow: Optional subtle shadow on accent (2-4dp blur)

---

## 7. Key Recommendations

1. **Always show labels** - Critical for trading app familiarity
2. **Default dark theme** - Premium crypto aesthetic, battery efficiency
3. **Trade tab visual priority** - Slight accent/glow even in unselected state
4. **Smooth 300-350ms transitions** - Feels responsive, not sluggish
5. **Active indicator bar** - Material 3 style (4dp bar) vs Material 2 style (highlight)
6. **Badge support** - Essential for portfolio changes & notifications
7. **Haptic feedback** - 20-30ms vibration on tab change (optional, premium UX)
8. **Safe area padding** - Account for system navigation bar height (~80dp)

---

## 8. Implementation Checklist

- [ ] Select icon set (Material Symbols + custom variants)
- [ ] Define color tokens (dark/light theme)
- [ ] Create animation specs (Lottie JSON or Compose Animation)
- [ ] Accessibility review (color contrast, touch targets)
- [ ] Create Figma design components (reusable)
- [ ] Build NavigationBar Composable (Jetpack Compose)
- [ ] Implement badge system
- [ ] Test on devices 5"→6.7" screens
- [ ] Performance: <16ms frame times during transitions

---

## Unresolved Questions

1. Will Flash Trade app use 3, 4, or 5 tabs initially? (Affects item width calculation)
2. Primary brand accent color? (Assuming blue/purple, confirm)
3. Badge support needed for notifications/alerts?
4. Should Trade tab have constant accent glow in unselected state?
5. Haptic feedback preference for Android devices?
6. Custom icon design or Material Symbols sufficient?

---

**Sources:**
- [Material Design 3 Navigation Bar Specs](https://m3.material.io/components/navigation-bar/specs)
- [Jetpack Compose Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Crypto Trading UI Design Trends](https://medium.com/@extej/innovative-design-trends-in-crypto-trading-platforms-c98c593d978e)
- [Microinteractions Best Practices 2025](https://medium.com/@rosalie24/microinteractions-in-mobile-apps-2025-best-practices-c2e6ecd53569)
- [Material Design 3 Bottom Navigation](https://www.boltuix.com/2025/06/materialbottomnavigation.html)

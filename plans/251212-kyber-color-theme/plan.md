# Kyber Brand Color Theme Plan

**Date:** 2025-12-12
**Status:** Planning
**Branch:** feature/login-via-privy
**Priority:** P1 (Design Foundation)

---

## Overview

Define comprehensive color system for Flash Trade app aligned with Kyber Network brand identity. Dark mode primary, light mode secondary. Material3 compatible.

**Timeline:** 2-3 hours | **Files:** 2 modified (Color.kt, Theme.kt)

---

## Kyber Brand Colors (Source: Media Kit)

| Color | HEX | RGB | Usage |
|-------|-----|-----|-------|
| **Kyber Teal** | #31CB9E | 49, 203, 158 | Primary accent |
| **Kyber Navy** | #141927 | 20, 25, 39 | Dark backgrounds |

---

## Implementation Phases

### Phase 01: Color Palette Definition
**File:** [phase-01-color-palette.md](./phase-01-color-palette.md)
**Status:** Pending

Define complete color system with dark/light variants.

### Phase 02: Theme Implementation
**File:** [phase-02-theme-implementation.md](./phase-02-theme-implementation.md)
**Status:** Pending

Update Theme.kt with Material3 color schemes.

---

## Files to Modify

```
app/src/main/java/com/otistran/flash_trade/ui/theme/
├── Color.kt    # MODIFY - Add Kyber colors
└── Theme.kt    # MODIFY - Update color schemes
```

---

## Success Criteria

- [ ] All colors derived from Kyber brand
- [ ] WCAG 2.1 AA contrast compliance
- [ ] Dark mode as default
- [ ] Light mode functional
- [ ] Material3 color scheme complete
- [ ] No purple/pink default colors remaining

---

## Next Steps

1. Review phase-01 color palette
2. Implement Color.kt changes
3. Update Theme.kt schemes
4. Visual testing across screens

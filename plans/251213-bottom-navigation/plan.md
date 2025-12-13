# Bottom Navigation Implementation Plan

**Feature:** Bottom Navigation with Nested Back Stacks
**Date:** 2025-12-13
**Status:** Ready for Implementation
**Estimated Effort:** 8-12 hours

## Overview

Implement Material3 bottom navigation with nested graphs for Trading, Portfolio, and Settings screens. Each tab maintains independent back stack, state preserved on switch, conditional visibility for auth/detail screens. Follows Kyber brand design (Teal #31CB9E, Navy #141927).

## Architecture Summary

- **Pattern:** Single NavHost with nested navigation graphs per tab
- **State Management:** Centralized AppState holder for nav logic
- **Root Scaffold:** MainActivity level with conditional bottom bar
- **Type Safety:** Navigation 2.8+ with @Serializable routes
- **Back Stack:** Independent per tab, preserved on switch

## Phases

| Phase | Description | Status | Effort |
|-------|-------------|--------|--------|
| [01](./phase-01-route-restructure.md) | Navigation Route Restructure | Pending | 1.5h |
| [02](./phase-02-root-scaffold.md) | Root Scaffold & App State | Pending | 2h |
| [03](./phase-03-bottom-nav-component.md) | Bottom Navigation Component | Pending | 2.5h |
| [04](./phase-04-navigation-implementation.md) | Navigation Implementation | Pending | 2h |
| [05](./phase-05-testing-polish.md) | Testing & Polish | Pending | 2h |

## Key Dependencies

- Navigation Compose 2.8.0+ (already in project)
- Material3 NavigationBar component
- Kotlin Serialization plugin
- Research reports: `researcher-01-navigation-compose.md`, `researcher-02-ui-ux-design.md`

## Success Criteria

- Bottom bar visible on: Trading, Portfolio, Settings
- Bottom bar hidden on: Welcome, Login, TradeDetails
- Each tab preserves back stack independently
- State survives configuration changes
- Kyber brand colors applied correctly
- Smooth 300-350ms transitions
- No layout jank or flickers

## Technical Notes

- Max 200 lines per file (split if needed)
- Follow MVI architecture patterns
- Use existing Color.kt for Kyber brand
- Support dark/light themes
- No breaking changes to existing screens

## Files to Create

- `presentation/navigation/AppState.kt`
- `presentation/navigation/BottomNavBar.kt`
- `presentation/navigation/TopLevelDestination.kt`

## Files to Update

- `MainActivity.kt` - Add root Scaffold with bottom bar
- `NavGraph.kt` - Implement nested navigation graphs
- `Screen.kt` - Convert to @Serializable type-safe routes

## Unresolved Questions

None. All architectural decisions documented in research reports.

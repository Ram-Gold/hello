# 0026. Elevated Toast Pill and Floating Dock Clearance

Date: 2026-11-08
Status: Accepted

## Context

Material 3 `Scaffold` positions its default `snackbarHost` slot at the bottom-center of the screen. In Hello, primary navigation is driven by a `FloatingBottomDock` (housed at the bottom center with system navigation bar inset clearance). Consequently, when a toast / snackbar notification was displayed (e.g. "Marked as done!", "Contact updated", "Added"), it rendered directly on top of the floating bottom dock, occluding tab switching buttons, the companion Quick-Add FAB, and thumb navigation.

## Decision

1. **Elevated Bottom Placement with Dynamic Adaptive Clearance:**
   - Elevate toast notifications above the `FloatingBottomDock` on docked root tabs (`Home`, `People`, `Timeline`) with bottom clearance set to `dockHeight + 16.dp` (~96dp to 104dp above navigation bars).
   - On undocked sub-screens without the bottom dock (`PersonDetailScreen`, `PersonEditScreen`, Settings screens), set clearance adaptively to `navigationBars + 16.dp` above screen bottom.
   - Primary navigation controls (dock tabs, Quick-Add FAB, speed dial) and thumb navigation remain 100% unobstructed during toast displays.

2. **Tactile Pill Capsule Styling (`HelloToastHost` / `Toast Pill`):**
   - Depart from standard wide rectangular Material 3 banners in favor of a tactile compact capsule (`RoundedCornerShape(24.dp)`).
   - Built on a warm `surfaceContainerHigh` or `inverseSurface` pill with earthy contrast borders and subtle elevation, adhering to Hello's warm beige/terracotta design language (AGENTS.md Section 3).
   - Features a subtle leading status icon (e.g. `Icons.Rounded.CheckCircle` for confirmations, `Icons.Rounded.Info` for information) alongside concise message text.

3. **Global Architectural Consolidation:**
   - Create a reusable `HelloToastHost` / `HelloSnackbarHost` component and integrate it cleanly with `MainAppShell`'s global `SnackbarFeedbackCollector` as well as standalone screen scaffolds.
   - Ensure spring motion physics (`MotionTokens.badgePressSpring()` or snappy entrance/exit) for responsive feel without layout thrashing.

## Consequences

- **Positive:** Eliminates dock occlusion bug; users can freely navigate tabs, tap Quick-Add, or read list content while transient feedback is visible.
- **Aesthetic:** Unifies toast notifications under Hello's tactile, warm squircle and pill design system.


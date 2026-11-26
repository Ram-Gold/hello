# 0034: In-Place Floating Bottom Dock to Profile Encounter Pill Morph

**Status:** Accepted  
**Date:** 2026-11-19  
**Amends:** [ADR-0001](0001-floating-pill-and-quick-add-dock.md), [ADR-0010](0010-contact-profile-card-harmonization-and-minimal-communication.md), [ADR-0033](0033-predictive-back-warm-root-coordination.md)

## Context

Previously, navigating from a root tab (`HomeRoute`, `PeopleRoute`, `TimelineRoute`) to the `Contact Profile Screen` (`PersonDetailRoute`) translated the shell-level `Floating Bottom Dock` vertically downward off-screen (`shouldHideFloatingDock = true`), while `LivingTimeline` inside `PersonDetailScreen` rendered a separate inline `+ Log Met` button in its section header.

This created two issues:
1. The primary action on a Contact Profile Screen—logging a new `Encounter`—moved away from the ergonomic bottom thumb zone established by the `Floating Bottom Dock`.
2. Sliding the entire bottom dock down and revealing an inline button higher up the scrollable page broke spatial continuity between root surfaces and the Contact Profile Screen.

## Decision

1. **In-Place Morph from `Floating Bottom Dock` to Split Profile Action Bar (`Profile Reminder Pill` + `Profile Encounter Pill`)**:
   - When navigating between a Root Tab and `PersonDetailRoute` (while not in `Timeline Selection Mode`), the shell-level `FloatingDockOverlay` remains at its resting vertical position (`translationY = 0`, `dockVisibility = 1f`) instead of sliding down off-screen.
   - `HomeFloatingBottomDock` is driven by a continuous `profileMorphProgress` parameter in `0f..1f` (`0f` = Root Tab 3-tab dock + Quick-Add FAB; `1f` = split `60.dp` stadium pills: left `Profile Reminder Pill` (`Remind`) + `14.dp` gap + right `Profile Encounter Pill` (`+ Catching up?`)).
   - Remove both the inline `+ Log Met` and inline `+ Remind` buttons from the `LivingTimeline` section header on `PersonDetailScreen` so the bottom dock houses both primary actions at thumb level.

2. **Dual-Capsule In-Place Morph with Interior Blur Cross-Morph & Solid Right-to-Left Sweep (350ms Window, RAM-63)**:
   - **Zero Vertical Drop / Constant 60dp Height**: Both the left capsule (`Floating Bottom Dock` 3-tab navigation pill -> `Profile Reminder Pill`) and the right capsule (Quick-Add `+` FAB -> `Profile Encounter Pill`) remain locked at full `60.dp` height (`translationY = 0`) throughout the entire transition—never sliding downward or disappearing.
   - **Single Continuous Left Capsule & Locked 14dp Gap**: Instead of fading out the 3-tab navigation pill to `alpha = 0` and fading in a separate `Profile Reminder Pill`, a single crisp, 100% solid opaque left capsule (`left = 0.dp`, `height = 60.dp`, `RoundedCornerShape(30.dp)`, continuous `12.dp` resting shadow) contracts smoothly from `dockWidthDp` (`226.dp`–`246.dp`) down to `114.dp` in 1:1 lockstep with the right pill's sweep (`leftPillWidthDp = pillLeftDp - 14.dp`), preserving a rock-steady `14.dp` gap at every frame.
   - **Interior Blur Cross-Morph (`0.00f..0.88f`)**: Inside the clipped contracting left capsule, the outgoing fixed-width left-anchored 3-tab navigation layer blurs out (`0.dp -> 8.dp` via native GPU `BlurEffect` on Android 12+ / API 31+, clean opacity fallback below; `alpha: 1f -> 0f` over `0.00f..0.52f` with `StrongEaseOut`, never squishing tab slots), overlapping softly across the midpoint with the incoming center-aligned `Profile Reminder Pill` content (bell + `"Remind"`) un-blurring into focus (`8.dp -> 0.dp`, `alpha: 0f -> 1f`, `scale: 0.94f -> 1.0f` over `0.20f..0.88f`).
   - **100% Solid Opaque Right-to-Left Sweep (`0.00f..0.85f`, Unchanged)**: The right-anchored `primary` `+` button remains 100% solid opaque, rounds its corners from `20.dp` squircle to `30.dp` stadium pill (`0.00f..0.34f`), and expands smoothly right-to-left (`pillLeftDp: dockWidthDp + 14.dp -> 114.dp + 14.dp`), while `"Catching up?"` glides in right-to-left (`0.45f..1.00f`).
   - **Asymmetric Snappy Reverse & Predictive Back**: Navigating back to a root tab (or scrubbing predictive back `1f -> 0f`) expands the continuous left capsule back to `dockWidthDp` in 1:1 lockstep (`leftPillWidthDp = pillLeftDp - 14.dp`) while `Profile Reminder Pill` content blurs out quickly (`r: 0.00f..0.38f`) and the 3-tab navigation layer un-blurs smoothly back into crisp focus (`r: 0.28f..0.85f`).
   - **Profile Reminder Micro-Interactions**: Once in Profile mode (`progress >= 0.5f`), the unified left capsule preserves all `Profile Reminder Pill` interactions (press compression `0.96f`, hanging bell tilt/pendulum swing, interruptible blur-bridged `"✓ Saved"` confirmation with `primaryContainer` surface wash, and active-reminder filled bell + `6.dp` monochrome primary dot).

3. **Predictive Back & Selection Mode Coordination**:
   - Swiping back from `PersonDetailRoute` to a root tab scrubs `profileMorphProgress` in 1:1 reverse lockstep (`1f - deeperRouteBackProgress`) with `WarmRootTabsHost` depth parallax, while keeping `dockVisibility` at `1f` (zero vertical slide).
   - Entering `Timeline Selection Mode` on `PersonDetailRoute` or pushing deeper to `PersonEditRoute` hides the bottom overlay via `dockVisibility` (`1f -> 0f`), and predictive back from `PersonEditRoute -> PersonDetailRoute` or out of selection mode restores the `Profile Encounter Pill` (`profileMorphProgress = 1f`).
   - When `reduceAnimations` (`LocalReducedMotion`) is enabled, `profileMorphProgress` snaps immediately (`0ms`) without blur or intermediate interpolation.
   - Because the `Profile Encounter Pill` occupies the same `84.dp` bottom dock footprint on `PersonDetailRoute`, `PersonDetailScreen` reserves bottom scroll padding and `Toast Pill` clearance above the pill.

## Consequences

- **Positive**:
  - Seamless, tactile spatial continuity between the root `Floating Bottom Dock` and the Contact Profile's primary encounter action (`+ Catching up?`) in the natural bottom thumb zone.
  - Zero duplicate composer hierarchies or cross-screen handoff flicker because the single shell-level `FloatingDockOverlay` owns both states and every intermediate frame.
- **Negative / Tradeoffs**:
  - `FloatingDockOverlay` and `shouldHideFloatingDock` must distinguish `PersonDetailRoute` (where the dock stays vertically visible at `dockVisibility = 1f` and morphs via `profileMorphProgress = 1f`) from other deeper routes (`PersonEditRoute`, `Settings*`, `ArchivedContactsRoute`) where the dock still slides down off-screen.

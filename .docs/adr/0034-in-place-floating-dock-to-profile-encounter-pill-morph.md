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

2. **Unclipped Whole-Navbar Blur & Solid Right-to-Left Pill Sweep (350ms Window)**:
   - **Zero Vertical Drop / Constant 60dp Height**: Both the 3-tab navigation pill and the right-anchored Quick-Add `+` button remain locked at full `60.dp` height (`translationY = 0`) throughout the entire transition—never sliding downward or shrinking into a center dot.
   - **Whole-Navbar Unclipped Blur & Dissolve (`0.00f..0.55f`)**: The entire 3-tab navigation pill (its outer surface background together with its active pill, icons, and labels) is recorded/rendered inside an unclipped `+18.dp` padded layer and blurred immediately from frame 1 (`0.dp -> 12.dp` via `BlurEffect` on Android 12+ / API 31+ or progressive bilinear downsample/upsample bitmap crossfade on Android 7–11), dissolving smoothly (`alpha: 1.0f -> 0.0f` over `0.15f..0.55f`) while its resting shadow fades out early (`0.00f..0.26f`). Blurring the whole capsule unclipped prevents any sharp white semicircle from remaining on the right side of the navbar.
   - **100% Solid Opaque Right-to-Left Sweep (`0.00f..0.85f`)**: No gradient connector or translucent edge feather is used between the pills; the right-anchored `primary` `+` button stays 100% solid opaque (preventing background text bleed-through), rounds its corners from `20.dp` squircle to `30.dp` stadium pill (`0.00f..0.34f`), and expands smoothly right-to-left across the full dock footprint (`dockWidthDp + 14.dp + 60.dp`).
   - **Right-to-Left Copy Glide-In (`0.45f..1.00f`)**: The `+` icon refines (`28.dp -> 22.dp`, `0.12f..0.48f`) and glides right-to-left into its centered resting position while `"Catching up?"` glides in from right to left (`translationX` + `CompositingStrategy.Offscreen` `BlendMode.DstIn` `18.dp` soft feather + alpha fade).
   - **Symmetric Reverse**: Navigating back to a root tab (or scrubbing predictive back `1f -> 0f`) reverses the sweep left-to-right and un-blurs the whole 3-tab navbar smoothly back into `[3-tab pill] + [14dp gap] + [60dp squircle + FAB]`.

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

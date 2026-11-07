# 02: Universal Single-Size Borderless Stadium Tag Pill Across All Screens

**What to build:** Enforce a single, invariant stadium pill size (`CircleShape`, borderless, unified height/padding, and consistent typography) for tags across every screen in Hello, completely eliminating per-screen sizing variations, border strokes, and squircle shapes.

**Blocked by:** 01: Global Tag Color Reactivity and Repository Pipeline Sync

**Status:** done

- [x] `TonalTagPill` is standardized to a single universal size using `CircleShape` (stadium pill shape with full semicircular ends) and zero border strokes (`border = null`).
- [x] Internal pill padding and typography are unified to a single invariant standard (e.g. `horizontal = 10.dp, vertical = 5.dp`, `MaterialTheme.typography.labelSmall`) across all screens.
- [x] People screen filter bar: All filter pills (including the "All" pill and tag pills) use matching single-size stadium pill geometry and height without borders.
- [x] Contact cards on the People screen: Tags and the `+$extraTagsCount` overflow indicator match the universal single-size stadium pill shape and height.
- [x] Profile screen (`PropertiesPanel`), Contact Edit screen (`PersonEditScreen`), Quick Capture (`FlowAAddContactCard`), and Onboarding screens align to the exact same single-size stadium pill.
- [x] Unit tests and debug APK build compile and pass cleanly with zero regressions.


# 03: Sub-Screen Return Invariants, Dock Height Preservation, and Regression Suite

**What to build:**
Guarantee that returning from deeper screens (`PersonEditScreen`, `PersonDetailScreen`) back to the Relationship Tab reliably re-evaluates the active route as a root destination with inactive selection mode, restoring the Floating Bottom Dock without flicker. Preserve `dockHeightPx` throughout animations to prevent layout jumps or zero-height gaps, and run the comprehensive regression test suite verifying 100% pass rate.

**Blocked by:** 02 (Back Navigation Interception and In-Place Selection Exit)

**Status:** closed

- [x] Navigating back from `PersonEditScreen` (saving changes, canceling, deleting, or archiving) restores the Floating Bottom Dock on the Relationship Tab.
- [x] Navigating back from `PersonDetailScreen` restores the Floating Bottom Dock on the Relationship Tab.
- [x] Measured `dockHeightPx` is preserved during hide/restore transitions to avoid layout jumps or zero-offset miscalculations.
- [x] Adaptive spacing for Toast Pills correctly accounts for the restored Floating Bottom Dock height without overlap.
- [x] Reduced-motion mode snaps dock visibility instantly upon returning without lag or dropped frames.
- [x] Full navigation transition and ViewModel regression test suite runs and passes 100% (`./gradlew testDebugUnitTest`).

# 03: Sub-Screen Return Invariant & End-to-End Navigation Test Suite

**What to build:** Enforce that returning to `PeopleRoute` from sub-screens (`PersonEditScreen`, `PersonDetailScreen`, or `ArchivedContactsScreen`) reliably restores the Floating Bottom Dock with correct layout height preservation (`dockHeightPx`) and bottom clearance. Add automated regression tests verifying dock visibility across all edit exit paths.

**Blocked by:** 02: Back Navigation Interception and Edit State Exit Restoration

**Status:** ready-for-agent

- [ ] Returning to `PeopleRoute` from `PersonEditScreen` (via cancel, save, or back gesture) guarantees the Floating Bottom Dock is visible and interactive.
- [ ] Returning to `PeopleRoute` from `ArchivedContactsScreen` or `PersonDetailScreen` guarantees the dock is visible.
- [ ] Deleting or archiving from `PersonEditScreen` and returning to `PeopleRoute` restores the dock while positioning the confirmation Toast Pill comfortably above it.
- [ ] Dock height in pixels (`dockHeightPx`) is preserved across transitions without zero-height flicker.
- [ ] Automated regression tests in `NavigationTransitionTest` and `AppNavigationTest` verify dock visibility across all edit exit transitions with 100% pass rate.

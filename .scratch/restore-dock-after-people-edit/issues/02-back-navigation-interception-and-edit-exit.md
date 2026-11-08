# 02: Back Navigation Interception and Edit State Exit Restoration

**What to build:** Guarantee that back gestures, back button invocations, and header close ("X") taps on `ContactsScreen` always intercept and consume back navigation while in selection mode, clearing all selected contacts and immediately restoring the Floating Bottom Dock before allowing any route pops. Ensure that deselecting the final contact cleanly triggers dock restoration.

**Blocked by:** 01: Scoped Selection State and Orphan Pruning on Route/Tab Transitions

**Status:** ready-for-agent

- [ ] `BackHandler` on `ContactsScreen` intercepts back navigation while `uiState.isSelectionMode` is true, clears selections, and restores the Floating Bottom Dock without popping the route.
- [ ] Tapping the close ("X") button in the contextual selection header clears selections and restores the dock.
- [ ] Deselecting the last remaining contact automatically clears selection mode and animates the dock back to resting position (`translationY = 0`, opacity = 1).
- [ ] Reduced motion mode restores the dock instantaneously without animation delays.
- [ ] Automated tests verify back-press precedence and selection exit dock restoration.

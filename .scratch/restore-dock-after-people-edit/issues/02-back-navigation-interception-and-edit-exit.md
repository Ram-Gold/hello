# 02: Back Navigation Interception and In-Place Selection Exit

**What to build:**
Ensure that invoking back navigation (system back gesture, device back button, top bar back icon, or selection header dismiss button) while in selection edit mode on the Relationship Tab intercepts the back event to clear selected contacts and exit selection mode in place, without popping to the Home Tab. Exiting selection mode immediately restores the Floating Bottom Dock to full opacity and resting position (`translationY = 0`).

**Blocked by:** 01 (Scoped Selection State and Orphan Selection Pruning)

**Status:** closed

- [x] `BackHandler` in `ContactsScreen` intercepts back navigation while `isSelectionMode` is true, clearing selections via `OnClearSelection` without leaving the Relationship Tab.
- [x] Tapping the selection header "X" dismiss button clears selections and restores the dock.
- [x] Deselecting the last selected contact automatically exits selection mode and restores the dock.
- [x] A subsequent back press after selection mode is cleared performs normal route backstack navigation.
- [x] Dock visibility animation smoothly animates from hidden to visible (`translationY = 0`, `alpha = 1.0f`) upon edit exit.
- [x] Unit and UI contract tests verify back-interception precedence and in-place dock restoration.

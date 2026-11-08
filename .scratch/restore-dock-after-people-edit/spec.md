# Spec: Restore Floating Bottom Dock After Relationship Edit State

**Status:** closed

## Problem Statement

When a user is on the Relationship Tab (the People directory) and enters an edit state—specifically when activating multi-selection edit mode by long-pressing a contact card, or when navigating to edit contact details—the Floating Bottom Dock is intentionally dismissed from view to maximize vertical space for contextual actions (such as the selection action tray or full-screen editing controls). 

However, when the user navigates back (via Android system back gesture, hardware back button, top bar navigation controls, or the selection header dismiss button), the Floating Bottom Dock fails to reappear. The dock remains translated off-screen or unrendered, leaving the user stranded on the screen without primary navigation controls to switch between the Home Tab, Timeline Tab, and Relationship Tab.

This occurs because:
1. When multi-selection edit mode is engaged, the selection state remains active in memory. If system back navigation is handled by the navigation backstack before the local selection handler clears the selection, the app transitions to another root tab (e.g., Home Tab) while the selection mode flag remains `true`.
2. The shell-level dock visibility derivation logic (`shouldHideDock = shouldHideFloatingDock(currentRoute) || isSelectionMode`) observes the lingering selection flag across all root surfaces, permanently suppressing the Floating Bottom Dock.
3. Returning to the Relationship Tab from deeper editing surfaces does not trigger a re-evaluation or cleanup of orphaned selection states.

## Solution

Ensure that exiting an edit state on the Relationship Tab reliably and deterministically restores the Floating Bottom Dock across all exit paths:

1. **Prioritized Back Handler Contract:** Ensure that pressing back while in an edit or selection state on the Relationship Tab always intercepts the back invocation to clear all selections and exit the edit mode before allowing route pop navigation.
2. **Orphaned Selection Pruning on Tab/Route Changes:** Automatically reset and clear selection mode in the Relationship Tab ViewModel whenever navigation shifts away from the tab or when top-level destinations change, ensuring selection flags never persist silently in the background.
3. **Deterministic Dock Visibility Restoration:** Ensure the shell's visibility state derivation reliably detects the exit of edit states, triggering the slide-in and alpha animation of the Floating Bottom Dock back to full opacity and resting position (`translationY = 0`).
4. **Sub-Screen Navigation Invariant:** When returning to the Relationship Tab from edit profile screens or contact detail screens, verify that the route state correctly evaluates to a main root route with inactive selection mode, guaranteeing dock visibility.

---

## User Stories

1. As a user on the Relationship Tab, I want to long-press a contact card to enter selection edit mode, so that I can perform batch actions on multiple contacts.
2. As a user in selection edit mode on the Relationship Tab, I want the Floating Bottom Dock to slide smoothly off-screen, so that contextual action trays and list content have unobstructed screen real estate.
3. As a user in selection edit mode on the Relationship Tab, I want pressing the system back button or back gesture to exit selection mode and unselect all contacts without navigating away from the Relationship Tab.
4. As a user exiting selection edit mode via the back gesture, I want the Floating Bottom Dock to smoothly animate back into view immediately, so that I can navigate to other tabs.
5. As a user in selection edit mode on the Relationship Tab, I want tapping the close ("X") button in the selection header to clear all selections and restore the Floating Bottom Dock.
6. As a user in selection edit mode who deselects the last remaining selected contact, I want the screen to automatically exit edit mode and restore the Floating Bottom Dock.
7. As a user who navigates from the Relationship Tab to a contact's profile and opens the Edit Profile screen, I want the Floating Bottom Dock to remain hidden while editing.
8. As a user who cancels editing or saves changes on the Edit Profile screen and navigates back to the Relationship Tab, I want the Floating Bottom Dock to be fully visible and interactive.
9. As a user who deletes or archives a contact from the Edit Profile screen and is returned to the Relationship Tab, I want the Floating Bottom Dock to be visible and positioned correctly above the bottom window insets.
10. As a user who enters selection edit mode and then receives an external navigation event (e.g., deep link or notification tap), I want the selection state to be cleared so the Floating Bottom Dock is not hidden on the destination screen.
11. As a user on the Home Tab after backing out of the Relationship Tab, I want the Floating Bottom Dock to always be visible, ensuring primary tabs are never missing on root destinations.
12. As a user who completes a batch archive or batch delete in selection edit mode, I want the edit mode to dismiss and the Floating Bottom Dock to return to its docked resting position.
13. As a user who triggers an Undo Action from a Toast Pill after a batch deletion, I want the Floating Bottom Dock to stay visible while the Toast Pill floats comfortably above it without overlap.
14. As a user with reduced animations enabled, I want the Floating Bottom Dock to snap back into view instantly upon exiting the edit state without lag or dropped frames.

---

## Implementation Decisions

### 1. Interception and Precedence of Back Events in Edit Mode
- The BackHandler responsible for clearing selection mode must take precedence over navigation backstack popping whenever contacts are selected.
- If back is triggered while selection mode is active, the event must consume the back action, set `selectedContactIds` to empty, and exit selection mode, leaving the user on the Relationship Tab with the Floating Bottom Dock restored.
- A secondary back press after selection mode is already cleared should then execute standard system/navigational back behavior.

### 2. Tab Navigation & State Boundary Decoupling
- Selection mode is local to the active tab's domain workflow. A global flag in the application shell must not remain `true` when the user is not actively viewing that tab.
- When switching away from the Relationship Tab (or when top-level route transitions occur), any active selection state in the contacts view model must be automatically cleared or ignored by root tabs that are not in selection mode.
- The derived state for dock visibility must ensure that `isContactsSelectionMode` only affects dock visibility while the active tab is `HomeNavTab.PEOPLE` / `PeopleRoute`.

### 3. Shell Dock Visibility State Invariant
- The shell's dock visibility computation:
  - If current route is a root route (`HomeRoute`, `PeopleRoute`, `TimelineRoute`), the Floating Bottom Dock is visible (`targetValue = 1f`) unless the *active* tab has an active selection mode.
  - Exiting selection mode immediately shifts `targetValue` to `1f`.
  - When returning from deeper screens (such as `PersonEditRoute` or `PersonDetailRoute`) to `PeopleRoute`, the route update must trigger the animated progress to return to `1f`.

### 4. Layout Height Preservation & Recomposition Safety
- The measured dock height in pixels (`dockHeightPx`) must be retained even while the dock is translated off-screen or temporarily unrendered, preventing layout jumps or zero-height calculations when sliding back in.
- The Toast Pill's adaptive bottom offset calculation must smoothly track dock visibility progress during restoration.

---

## Testing Decisions

### What Makes a Good Test
- Tests should assert external contract behaviors and state transitions rather than internal composable layout primitives.
- Navigation tests must verify route changes, backstack pop behavior, and dock visibility predicates across transitions.
- View model tests must verify that selection mode lifecycle events (entering, toggling, clearing, and route dismissal) produce correct state models.

### Tested Modules
- **`AppNavigation` & Navigation Contract (`NavigationTransitionTest`, `PersonDetailNavigationTest`):**
  - Verify that `shouldHideFloatingDock` evaluates to `false` for `PeopleRoute` and `HomeRoute`.
  - Verify route resolution and dock visibility predicate behavior before and after popping from edit destinations.
- **`ContactsViewModel` (`ContactsViewModelTest`):**
  - Verify `OnContactLongClick`, `OnClearSelection`, and automatic exit when all contacts are deselected.
  - Verify state sanitization when navigating away or clearing filters.
- **App Shell State Derivation Contract:**
  - Verify that when `isContactsSelectionMode` changes from `true` to `false`, the effective dock hide flag transitions from `true` to `false`.
  - Verify that selection mode on an inactive tab does not suppress dock visibility on other root tabs.

### Prior Art
- `NavigationTransitionTest.kt`: Tests `shouldHideFloatingDock` across all route types.
- `ContactsViewModelTest.kt`: Tests `OnContactLongClick`, `OnToggleSelectContact`, and `OnClearSelection`.
- `PersonDetailNavigationTest.kt`: Tests back intercept logic and depth transitions.

---

## Out of Scope

- Redesigning the multi-selection action tray or adding new batch operations (e.g., batch tagging or batch export).
- Modifying the visual appearance, typography, or styling of the Floating Bottom Dock.
- Changing contact edit form validation, avatar cropping, or persistence mechanisms in `PersonEditScreen`.

---

## Further Notes

- In accordance with ADR-0005, cold-start latency (<50ms) and frame render times must remain uncompromised; state changes in dock visibility must operate cleanly in the GPU drawing phase without unnecessary layout re-passes.
- Terminology compliance: Uses **Floating Bottom Dock** (not "navbar"), **Relationship Tab** (not "people tab/contacts tab"), and **Toast Pill** (not "snackbar").


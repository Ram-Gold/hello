# 02: Pull-to-Action Navigation and Shell Wiring

**What to build:**
Register `ArchivedContactsRoute` inside `MainAppNavHost` in `MainAppShell.kt` with shared-axis slide and fade transitions, ensuring the floating dock hides automatically while on the full screen. Connect `ContactsScreen` pull-to-action threshold release directly to navigating to `ArchivedContactsRoute`, replacing the ephemeral modal bottom sheet.

**Blocked by:** 01 (Archived Contacts Route and Full-Screen UI)

**Status:** closed

- [x] Add `ArchivedContactsRoute` destination to `MainAppNavHost` with smooth enter and exit transitions.
- [x] Connect `PeopleTabDestination` and `ContactsScreen` navigation event: pulling past threshold navigates to `ArchivedContactsRoute`.
- [x] Deprecate/remove `ContactsUiState.isArchivedSheetOpen` and bottom sheet modal code in `ContactsScreen`.
- [x] Ensure floating bottom dock remains hidden during full screen archived view and smoothly returns on pop.


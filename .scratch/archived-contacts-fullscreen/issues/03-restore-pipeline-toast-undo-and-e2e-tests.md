# 03: Restore Pipeline, Toast Undo & E2E Tests

**What to build:**
Verify single-tap restore action updates Room SQLite reactively (`isArchived = false`), immediately removes the contact from the full screen, and triggers the semantic Toast Pill with working `"Undo"` support. Add unit tests for screen state, navigation events, and restore/undo pipeline, verifying a 100% test pass rate across the full project test suite.

**Blocked by:** 02 (Pull-to-Action Navigation and Shell Wiring)

**Status:** closed

- [x] Connect restore action to `ContactsViewModel.onEvent(ContactsUiEvent.OnRestoreContact)` and verify reactive Room updates.
- [x] Verify semantic Toast Pill with `"Undo"` action successfully re-archives contact if pressed.
- [x] Write unit tests for `ArchivedContactsScreen` interactions and navigation wiring.
- [x] Run full project test suite (`./gradlew testDebugUnitTest`) and verify 100% pass rate.


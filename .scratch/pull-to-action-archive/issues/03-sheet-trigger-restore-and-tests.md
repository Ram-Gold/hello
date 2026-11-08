# 03: Archived Contacts Trigger, Restore Flow & E2E Tests

**What to build:**
Connect the release-past-threshold trigger event to opening the `ArchivedContactsSheet`. Ensure single-tap contact restoration updates SQLite, removes the card from the sheet, re-inserts into active contacts, and triggers the Toast Pill. Write automated unit and layout seam tests validating rubber-band math, threshold crossing, and snap-back state.

**Blocked by:** 02 (Scroll-Driven Reveal Header with Animated Archive Icon & Text)

**Status:** closed

- [x] Wire threshold release action to `onEvent(ContactsUiEvent.OnOpenArchivedSheet)`.
- [x] Verify `ArchivedContactsSheet` multi-restore action cleanly moves contacts back to active list and surfaces Toast Pill.
- [x] Add unit tests verifying `PullToActionNestedScrollConnection` offset calculation, rubber-band resistance, and threshold triggering.
- [x] Run full project test suite (`./gradlew testDebugUnitTest`) and verify 100% pass rate.


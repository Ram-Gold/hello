# 03: Timeline Screen Minimal Header Cleanup

**What to build:** On the Timeline screen, remove the verbose "FUTURE HORIZON" title and "Upcoming birthdays, notes & cadence catch-ups" subtitle section header, providing an uncluttered, minimal presentation that seamlessly leads directly into upcoming timeline nodes.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Remove the `future-section-header` item from `TimelineScreen.kt` (stripping the "FUTURE HORIZON" and "Upcoming birthdays, notes & cadence catch-ups" text).
- [x] Ensure list spacing and continuous timeline vertical spine alignment remain fluid and properly padded.
- [x] If `TimelineFilter.FUTURE` is active or future nodes are present, timeline items render seamlessly without awkward top gap.
- [x] Unit tests and debug APK build compile and pass cleanly.


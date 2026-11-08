# 03: TimelineScreen Encounter Node Alignment & Full Test Suite

**What to build:**
Refactor encounter nodes in `TimelineScreen` to remove the inline checkmark beside `node.contactName` and integrate `SelectableAvatarBadge` for consistent selection appearance across the app. Run the full automated test suite to verify a 100% pass rate.

**Blocked by:** 02 (ContactCard Name Row Refactor & Avatar Checkmark Integration)

**Status:** closed

- [x] Remove inline checkmark icons from `TimelineScreen.kt` encounter headers.
- [x] Wrap timeline avatars in `SelectableAvatarBadge` connected to `isSelected`.
- [x] Add unit tests verifying `ContactCard` and timeline selection contracts.
- [x] Run full project test suite (`./gradlew testDebugUnitTest`) and verify 100% pass rate.

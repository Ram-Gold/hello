# 03: TimelineScreen Encounter Node Alignment & Full Test Suite

**What to build:**
Refactor encounter nodes in `TimelineScreen` to remove the inline checkmark beside `node.contactName` and integrate `SelectableAvatarBadge` for consistent selection appearance across the app. Run the full automated test suite to verify a 100% pass rate.

**Blocked by:** 02 (ContactCard Name Row Refactor & Avatar Checkmark Integration)

**Status:** ready-for-agent

- [ ] Remove inline checkmark icons from `TimelineScreen.kt` encounter headers.
- [ ] Wrap timeline avatars in `SelectableAvatarBadge` connected to `isSelected`.
- [ ] Add unit tests verifying `ContactCard` and timeline selection contracts.
- [ ] Run full project test suite (`./gradlew testDebugUnitTest`) and verify 100% pass rate.


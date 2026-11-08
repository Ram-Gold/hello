# 03: List Placement Springs, Restore Animation & Full Test Suite

**What to build:**
Wire Compose `Modifier.animateItem` into `ContactsScreen` and `ArchivedContactsScreen` lists so adjacent cards smoothly slide into position without jump-cuts when items leave. Implement the complementary restore card exit animation in `ArchivedContactsScreen`, and run the automated test suite verifying 100% pass rate.

**Blocked by:** 02 (Dissolve Deletion Animation and Batch Selection Exit)

**Status:** ready-for-agent

- [ ] Apply `Modifier.animateItem` placement springs across `ContactsScreen` contact cards so remaining items smoothly reflow.
- [ ] Implement restore row departure animation in `ArchivedContactsScreen` upon tapping "Restore".
- [ ] Add unit tests verifying exit motion specs, duration bounds, and reduced motion bypass.
- [ ] Run full project test suite (`./gradlew testDebugUnitTest`) and verify 100% pass rate.


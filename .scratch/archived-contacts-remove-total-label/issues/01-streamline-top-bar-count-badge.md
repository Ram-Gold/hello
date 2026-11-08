# 01: Streamline Archived Contacts Top Bar Count Badge & Contract Tests

**What to build:** In `ArchivedContactsScreen`, update the top bar badge to display only the numeric count (e.g. "3", "0") rather than the prefixed string "Total: 3". Refine the tonal badge capsule padding to fit the compact number, attach accessible semantic descriptions for screen readers, and add verification tests in `ArchivedContactsContractTest` asserting the absence of "Total" and presence of the clean count.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Top bar count badge in `ArchivedContactsTopBar` renders `"$count"` without any `"Total:"` prefix.
- [ ] Badge capsule padding is refined (`horizontal = 8.dp, vertical = 4.dp`) to keep single- and multi-digit counts balanced and centered.
- [ ] Accessibility semantics provide the full contextual description (e.g. `"$count archived ${terminology.pluralTitle.lowercase()}"`) for TalkBack users.
- [ ] Restoring or archiving contacts updates the displayed number badge immediately.
- [ ] Contract tests in `ArchivedContactsContractTest` verify that the count badge representation contains exclusively the numeric value and omits the string "Total".


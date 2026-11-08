# 01: Scoped Selection State and Orphan Selection Pruning

**What to build:**
Scope selection mode visibility logic in `MainAppShell` so multi-selection on the Relationship Tab only suppresses the Floating Bottom Dock when the Relationship Tab is actively visible. Prune orphaned selection states in `ContactsViewModel` whenever top-level navigation moves to another tab, preventing background selection flags from stranding the user without primary navigation dock controls.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Shell dock visibility derivation (`shouldHideDock`) checks whether `currentRoute` is the Relationship Tab before letting `isSelectionMode` hide the dock.
- [ ] Navigating between root tabs (Home, Timeline, Relationship) cleanses or ignores inactive tab selection states.
- [ ] Switching away from the Relationship Tab while contacts are selected automatically resets/prunes selection mode in `ContactsViewModel`.
- [ ] Unit tests in `ContactsViewModelTest` verify selection state cleanup upon route/tab departure.
- [ ] Automated tests in navigation contract verify dock remains visible on Home and Timeline even if contact selection was active.

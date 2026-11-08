# 01: Scoped Selection State and Orphan Pruning on Route/Tab Transitions

**What to build:** Ensure selection mode in `MainAppShell` is strictly scoped to the active route (`isContactsSelectionMode` only suppresses the Floating Bottom Dock while on `PeopleRoute`; `isTimelineSelectionMode` only while on `TimelineRoute`). When navigating away from `PeopleRoute` to another root tab or deeper destination, automatically clear or ignore orphaned contact selections so background flags never permanently suppress the Floating Bottom Dock.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Shell dock visibility computation scopes `isContactsSelectionMode` strictly to `currentRoute == PeopleRoute.ROUTE_NAME` (and timeline selection to `TimelineRoute.ROUTE_NAME`).
- [ ] Navigating between root tabs (e.g. from `PeopleRoute` to `HomeRoute` or `TimelineRoute`) automatically clears selection mode in `ContactsViewModel`.
- [ ] An active contact selection state cannot hide the Floating Bottom Dock on `HomeRoute` or `TimelineRoute`.
- [ ] Unit tests in test suite verify route-scoped selection derivation and tab-transition orphan pruning.

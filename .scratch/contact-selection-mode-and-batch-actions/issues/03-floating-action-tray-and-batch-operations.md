# 03: Floating Action Tray, Batch Deletion Dialog & Select-All

**What to build:** Build the vertical floating action tray docked on the right edge with Share, Delete, and Kebab (⋮) buttons. The Kebab button anchors a Material 3 `DropdownMenu` with a "Select all" item that selects all currently visible/filtered contacts. Tapping Delete displays a Material 3 confirmation dialog detailing the number of contacts to permanently remove; confirming permanently deletes them from Room DB via `ContactRepository` and exits selection mode.

**Blocked by:** 01: Core Selection State, Long-Press Trigger & Decoupled Card Geometry, 02: Pure Domain vCard Exporter & Share Action

**Status:** done

- [x] Vertical floating action tray renders on the right edge of `ContactsScreen` with Share, Delete, and Kebab (⋮) buttons.
- [x] Tapping Kebab (⋮) opens a DropdownMenu with "Select all".
- [x] Tapping "Select all" selects all contacts visible under the active search/circle filter.
- [x] Tapping Delete opens a Material 3 AlertDialog showing the count of contacts to delete.
- [x] Confirming delete removes selected contacts from Room DB and exits selection mode.
- [x] Unit tests in `ContactsViewModelTest` verify batch delete and select-all logic.
- [x] Device verification confirms smooth gestures, visual card decoupling, and batch operations on live hardware.

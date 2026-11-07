# ADR-0014: Contact Selection Mode, Decoupled Card Geometry, and Floating Action Tray

- **Status:** Accepted
- **Date:** 2026-11-05
- **Context:** Providing batch operations (sharing vCards, deletion with confirmation, and select all) on the Relationship Tab without disrupting Hello's connected card architecture.

---

## 1. Problem & Decision Drivers

In Hello's Relationship Tab, contacts are presented as vertically connected Material 3 Expressive surfaces (`ADR-0008`), grouped by alphabetical headers.
Users need a way to perform batch actions on multiple contacts simultaneously (e.g. sharing vCards with colleagues, deleting multiple outdated contacts).
Key design drivers:
1. **Zero Accidental Enters:** Entering multi-selection must be deliberate and frictionless (long-press gesture).
2. **Distinct Selected Visual Language:** Selected contacts must immediately stand out from normal connected items while preserving theme harmony.
3. **Screen Real Estate & Dock Arbitration:** The persistent Floating Bottom Dock (`ADR-0001`) occupies the bottom screen real estate. Displaying batch actions alongside the dock creates tap conflicts and visual clutter.
4. **Safety Invariants:** Deleting local-first SQLite records is permanent (`ADR-0006`). Accidental batch deletions must be guarded with explicit confirmation.

---

## 2. Decisions

### Decision 1: Long-Press Activation & Selection State Management
- Long-pressing any Contact Card activates **Contact Selection Mode**.
- The initial long-pressed card is immediately selected.
- While in Selection Mode, tapping any card toggles its selection state (select / deselect) rather than opening the Contact Profile Screen.
- If the user deselects the final selected contact, Selection Mode automatically exits, returning to normal list navigation.
- The system Back button or the top-right 'X' icon button immediately cancels Selection Mode and clears all selections.

### Decision 2: Decoupled Card Geometry & Theme-Aware Red Container
- **Decoupled Roundness:** Unselected contacts in a group remain connected with zero-radius touching edges (`shapeFor(index, count)`). When selected, a Contact Card decouples from the connected stack into an independent, fully-rounded surface (`RoundedCornerShape(24.dp)` on all four corners, matching the top and bottom group roundness).
- **Theme-Aware Red Container:** Selected cards render with dynamic theme-aware red container coloring (`MaterialTheme.colorScheme.errorContainer` with on-error-container text/icon tints) adapting seamlessly between light and dark themes.
- **Inline Name Checkmark:** An inline checkmark icon appears beside the contact's bold name to unambiguously confirm selection.

### Decision 3: Floating Vertical Action Tray & Dock Dismissal
- A vertical floating action tray docks on the right edge of the screen:
  `[ Share | Delete | Kebab (⋮) ]`.
- While Selection Mode is active, the Floating Bottom Dock automatically translates down off-screen.
- **Top Bar Replacement:** The search bar and standard header are replaced by a contextual header showing `[X selected]` on the left and a circular 'X' cancel button on the top right.

### Decision 4: Batch Action Specifications
- **Share:** Generates a single combined standard vCard (`.vcf`) payload containing all selected contacts (with names, phones, emails, roles, companies, and notes) and opens the Android Share Sheet via `Intent.ACTION_SEND` (`text/x-vcard`).
- **Delete:** Displays an in-app Material 3 confirmation dialog detailing the exact number of contacts to be permanently deleted before executing Room database deletion.
- **Kebab (⋮) -> Select All:** Opens a Material 3 `DropdownMenu` anchored to the kebab button featuring a "Select all" item. Tapping "Select all" selects all contacts visible under the active filter/search query.

---

## 3. Consequences

### Positive
- **Clear Interaction Seam:** Long-press is intuitive and standard on Android CRM applications.
- **High-Contrast Feedback:** The combination of fully-rounded shape decoupling, theme-aware red surface, and checkmark icon provides instantaneous visual confirmation.
- **Clean Screen Budget:** Dismissing the bottom dock eliminates bottom-of-screen interaction collisions while the side tray keeps primary thumbs in comfortable reach.
- **Data Safety:** Explicit confirmation prevents catastrophic batch deletion.

### Negative / Tradeoffs
- Tapping a card during selection mode is repurposed to selection toggling; users cannot view details without first exiting selection mode.

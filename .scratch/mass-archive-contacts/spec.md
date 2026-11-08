# Feature Specification: Mass Archive in Contact Selection Mode

Status: ready-for-agent  
Date: 2026-11-08  
Topic: Relationship Tab / Contact Selection Mode / Batch Operations  

---

## Problem Statement

When organizing an active relationship directory, users often accumulate contacts who have transitioned out of regular cadence (e.g., past colleagues, graduated classmates, or finished client engagements). Today, users can only archive contacts individually one-by-one from their individual profile screens or swipe actions. 

While Contact Selection Mode allows users to select multiple or all contacts to delete them permanently, there is no mass operation to archive them. Users who wish to declutter their active directory without permanently destroying contact records, notes, and encounter histories are forced into tedious repetitive actions or risk accidental permanent deletion.

---

## Solution

Extend Contact Selection Mode on the Relationship Tab with a first-class **Mass Archive** batch action:

1. **Floating Action Tray Integration:** Add a dedicated Archive action button to the floating vertical action tray alongside the existing Share and Delete actions.
2. **Instant Execution with Zero Friction:** Tapping the Archive action immediately marks all selected contacts as archived (`isArchived = true`) in the local SQLite canonical database.
3. **Automatic Mode Teardown:** Clears the selection set and automatically exits Contact Selection Mode, returning the Relationship Tab to standard navigation and smoothly restoring the Floating Bottom Dock.
4. **Instant Active Directory Pruning:** The active directory list reactively filters out the archived contacts with sub-2ms local persistence.
5. **Transient Recovery via Toast Pill:** Surfaces an elevated Toast Pill above the restored Floating Bottom Dock displaying `"Archived N contacts"` paired with an `Icons.Rounded.Archive` leading icon and an `"Undo"` action button.
6. **Multi-Item Undo Support:** Tapping `"Undo"` on the Toast Pill atomically restores all recently mass-archived contacts back to active status and brings them back into the active directory.

---

## User Stories

1. As a user reviewing my relationship directory, I want to long-press any contact card to enter Contact Selection Mode, so that I can prepare multiple contacts for a batch operation.
2. As a user in Contact Selection Mode, I want to see an Archive action button within the floating vertical action tray, so that I have a clear and accessible target to archive my selections.
3. As a user with multiple contacts selected, I want to tap the Archive button in the action tray, so that all selected contacts are archived simultaneously in one tap.
4. As a user who wants to clear a large group of inactive contacts, I want to open the kebab menu in the action tray and tap "Select all", so that every contact matching my current filter is selected.
5. As a user who selected all contacts, I want to tap Archive, so that my entire active directory (or active filtered segment) is cleanly moved to the archive in a single operation.
6. As a user filtering by a specific tag (e.g., `#project-2024`), I want to select all filtered contacts and archive them, so that finished project rosters can be archived without affecting other contacts.
7. As a user with an active search query, I want to select all search results and mass archive them, so that matching keyword groups can be archived together.
8. As a user executing a mass archive, I want Contact Selection Mode to exit immediately upon triggering the archive, so that I am returned to my normal browsing experience without having to tap cancel.
9. As a user executing a mass archive, I want the active relationship directory to update instantly without UI lag, so that the archived contacts immediately disappear from the active view.
10. As a user executing a mass archive, I want to see an elevated Toast Pill stating "Archived N contact(s)" with an Archive icon, so that I receive unambiguous visual confirmation of how many relationships were archived.
11. As a user who accidentally tapped Archive or made a mistaken selection, I want an "Undo" action on the confirmation Toast Pill, so that I can instantly revert the entire mass archive within the 5-second transient window.
12. As a user tapping "Undo" on the mass archive Toast Pill, I want all affected contacts to immediately reappear in the active directory with their previous state intact, so that I never lose my directory organization due to a slip.
13. As a user navigating to the Archived Contacts Screen after a mass archive, I want to see all newly archived contacts listed there, so that I can inspect or restore them individually whenever needed.
14. As a user who mass archives contacts marked as Favorite Contacts, I want their cards removed from the active Favorites strip, so that the quick-access carousel only contains actively engaged relationships.
15. As a user mass archiving contacts who have scheduled FollowUps or Subconscious Check-Ins, I want those proactive reminders paused, so that my Home Tab and Living Timeline remain clutter-free.
16. As a user who mass archives contacts, I want all notes, encounters, places, and birthday milestones preserved in SQLite, so that archiving remains strictly non-destructive.
17. As a user operating Hello completely offline, I want the mass archive operation to execute within a single atomic local Room transaction, so that data integrity is guaranteed even during unexpected app pauses or shutdowns.
18. As a user interacting with the floating action tray on a mobile device, I want the Archive button to have adequate touch target sizing (at least 44dp) and clear semantic iconography, so that I can trigger it comfortably with one hand.

---

## Implementation Decisions

### 1. Presentation & State Architecture (`ui/contacts`)
- **Event Contract Extension:** Add a new event `OnArchiveSelected` to the existing `ContactsUiEvent` sealed interface.
- **Action Tray Interface:** Update the floating vertical action tray component to accept an `onArchiveClick: () -> Unit` callback. Place the Archive icon button above the Delete button (`[ Share | Archive | Delete | Kebab (⋮) ]`), visually grouping non-destructive and data-management actions together.
- **Instant Flow over Blocking Dialog:** Unlike Delete—which permanently drops SQLite rows and therefore mandates a modal confirmation dialog—Archive is non-destructive, reversible, and recoverable. Therefore, `OnArchiveSelected` executes immediately, dismisses selection mode, and emits a Toast Pill with an Undo action.
- **Selection State Reset:** On archive execution, `selectedContactIds` is reset to an empty set and `isSelectionMode` becomes `false`, allowing the Floating Bottom Dock to smoothly transition back into view.

### 2. Repository & Persistence Architecture (`data/repository`)
- **Batch Archiving Method:** Introduce `archiveContacts(contactIds: Set<String>, archived: Boolean = true)` to `ContactRepository`.
- **Multi-Item Undo Cache:** Extend the repository's transient undo operation pipeline to support batch operations:
  ```kotlin
  // Prototype concept for batch undo operation
  sealed interface LastArchiveOperation {
      data class Single(val contactId: String, val previousArchived: Boolean) : LastArchiveOperation
      data class Batch(val contactIds: Set<String>, val previousArchived: Boolean) : LastArchiveOperation
  }
  ```
  `undoLastArchive()` will inspect whether the last archive was single or batch and revert all respective contact states accordingly in Room SQLite.

### 3. Toast Pill Clearance & Styling
- The resulting feedback message uses the standard `ToastPill` with `ToastPillSemantic.ARCHIVE`, ensuring proper tinting, semantic iconography, and adaptive vertical clearance above the Floating Bottom Dock (`dockHeight + 16.dp`).

---

## Testing Decisions

### Good Test Principles
- Tests must assert observable presentation behavior and data contracts, not internal component layout details or coroutine dispatch mechanisms.
- Assert state changes: selection set clearance, active list membership exclusion, archived list inclusion, and Toast Pill text & undo trigger.

### Target Test Seams
1. **Primary Seam — Presentation State Contract (`ContactsViewModelTest`):**
   - **Batch Selection & Archive:** Selecting multiple contacts and dispatching `OnArchiveSelected` prunes them from `uiState.value.contacts`, populates `uiState.value.archivedContacts`, resets `selectedContactIds`, and turns `isSelectionMode` to `false`.
   - **Select All & Mass Archive:** Selecting all visible contacts and archiving clears the active directory and updates total counts.
   - **Filtered Mass Archive:** Applying a tag filter or search query, selecting all visible, and archiving only touches the filtered subset; non-matching contacts remain active.
2. **Secondary Seam — Repository Batch & Undo Contract (`ContactRepositoryTest`):**
   - Verify `archiveContacts(ids, archived = true)` updates Room entities in a single transaction.
   - Verify `undoLastArchive()` successfully reverts all contacts in the batch back to `isArchived = false`.

### Prior Art
- `ArchivedContactsContractTest`: Verifies single restore and undo contract transitions.
- `ContactsViewModelTest`: Verifies selection mode entry, tag-filtering selection pruning, and select-all mechanics.

---

## Out of Scope

- **Mass Restore within Archived Contacts Screen:** Batch selection on the full-screen archived list is out of scope for this feature (contacts can be restored individually or mass-restored in a future enhancement).
- **Auto-Archiving Cadence Rules:** Automatically archiving contacts based on elapsed days without encounters is handled separately under Subconscious Check-Ins.
- **Export Bundle Modifications:** Export schemas already encode `isArchived: Boolean` in JSON and YAML frontmatter; no schema changes are required.

---

## Further Notes

- Maintains complete alignment with **ADR-0014** (Contact Selection Mode and Action Tray) and **ADR-0027** (Toast Pill Semantic Icons & In-Memory Undo Pipeline).
- Supersedes the historical limitation where batch operations only supported Share and Delete.


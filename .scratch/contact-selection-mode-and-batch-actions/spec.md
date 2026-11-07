# Specification: Contact Selection Mode & Batch Actions

## Problem Statement
In Hello's Relationship Tab, contacts are presented in vertically connected cards grouped by alphabetical section headers. Currently, users can only interact with contacts one at a time (tapping to view details). There is no mechanism to select multiple contacts to batch-export their vCards, batch-delete outdated contacts, or select all filtered contacts. Attempting to manage multiple relationships individually is slow and creates unnecessary cognitive friction.

## Solution
Introduce **Contact Selection Mode** on the Relationship Tab. A user can long-press any Contact Card to enter selection mode, automatically selecting that card. Selected cards visually decouple from the connected card stack into independent, fully rounded surfaces (`RoundedCornerShape(24.dp)` on all four corners) tinted with a theme-aware red container (`errorContainer`) and displaying an inline checkmark beside their bold name. 

While in this mode:
- The persistent Floating Bottom Dock translates smoothly down off-screen.
- The top search bar is replaced by a contextual header showing `[X selected]` and a circular 'X' cancel button on the top right.
- A vertical floating action tray appears on the right edge containing:
  - **Share**: Generates a single combined `.vcf` vCard file and launches the Android Share Sheet.
  - **Delete**: Displays a Material 3 confirmation dialog detailing the number of contacts to permanently delete from Room DB.
  - **Kebab (⋮)**: Opens a popover menu with a `"Select all"` option (selecting all currently visible contacts under active circle/search filters).
- Tapping any card toggles its selection state.
- Deselecting the final selected card or tapping 'X' / pressing system Back automatically exits selection mode and restores the standard interface and Floating Bottom Dock.

## User Stories

1. As a user, I want to long-press on any contact card in the Relationship Tab, so that I can immediately enter selection mode with that contact selected.
2. As a user, I want to see a checkmark next to the name of each selected contact, so that I have unambiguous confirmation of which contacts are chosen.
3. As a user, I want selected contact cards to turn into a theme-aware red container, so that they visually stand out from unselected cards across both light and dark themes.
4. As a user, I want selected contact cards to become fully rounded on all four corners (matching the first and last cards in a group), so that they visually decouple from the connected card stack.
5. As a user in selection mode, I want single taps on contact cards to toggle their selection state on or off rather than opening their profile screen, so that I can easily pick multiple people.
6. As a user in selection mode, I want the persistent Floating Bottom Dock to slide down off-screen, so that it does not cause visual clutter or tap collisions with batch actions.
7. As a user in selection mode, I want the top bar to show the count of selected contacts (e.g. "3 selected"), so that I always know how many items my actions will affect.
8. As a user in selection mode, I want a prominent 'X' button on the top right of the screen, so that I can cancel selection mode and unselect everything with a single tap.
9. As a user in selection mode, I want pressing the system Android Back gesture/button to cancel selection mode, so that standard platform navigation conventions work naturally.
10. As a user in selection mode, I want deselecting the last selected contact to automatically exit selection mode, so that I am returned to normal browsing without extra taps.
11. As a user in selection mode, I want a floating vertical action tray docked on the right edge containing Share, Delete, and a Kebab icon, so that primary batch actions are easily reachable with my thumb.
12. As a user in selection mode, I want tapping the Share icon in the action tray to generate a combined standard `.vcf` file and open the system Share Sheet, so that I can easily send multiple contacts to other apps or people.
13. As a user in selection mode, I want tapping the Delete icon in the action tray to display a clear confirmation dialog showing the exact count of contacts being deleted, so that I don't accidentally erase data permanently.
14. As a user, I want confirming the delete dialog to permanently remove the selected contacts and all their associated records from the local Room database, so that my directory stays tidy.
15. As a user in selection mode, I want tapping the Kebab (⋮) icon in the action tray to open a menu with "Select all", so that I can quickly select all currently visible contacts without tapping each one manually.
16. As a user with an active filter or search query, I want "Select all" to select only the contacts matching my current filter, so that I can perform targeted batch actions.

## Implementation Decisions

### 1. Architectural Boundaries & State Machine
- **State Seam**: `ContactsUiState` extended with:
  - `selectedContactIds: Set<String> = emptySet()`
  - Computed property `isSelectionMode: Boolean get() = selectedContactIds.isNotEmpty()`
  - `isDeleteConfirmationVisible: Boolean = false`
- **Intent Seam**: `ContactsUiEvent` updated with explicit intents:
  - `OnContactLongClick(contactId: String)`
  - `OnToggleSelectContact(contactId: String)`
  - `OnSelectAllVisibleContacts`
  - `OnClearSelection`
  - `OnRequestDeleteSelected`
  - `OnConfirmDeleteSelected`
  - `OnDismissDeleteDialog`

### 2. ContactCard Interaction & Presentation
- Update `ContactCard` to use `Modifier.combinedClickable(onClick = ..., onLongClick = ...)` with `@OptIn(ExperimentalFoundationApi::class)`.
- When `isSelected == true`:
  - Background surface color switches to `MaterialTheme.colorScheme.errorContainer` with appropriate on-container content tints.
  - Shape changes from connected `shapeFor(index, count)` to fully rounded `RoundedCornerShape(24.dp)` on all four corners.
  - Header row displays an inline checkmark icon next to the bold contact name.
- When `isSelectionMode == true` and `isSelected == false`:
  - Retains standard connected shape and surfaceContainer background, but click handler delegates to `OnToggleSelectContact`.

### 3. Floating Action Tray & Chrome Coordination
- The Floating Bottom Dock in `MainAppShell` already uses `dockVisibilityProgressState`: when `isSelectionMode` is true, dock progress animates to 0f (translating off-screen).
- A vertical floating action tray composable (`ContactSelectionActionTray`) docks at `Alignment.CenterEnd` with 16dp end margin:
  - Surface pill with vertical column layout containing Share, Delete, and Kebab (⋮) icon buttons.
  - Kebab button anchors a Material 3 `DropdownMenu` with a "Select all" item.
- Top bar switches between standard search header and contextual selection header with animation (`AnimatedContent`).

### 4. Pure Domain vCard Exporter
- Introduce pure Kotlin utility `VCardExporter.generateCombinedVCard(contacts: List<ContactSummary>): String`.
- Formats standard vCard 3.0 syntax including `FN`, `N`, `TEL`, `EMAIL`, `TITLE`, `ORG`, and `NOTE`.
- Android Share Sheet integration writes the string to a cache vCard file via FileProvider and fires `Intent.ACTION_SEND` with `text/x-vcard`.

## Testing Decisions

- **Good Test Criteria**: Test strictly through the two defined public seams (`ContactsViewModel` for state reducer & repository interactions; `VCardExporter` for RFC-compliant vCard syntax generation). Zero internal layout inspection.
- **Seams Tested**:
  1. `ContactsViewModelTest`:
     - Long-press triggers selection mode and adds contact ID to `selectedContactIds`.
     - Toggle selection adds and removes contact IDs.
     - Deselecting last item resets selection mode.
     - `OnSelectAllVisibleContacts` selects all filtered contacts.
     - `OnClearSelection` empties selection.
     - `OnConfirmDeleteSelected` calls `repository.deleteContact` for each selected contact and clears selection.
  2. `VCardExporterTest`:
     - Single and multiple contacts export with valid `BEGIN:VCARD` ... `END:VCARD` blocks.
     - Proper escaping of special characters in notes and names.

## Out of Scope
- Selection mode on the Home Screen (limited strictly to Relationship Tab).
- Batch editing of tags or circles (can be added in future iterations).
- Batch drag-and-drop reordering.

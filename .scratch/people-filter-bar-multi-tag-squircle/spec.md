# Specification: People Filter Bar Multi-Tag Selection, Equal-Size "All" Pill, and Squircle Selection State

**Status:** done

## Problem Statement

On the Relationship Tab ("People" screen), the filter bar currently has three visual and functional friction points:
1. **Inconsistent "All" Filter Pill Size:** The leading `"All"` chip uses Material 3's `FilterChip` component, which enforces a taller minimum height (`32.dp`) and different internal padding compared to the adjacent `TonalTagPill` components (`horizontal = 10.dp, vertical = 5.dp`), causing visual misalignment in the filter strip.
2. **Cluttered Checkmark Selection Indicator:** Selecting a tag pill inserts a leading checkmark icon (`Icons.Rounded.Check`), which shifts the pill width horizontally and adds visual noise instead of using expressive shape and typography.
3. **Single-Tag Filter Limitation:** Users can only select one Tag at a time on the Relationship Tab filter bar; tapping a second Tag replaces the first rather than allowing users to view Contacts across multiple selected Tags simultaneously.

## Solution

Refine the Relationship Tab filter bar and `TonalTagPill` selection treatment so that:
1. **Identical Pill Geometry:** Replace the Material 3 `FilterChip` for `"All"` with a custom `Surface` pill sharing the exact padding (`horizontal = 10.dp, vertical = 5.dp`) and typography (`MaterialTheme.typography.labelSmall`) of `TonalTagPill`, ensuring identical height and vertical alignment across the entire filter bar.
2. **Checkmark-Free Squircle + Bold Selection State:** Remove the checkmark icon from selected pills. Instead, unselected pills render as stadium pills (`CircleShape`) with `FontWeight.Medium` / `SemiBold` text, and selected pills morph into a tactile squircle (`RoundedCornerShape(8.dp)`) with filled container color and `FontWeight.Bold` text. Apply this same shape and typography transition to both `"All"` and all `TonalTagPill` instances.
3. **Multi-Tag Union (OR) Filtering:** Upgrade the Relationship Tab filter state from a single optional tag to a set of selected tags (`selectedTags: Set<String>`). Tapping a tag toggles its membership in `selectedTags` and filters the Contact list to display any Contact matching **any** of the active tags (Union / OR logic). Tapping `"All"` (or deselecting all active tags) clears the selection and shows all Contacts.

## User Stories

1. As a user browsing the Relationship Tab, I want the `"All"` filter pill to have the exact same height, padding, and text size as the adjacent Tonal Tag Pills, so that the filter bar looks balanced and uniform.
2. As a user selecting a Tag on the filter bar, I want the pill to become bold and morph from a stadium pill into a squircle without displaying a checkmark icon, so that selection feels tactile, minimal, and free of layout shifts.
3. As a user viewing the `"All"` filter pill when no specific Tags are active, I want `"All"` to display in the selected squircle + bold state, and morph into an unselected stadium pill when one or more Tags are selected.
4. As a user organizing Contacts across multiple contexts, I want to tap multiple Tags in the Relationship Tab filter bar simultaneously, so that I can view all Contacts belonging to any of the selected Tags (Union / OR filtering).
5. As a user with multiple Tags selected, I want to tap an already-selected Tag to deselect it while keeping my other selected Tags active, so that I can refine my filter set incrementally.
6. As a user who deselects the last active Tag in the filter bar, I want the filter bar to automatically return to the `"All"` state, so that I am never left with an empty selection state that hides all Contacts.
7. As a user with multiple Tags selected, I want to tap the `"All"` pill to immediately clear all selected Tags and restore the full Contact list, so that resetting my filter takes a single tap.
8. As a user combining text search with multi-tag selection, I want the search query to filter within the union of selected Tags, so that search and multi-tag filters compose predictably.
9. As a user encountering the empty state when no Contacts match my active search and selected Tags, I want the "Clear Filters" action to reset both the search query and all selected Tags.
10. As a user selecting Tags in other flows (such as Contact Edit or Quick Capture), I want `TonalTagPill` to consistently use the checkmark-free bold squircle selection style so that tag selection looks cohesive across the entire app.

## Implementation Decisions

- **Universal Selected vs. Unselected Pill Geometry (`TonalTagPill` & `"All"` Filter Pill):**
  - Unselected state (`isSelected = false`): `CircleShape` (stadium pill), soft tinted background (`alpha = 0.16f` for tag pills, `surfaceContainerHigh` or tonal surface for `"All"`), and `FontWeight.Medium`.
  - Selected state (`isSelected = true`): Squircle shape (`RoundedCornerShape(8.dp)`), solid high-contrast container fill (tag color for tag pills, `primaryContainer` / `primary` for `"All"`), contrasting label color, and `FontWeight.Bold`.
  - Remove the `Icons.Rounded.Check` icon from `TonalTagPill` when `isSelected = true`.
  - Both `"All"` and `TonalTagPill` share identical internal padding (`horizontal = 10.dp, vertical = 5.dp`) and text style (`MaterialTheme.typography.labelSmall`).
- **Multi-Tag State Contract (`ContactsUiState` & `ContactsUiEvent`):**
  - Introduce `selectedTags: Set<String> = emptySet()` on `ContactsUiState` as the canonical multi-select filter state, while preserving a computed or backward-compatible `selectedTag: String?` property (`selectedTags.firstOrNull()`) so existing single-tag callers and tests remain compatible where needed.
  - When `ContactsUiEvent.OnSelectTag(tagName)` is dispatched:
    - If `tagName == null` (e.g., tapping `"All"`), clear `selectedTags` to `emptySet()`.
    - If `tagName` is non-null, normalize it (strip `#`, trim, lowercase) and toggle its presence in `selectedTags` (add if absent, remove if present).
- **Union (OR) Filtering Reducer (`ContactsViewModel.computeState`):**
  - When `selectedTags` is empty, `matchesTag` is `true` for all Contacts.
  - When `selectedTags` is non-empty, a Contact matches if it carries **any** Tag in `selectedTags` (case-insensitive match on `contact.tags` or `contact.contextSubtitle`).
  - Intersection (AND) is maintained between the text `searchQuery` and the `selectedTags` union filter (`matchesQuery && matchesTag`).

## Testing Decisions

- **What makes a good test:** Tests should assert observable state transitions on `ContactsUiState` and visual/semantic properties at the Composable boundary, without coupling to private internal helper methods.
- **Primary Testing Seam — `ContactsViewModel` & `ContactsUiState` Reducer:**
  - Verify toggling a single tag adds it to `selectedTags` and filters `contacts`.
  - Verify selecting a second tag retains the first tag in `selectedTags` and returns the union (OR) of Contacts matching either tag.
  - Verify tapping an already-selected tag removes only that tag from `selectedTags`.
  - Verify tapping `"All"` (`OnSelectTag(null)`) or `OnClearFilters` clears all `selectedTags` back to `emptySet()`.
- **Secondary Verification Seam — `TonalTagPill` & `ContactsFilterBar` UI:**
  - Verify `TonalTagPill` renders without `Icons.Rounded.Check` when `isSelected = true`, uses `RoundedCornerShape(8.dp)` and `FontWeight.Bold` when selected, and matches the `"All"` pill's exact padding and typography.
- **Prior Art:** `ContactsViewModelTest.kt` for UDF state reducer and filter verification.

## Out of Scope

- Changing the tag color palette or tag persistence schema in Room.
- Altering how tags are assigned to Contacts in the database.
- Adding boolean AND/OR toggle switches to the UI.

## Further Notes

- Respects `GLOSSARY.md` definitions for **Relationship Tab**, **Contact**, **Tag**, and **Tonal Tag Pill**.


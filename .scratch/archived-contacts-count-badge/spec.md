# Archived Contacts Count Badge

Status: ready-for-agent

## Problem Statement

On the Archived Contacts Screen, the top bar count badge currently displays `"Total: 0"` (or `"Total: <count>"`) beside the screen title. Including the `"Total:"` label adds unnecessary visual clutter to a compact pill badge whose meaning is already clear from the screen header (`Archived <Relationship Terminology>`).

## Solution

Remove the `"Total: "` prefix from the count badge in the Archived Contacts Screen top bar so that the badge displays only the numeric count (e.g., `"0"`, `"3"`).

## User Stories

1. As a user viewing the Archived Contacts Screen with no archived Contacts, I want the top bar count badge to display `"0"` instead of `"Total: 0"`, so that the header remains clean and scannable.
2. As a user viewing the Archived Contacts Screen with archived Contacts, I want the top bar count badge to display the plain numeric count of archived Contacts, so that I can see how many Contacts are archived at a glance without redundant text.
3. As a user who triggers a Restore Action on an archived Contact, I want the numeric count badge in the top bar to decrement immediately without any `"Total:"` prefix reappearing, so that the header stays consistent during live updates.
4. As a user who archives a Contact and navigates to the Archived Contacts Screen, I want the numeric count badge to reflect the updated count cleanly, so that the archived directory state is always accurate.
5. As a user who switches Relationship Terminology (People, Friends, Connections, or Contacts), I want the top bar title to adapt to my chosen Relationship Terminology while the count badge continues to show only the plain number, so that the header reads naturally regardless of terminology preference.
6. As a user navigating to the Archived Contacts Screen on narrow or compact viewports, I want the count badge to take minimal horizontal width, so that the `"Archived <Relationship Terminology>"` title has maximum room and avoids awkward wrapping or truncation.
7. As a user with accessibility needs using a screen reader, I want the count badge text to remain legible and properly styled within its tonal pill container, so that the archived Contact count is still clearly perceived.
8. As a user who restores all archived Contacts until the list is empty, I want the count badge to transition smoothly to `"0"` alongside the empty state view, so that the empty state and header count agree.
9. As a user who returns from the Archived Contacts Screen back to the Relationship Tab, I want existing navigation and Restore Action behavior to remain completely unchanged, so that removing `"Total: "` introduces no regressions.

## Implementation Decisions

- Modify the Archived Contacts Screen top bar component so its count pill renders only the string representation of the archived Contacts count rather than prefixing it with `"Total: "`.
- Preserve the existing pill container shape, typography, color tokens, padding, and alignment in the top bar.
- Keep the top bar title driven by the active Relationship Terminology and keep all Restore Action, exit animation, and navigation behaviors unchanged.

## Testing Decisions

- **Testing Seam:** Screen-level Compose UI test rendering the Archived Contacts Screen directly.
- **What Makes a Good Test:** Tests should assert external, user-visible behavior through Compose semantics—verifying that the numeric count (e.g., `"0"` when empty, `"2"` when two archived Contacts are provided) is displayed and that `"Total:"` is absent—rather than inspecting private composable internals or implementation details.
- **Modules Tested:** The Archived Contacts Screen UI component in the contacts UI module.
- **Prior Art:** Existing Compose UI test dependencies (`androidx.compose.ui.test.junit4`) configured in the Android test source set, alongside the existing `ArchivedContactsContractTest` which covers route and ViewModel-level archived Contact contracts.

## Out of Scope

- Changing the count badge layout, shape, colors, or visibility when the count is zero.
- Modifying any other screens or settings export summaries that intentionally use `"Total Contacts:"` or similar phrasing.
- Adding new ViewModel state fields or modifying the domain or data layers.

## Further Notes

- No ADR or `GLOSSARY.md` conflicts: the change strictly affects presentation copy on the Archived Contacts Screen and preserves all domain terminology.

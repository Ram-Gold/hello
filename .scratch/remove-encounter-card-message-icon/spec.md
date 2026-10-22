Status: ready-for-agent

# Specification: Remove Message Action Icon from Recent Encounter Cards

## Problem Statement

In the Recent Encounters section on the Home screen, each encounter card contains a nested message icon button (`ChatBubbleOutline`) aligned to the far right. This button triggers the same click handler as the card itself, creating a redundant nested tap target. Furthermore, Hello is a strictly local-first personal CRM with zero messaging integrations or background SMS dispatch, making a messaging icon misleading to users. The extra icon also consumes valuable horizontal width, crowding contact names, encounter timestamps, and Circle badges on narrow mobile displays.

## Solution

Remove the message icon button (`ChatBubbleOutline`) from the recent encounter card component. The entire card remains a clean, cohesive interactive surface that routes users to the contact's details when tapped. The removed icon frees up horizontal space, allowing the contact's name, encounter metadata, context subtitle, and Circle badges to layout gracefully without awkward truncation.

## User Stories

1. As a user viewing my recent encounters list, I want each card to present clean contact information without an unnecessary message bubble icon, so that I am not misled into thinking the app sends instant messages.
2. As a user scanning recent encounters, I want more horizontal space for contact names and encounter recency tags, so that text is not prematurely truncated with ellipses.
3. As a user reviewing multi-word Circle badges on an encounter card, I want badges to flow naturally without colliding with a trailing action button, so that tags remain legible.
4. As a user tapping an encounter card, I want the entire card surface to be a single, reliable tap target, so that I can open the contact profile without worrying about hitting an overlapping nested button.
5. As a user navigating with accessibility talkback or screen readers, I want encounter cards to announce as a single cohesive entity rather than announcing an ambiguous secondary "Message [Name]" button, so that accessibility traversal is straightforward.
6. As a user holding the device with one hand, I want large, forgiving tap areas across the whole card rather than tiny 36dp nested targets, so that interaction ergonomics are improved.
7. As a user in Dark Mode or high-contrast settings, I want a minimalist card aesthetic without extra low-contrast icon badges, so that the visual hierarchy remains warm and uncluttered.
8. As a developer maintaining the UI components, I want unused icon imports and nested click callbacks eliminated from the card component, so that the composable tree remains lean.
9. As a developer verifying user events, I want the card click contract to unambiguously trigger contact navigation through the existing view model seam, so that event flows remain clean.
10. As a user on a low-end device, I want fewer composable nodes per list item in the scrollable home column, so that scroll frame drops are minimized.

## Implementation Decisions

- **Remove Trailing Action Node**: Eliminate the trailing `IconButton` containing the `ChatBubbleOutline` icon from the encounter card layout row.
- **Card Tap Target Unity**: Maintain the outer card click listener (`onClick`) as the single entry point for opening contact details and encounter history.
- **Horizontal Flow Expansion**: Allow the central metadata column (Contact Name, Recency bullet, Context subtitle, and Circle badges) to utilize the full available width of the card padding without trailing constraints.
- **Clean Imports**: Remove unused vector icon imports (`ChatBubbleOutline`) from the UI component file.
- **Zero Contract Breakage**: Preserve existing ViewModel and UI contract signatures (`OnContactClick`), ensuring downstream event handling remains completely unaffected.

## Testing Decisions

- **What makes a good test**: Tests verify external behavior at the primary view model seam (`HomeViewModel.uiState` and `HomeViewModel.onEvent`), verifying that tapping a recent encounter card dispatches `OnContactClick(contactId)` and updates state properly without side effects.
- **Modules under test**: `HomeViewModel` and `RecentEncountersSection`.
- **Prior art**: Follows existing event verification patterns in `HomeViewModelTest.kt`.

## Out of Scope

- Adding custom swipe actions to encounter cards.
- Modifying the contact summary card in the People/Contacts tab.
- Introducing deep links to external system messaging apps.

## Further Notes

- This change reinforces Hello's core identity as an offline, local-first relationship keeper focused on reflection and encounter logging rather than a direct communication client.

# Feature Specification: Archived Contacts UI Polish — Streamlined Counter & Restore Icon Action

Status: ready-for-agent  
Date: 2026-11-08  
Topic: UI / Archived Contacts Screen / Visual Polish / Tactile Actions  

---

## Problem Statement

The full-screen Archived Contacts destination currently presents two visual friction points that detract from Hello's tactile, minimal aesthetic:
1. **Redundant Counter Label:** The top app bar displays `"Total: $count"` inside a pill badge. The text prefix `"Total:"` is verbose, adds unnecessary visual weight, and crowds the screen title on narrower mobile displays.
2. **Clunky Text Action Button:** Each archived contact card features a text button with `"Restore"`. Text buttons on list rows consume excess horizontal real estate, crowd the contact's name and subtitle, and feel less tactile than direct icon affordances found throughout the rest of the application.

Users want a refined, modern presentation where the counter displays a clean numeric badge without the word "Total", and the text "Restore" is replaced by a clean, thumb-friendly restore icon button (a curved arrow pointing back).

---

## Solution

1. **Streamlined Top Bar Count Badge:**
   - Remove the `"Total: "` string prefix from the top app bar badge, displaying only the numeric count (e.g. `"3"`, `"0"`).
   - Maintain the warm `surfaceContainerHigh` tonal pill container with compact padding (`horizontal = 8.dp, vertical = 4.dp`) for visual balance.
   - Attach TalkBack accessibility descriptions (e.g. `"$count archived ${terminology.pluralTitle.lowercase()}"`) to ensure full context for screen readers.

2. **Tactile Restore Icon Button in Cards:**
   - Replace the `TextButton` labeled `"Restore"` on each `ArchivedContactCard` with a 40dp circular tonal icon button.
   - Utilize `Icons.AutoMirrored.Rounded.Undo` (a sleek curved arrow pointing back), visually conveying the restoration of a relationship back to the active directory.
   - Use Hello's warm tonal styling: `surfaceContainerHigh` or soft primary-tinted container with terracotta primary icon tint (`MaterialTheme.colorScheme.primary`).
   - Include an explicit `contentDescription = "Restore contact"` for accessibility.
   - Retain the reactive instant restoration flow: tapping the button restores the contact in SQLite, removes the card from the archived list, and triggers the elevated Toast Pill with an Undo action.

---

## User Stories

1. As a user viewing my archived contacts, I want the top bar counter badge to display just the number of contacts (e.g., "4"), so that the header looks minimal and uncrowded.
2. As a user viewing an empty archive, I want the badge to show "0", so that I immediately know there are no archived items.
3. As a user on a narrow phone screen, I want the top bar badge to be compact, so that long relationship titles (e.g., "Archived Connections") never truncate.
4. As a user scanning my archived contacts list, I want to see a clear curved-arrow restore icon on the right side of each card, so that I immediately understand I can return that contact to my active directory.
5. As a user holding my phone with one hand, I want the restore icon button to have a generous 40dp touch target with a subtle circular surface, so that I can easily tap it with my thumb.
6. As a user tapping the restore icon button, I want the contact card to animate out cleanly, so that I receive immediate visual confirmation of the restore action.
7. As a user who restored a contact, I want the top bar numeric counter to instantly decrement (e.g. from "4" to "3"), so that the counter reflects real-time database state.
8. As a user who accidentally tapped the restore icon button, I want the standard confirmation Toast Pill ("Contact restored") with an "Undo" action, so that I can immediately return the contact to the archive if tapped by mistake.
9. As a user using dark theme, I want the restore icon and top bar badge to maintain high tonal contrast, so that both are clearly visible in dim environments.
10. As a user using light theme, I want the restore button and count badge to use Hello's warm terracotta and beige palette, so that they feel tactile and harmonious.
11. As a user with contacts that have long names and lengthy context subtitles, I want the compact restore icon button to free up horizontal space, so that contact details don't unnecessarily truncate.
12. As a TalkBack / screen reader user, I want the restore button to announce "Restore contact", so that I can navigate and restore contacts non-visually with confidence.
13. As a TalkBack / screen reader user, I want the top bar count badge to announce the full count context without visual clutter, so that accessibility is never compromised.

---

## Implementation Decisions

### 1. Presentation Layer (`ui/contacts/ArchivedContactsScreen.kt`)
- **Top Bar Badge (`ArchivedContactsTopBar`):**
  - Change text rendering from `"Total: $count"` to `"$count"`.
  - Adjust padding to `PaddingValues(horizontal = 8.dp, vertical = 4.dp)` inside `RoundedCornerShape(10.dp)`.
  - Provide semantics description `"$count archived ${terminology.pluralTitle.lowercase()}"`.
- **Card Action (`ArchivedContactCard`):**
  - Replace `TextButton` with `IconButton` or `FilledTonalIconButton` with `shape = CircleShape` and `size = 40.dp`.
  - Icon: `Icons.AutoMirrored.Rounded.Undo`, `size = 20.dp`, `tint = MaterialTheme.colorScheme.primary`.
  - Container color: `MaterialTheme.colorScheme.surfaceContainerHigh` or `primaryContainer.copy(alpha = 0.4f)`.
  - Content description: `"Restore ${contact.name}"` or `"Restore contact"`.

### 2. Domain & Repository Decoupling
- No changes to `ContactRepository` or database schemas required; uses the existing `archiveContact(contactId, archived = false)` reactive flow.

---

## Testing Decisions

### Good Test Principles
- Test behavior and state contracts, verifying that the top bar output excludes `"Total"` and that the restore action continues to properly invoke restoration and list exclusion.

### Target Test Seams
- **Screen & Top Bar Contract Seam (`ArchivedContactsContractTest`):**
  - Verify top bar counter format produces numeric string without `"Total"`.
  - Verify `onRestore` action binding removes contact from archived list and reinserts into active directory.
  - Verify accessibility contract values.

### Prior Art
- `ArchivedContactsContractTest`: Existing route and restore flow contract tests.

---

## Out of Scope

- **Batch Multi-Select on Archived Screen:** Mass restoration from within the archived screen is out of scope.
- **Permanent Deletion from Archived Screen:** Direct deletion of archived contacts remains handled via standard profile or selection flows.

---

## Further Notes

- Directly refines **ADR-0028** and **ADR-0029** into a tighter, more visual Material 3 Expressive experience.


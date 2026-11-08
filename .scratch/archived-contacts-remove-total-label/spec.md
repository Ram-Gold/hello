# Feature Specification: Streamline Archived Contacts Top Bar Count Badge

Status: ready-for-agent  
Date: 2026-11-08  
Topic: UI / Archived Contacts Screen / Top Bar / Visual Polish  

---

## Problem Statement

On the full-screen Archived Contacts destination, the top app bar currently displays a pill badge containing `"Total: $count"` (e.g. `"Total: 4"`). In modern mobile design systems and Material 3 Expressive guidelines, labeling a single number badge with the redundant prefix word "Total:" creates visual clutter, adds unnecessary cognitive load, and wastes horizontal screen width on smaller devices.

Users want a cleaner, more minimal, and tactile presentation where the count badge simply displays the number of archived relationships (e.g. `"4"`) without the verbose `"Total:"` label.

---

## Solution

1. **Clean Numeric Count Badge:**
   - Update the top bar count badge in the Archived Contacts view to display exclusively the count number (e.g., `"0"`, `"3"`, `"12"`), eliminating the redundant `"Total: "` text prefix.
2. **Compact & Balanced Tonal Surface:**
   - Retain the warm `surfaceContainerHigh` tonal pill capsule with subtle padding and rounded geometry (`RoundedCornerShape(10.dp)` or pill shape), ensuring the count badge remains visually balanced and readable.
3. **Accessibility & Screen Reader Clarity:**
   - Ensure screen readers (TalkBack) continue to provide full context (e.g., "$count archived contacts") via content description or semantic properties, while the visual UI stays minimal and clutter-free.

---

## User Stories

1. As a user viewing my archived contacts, I want to see a clean numeric count badge (e.g., "5") in the top bar, so that the header looks sleek and unencumbered by redundant words.
2. As a user viewing a zero-archived contacts state, I want to see "0" in the top bar badge, so that I have instant confirmation of the empty count.
3. As a user on a narrow phone screen, I want the badge to take up minimal horizontal space, so that long terminology titles (e.g., "Archived Connections") have maximum room without truncating.
4. As a user restoring an archived contact, I want the numeric badge to immediately decrement (e.g., from "5" to "4"), so that I receive immediate visual feedback of the count update.
5. As a user who mass archives contacts, I want the count badge to reflect the updated number of archived contacts upon opening the screen, so that the count is always accurate.
6. As a user using dark theme, I want the numeric badge to maintain proper tonal contrast against the dark background, so that the number remains effortless to read.
7. As a user using light theme, I want the badge to utilize Hello's warm earthy container styling, so that it harmonizes with the rest of the application's Material 3 aesthetic.
8. As a TalkBack / screen reader user, I want the accessibility label to announce the count meaningfully (e.g. "5 archived"), so that removing the visual word "Total" does not reduce comprehension for vision-impaired users.
9. As a user navigating between the main Relationship Tab and the Archived Contacts Screen, I want consistent badge styling, so that the visual design language feels cohesive throughout the app.
10. As a user with single-digit or double-digit archived contacts, I want the pill badge padding to remain proportionate, so that single numbers (e.g., "3") and larger numbers (e.g., "42") both look centered and intentional.

---

## Implementation Decisions

### 1. Top Bar Presentation & Formatting
- **Badge Content:** Change the rendered text inside the top bar badge container from `"Total: $count"` to `"$count"`.
- **Badge Capsule Sizing:** Adjust horizontal and vertical padding (e.g. `horizontal = 8.dp, vertical = 4.dp`) so that the compact number badge looks like an intentional circular or pill-shaped counter without excessive empty space.
- **Typography:** Retain semi-bold label styling (`MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)`) with `color = MaterialTheme.colorScheme.onSurfaceVariant`.
- **Accessibility:** Add a semantic content description or semantics block to the count badge (e.g. `"$count archived ${terminology.pluralTitle.lowercase()}"`) to preserve accessible navigation.

### 2. Architectural Consistency
- Keep the title and back button layout unchanged.
- Retain the reactive binding to the contact list count (`archivedContacts.size`).

---

## Testing Decisions

### Good Test Principles
- Test the output contract and presentation state, avoiding brittle UI node tree coupling where possible.
- Assert that the formatted badge text contains strictly the count string and does not contain the substring `"Total:"` or `"Total"`.

### Target Test Seam
- **Primary Seam — Screen & Top Bar Contract (`ArchivedContactsContractTest`):**
  - Verify that the count formatter produces the clean numeric string `"$count"` without any `"Total:"` prefix.
  - Verify that decrementing or incrementing the archived list size updates the count contract reactively.
  - Verify accessibility string representation preserves clarity.

### Prior Art
- `ArchivedContactsContractTest`: Existing contract tests for route name, dock visibility, and restore flows.

---

## Out of Scope

- **Changing the Active Contacts List Header:** Modifying alphabetical headers or group counters on the main Relationship Tab is out of scope.
- **Hiding the Badge on Empty List:** The badge will remain visible showing `"0"` when the list is empty (matching current behavior).
- **Batch Selection on Archived Contacts:** Introducing multi-select within the archived screen remains a separate feature.

---

## Further Notes

- Directly refines **ADR-0029** (Archived Contacts Full-Screen Destination) by removing an overly verbose legacy label in favor of clean Material 3 Expressive counters.


# 0025. Favorite Contacts and Edit Profile Refinements

Date: 2026-11-08
Status: Accepted

## Context

Users maintain inner-circle relationships they revisit frequently across both the Home Tab and the Relationship Tab (People screen), requiring a frictionless way to star and access favorite contacts. Simultaneously, the Contact Edit screen contained enterprise-heavy terminology ("NICKNAMES & ALIASES" and "RELATIONSHIP LIFECYCLE") and a redundant manual "Add" button next to the nickname input field.

## Decision

1. **Edit Profile UI Refinements (`PersonEditScreen`):**
   - Rename `"NICKNAMES & ALIASES"` to `"NICKNAMES"` across presentation surfaces while preserving the underlying `aliases` column/JSON schema for 100% backward compatibility.
   - Remove the explicit `"Add"` button next to the nickname text field; nicknames are committed directly via the keyboard Enter / Done action (`ImeAction.Done` or newline keystroke).
   - Rename the bottom `"RELATIONSHIP LIFECYCLE"` card header to `"MANAGE CONTACT"`.

2. **Contact Favoriting & Activity Timestamp (`Contact`, `ContactEntity`, `ContactSummary`):**
   - Add `isFavorite: Boolean = false` and `updatedAt: Instant? = null` to `Contact`, `ContactEntity` (Room schema v5), and `ContactSummary`, plus `isFavorite` export portability.
   - Add a star toggle `IconButton` beside the Edit button in `PersonDetailScreen`'s top bar (`Icons.Rounded.Star` when favorited, `Icons.Rounded.StarOutline` when not favorited).
   - Update `updatedAt` whenever a contact is created, edited, favorited, or has an encounter logged, and order Favorite Contacts by `max(updatedAt, lastMetAt, firstMetAt)` descending ("Recently met, edited, or updated").

3. **Dual-Orientation Favorites Surfaces (`HomeScreen` & `ContactsScreen`):**
   - **Visibility Rule:** Hide the Favorites section completely when 0 contacts are favorited; display automatically when at least 1 active contact is favorited. Keep favorited contacts in the main alphabetical list and Recent Encounters as well.
   - **Home Tab (`HomeFavoritesSection`):** Positioned below `UpcomingFeedSection` and above `RecentEncountersSection`. Renders a vertical connected list of compact cards displaying `<Contact Avatar> | <Name>`.
   - **Relationship Tab / People View (`PeopleFavoritesRow`):** Positioned inside the scrollable `LazyColumn` right below the `ContactsFilterBar` and above the alphabetical sticky headers (hidden when searching, tag-filtering, or in selection mode). Renders a horizontal `LazyRow` where each item stacks `<Contact Avatar>` vertically above `<Name>`.

## Consequences

- **Positive:** Gives one-tap access to inner-circle contacts with layout ergonomics tailored to each tab (vertical connected cards on Home, compact horizontal avatar strip on People), while decluttering the Edit Contact form.
- **Compatibility:** Existing Room tables, JSON exports, and Markdown frontmatter remain backward compatible (`isFavorite` defaults to `false`).


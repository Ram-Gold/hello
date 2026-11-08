# 0028: Archived Contacts Full-Screen View and Restore Flow

**Date:** 2026-11-08  
**Status:** Accepted  
**Context:**  
Contacts can be archived to pause check-in suggestions and keep the active relationship directory uncluttered. Previously, archived contacts were not visible or discoverable on the Relationship Tab, requiring users to either undo immediately upon archiving or lack a clear path to review and restore archived relationships. Users required a tactile, discoverable mechanism to view archived contacts and selectively restore them without cluttering the primary contacts list, preferring a dedicated full-screen destination rather than a confined modal bottom sheet.

**Decision:**
1. **Strict Active Isolation:**
   - The primary contacts list on the Relationship Tab (`ContactsScreen`) strictly filters out archived contacts (`isArchived == false`).
   - Archived contacts are cleanly separated and surfaced in a dedicated full-screen destination.
2. **Hidden Pull-to-Action with Rubber-Banding Overscroll:**
   - In place of persistent chrome, the archive access is completely hidden during idle state for zero visual clutter.
   - Users access the Archived Contacts screen by pulling down at the top boundary of the contacts directory (pull-to-action pattern).
   - Utilizes diminishing-returns rubber-banding resistance physics (`PullToActionNestedScrollConnection`) preventing abrupt stops.
   - A scroll-driven indicator (`PullToActionArchiveHeader`) scales and rotates `Icons.Rounded.Archive`, dynamically transitioning between `"Pull for archive"` and `"Release for archive"` at an activation threshold (~72dp).
   - Releasing past the threshold delivers a haptic tap, snaps back with spring motion, and navigates directly to `ArchivedContactsRoute`.
3. **Dedicated Full-Screen Destination (`ArchivedContactsScreen`):**
   - Implemented as a full screen destination in `MainAppNavHost` (`contacts/archived`).
   - Features a Material 3 Expressive top bar with back button, terminology title (`Archived ${terminology.pluralTitle}`), and count badge.
   - The floating bottom dock automatically hides on this destination to maximize vertical viewing area.
   - Native system back gesture and app bar back arrow smoothly return to the primary contacts directory.
4. **Minimal Archived Contact Card & Multi-Restore Flow:**
   - Each card displays the Contact Avatar, full name, and context subtitle alongside a clean, direct terracotta `"Restore"` text button.
   - Tapping `"Restore"` immediately calls `repository.archiveContact(contactId, archived = false)`.
   - The restored card disappears from the archived list, reactively appears in the active contacts directory, and triggers a confirmation Toast Pill (`"Contact restored"`) with instant in-memory `"Undo"` capability.
   - Users can restore multiple contacts in one session.
5. **Empty State:**
   - If opened with zero archived contacts or when all are restored, displays a clean minimal state with an `Icons.Rounded.Archive` icon, `"No archived contacts"`, and `"Cards you archive will appear here safely until restored."`.

**Consequences:**
- **Positive:** Gives users a full-screen, unconfined experience to review and recover archived contacts; keeps the active directory focused and minimal with zero idle visual clutter; iOS/Telegram-style rubber-banding motion provides pleasant tactile delight; upholds zero-latency local-first Room reactivity.
- **Negative:** Pull-to-action requires initial user discovery, mitigated by clear animated icon and label affordances during gesture.


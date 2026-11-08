# 0028: Archived Contacts Pull-Up Sheet and Restore Flow

**Date:** 2026-11-08  
**Status:** Superseded by ADR-0029  
**Context:**  
Contacts can be archived to pause check-in suggestions and keep the active relationship directory uncluttered. Previously, archived contacts were not visible or discoverable on the Relationship Tab, requiring users to either undo immediately upon archiving or lack a clear path to review and restore archived relationships. Users required a tactile, discoverable mechanism to view archived contacts and selectively restore them without cluttering the primary contacts list.

**Decision:**
1. **Strict Active Isolation:**
   - The primary contacts list on the Relationship Tab (`ContactsScreen`) strictly filters out archived contacts (`isArchived == false`).
   - Archived contacts are cleanly separated and surfaced in a dedicated pull-up surface.
2. **Hidden Pull-to-Action with Rubber-Banding Overscroll:**
   - In place of a persistent bottom footer, the archive access is completely hidden during idle state for zero visual clutter.
   - Users access the Archived Contacts Sheet by pulling down at the top boundary of the contacts directory (pull-to-action pattern).
   - Utilizes diminishing-returns rubber-banding resistance physics (`PullToActionNestedScrollConnection`) preventing abrupt stops.
   - A scroll-driven indicator (`PullToActionArchiveHeader`) scales and rotates `Icons.Rounded.Archive`, dynamically transitioning between `"Pull for archive"` and `"Release for archive"` at an activation threshold (~72dp).
   - Releasing past the threshold delivers a haptic tap, snaps back with spring motion, and opens the Archived Contacts Sheet.
3. **Modal Bottom Sheet Surface:**
   - Rendered as an in-hierarchy modal bottom sheet with a grab handle, matching the tactile warm aesthetic of Hello (`RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`).
   - Dismissible by dragging down, pressing the back key, or tapping the scrim.
4. **Minimal Archived Contact Card & Multi-Restore Flow:**
   - Each row displays the Contact Avatar, full name, and context subtitle alongside a clean, direct terracotta `"Restore"` text button.
   - Tapping `"Restore"` immediately calls `repository.archiveContact(contactId, archived = false)`.
   - The restored card disappears from the sheet, reactively appears in the active contacts list, and triggers a confirmation Toast Pill (`"Contact restored"`).
   - The sheet remains open so users can restore multiple contacts in one session.
5. **Empty State:**
   - If opened with zero archived contacts, displays a clean minimal state with an `Icons.Rounded.Archive` icon, `"No archived contacts"`, and `"Contacts you archive will appear here"`.

**Consequences:**
- **Positive:** Gives users a tactile, intuitive way to review and recover archived contacts; keeps the active directory focused and minimal with zero idle visual clutter; iOS/Telegram-style rubber-banding motion provides pleasant tactile delight; upholds zero-latency local-first Room reactivity.
- **Negative:** Pull-to-action requires initial user discovery, mitigated by clear animated icon and label affordances during gesture.

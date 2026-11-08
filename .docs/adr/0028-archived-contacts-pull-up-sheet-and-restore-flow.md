# 0028: Archived Contacts Pull-Up Sheet and Restore Flow

**Date:** 2026-11-08  
**Status:** Accepted  
**Context:**  
Contacts can be archived to pause check-in suggestions and keep the active relationship directory uncluttered. Previously, archived contacts were not visible or discoverable on the Relationship Tab, requiring users to either undo immediately upon archiving or lack a clear path to review and restore archived relationships. Users required a tactile, discoverable mechanism to view archived contacts and selectively restore them without cluttering the primary contacts list.

**Decision:**
1. **Strict Active Isolation:**
   - The primary contacts list on the Relationship Tab (`ContactsScreen`) strictly filters out archived contacts (`isArchived == false`).
   - Archived contacts are cleanly separated and surfaced in a dedicated pull-up surface.
2. **Pull-Up Affordance & Overscroll Trigger:**
   - At the bottom of the contacts list, a subtle footer handle displays `"Pull up for archived (N)"` with an upward chevron icon.
   - Users can either drag up (overscroll gesture) or tap the footer handle to slide open the Archived Contacts Sheet.
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
- **Positive:** Gives users a tactile, intuitive way to review and recover archived contacts; keeps the active directory focused and minimal; upholds zero-latency local-first Room reactivity.
- **Negative:** Adds a gesture/tap footer trigger to the bottom of the contacts list.


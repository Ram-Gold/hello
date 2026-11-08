# Spec: Archived Contacts Dedicated Full Screen

## Overview
Elevate the archived contacts view from an ephemeral bottom sheet (`ArchivedContactsSheet`) to a dedicated, full-screen destination (`ArchivedContactsScreen`) accessible via the existing top rubber-band pull-to-action gesture.

## Key Changes
1. **Navigation Route (`ArchivedContactsRoute`):**
   - New route `contacts/archived` in `AppNavigation.kt`.
   - Registered in `MainAppNavHost` with standard slide/fade shared-axis transitions (`SettingsMotionConfig`).
   - Automatically hides floating bottom dock when active (standard non-root destination behavior).

2. **Full Screen UI (`ArchivedContactsScreen`):**
   - Material 3 Expressive top bar matching app aesthetic (`HelloTheme`).
   - Back arrow button with system BackHandler integration.
   - Title: `"Archived ${terminology.pluralTitle}"` with count pill badge (`Total: N`).
   - Full-height scrollable list of archived contacts with avatar, full name, context subtitle, and terracotta `"Restore"` action.
   - Clean empty state when all contacts are restored or none are archived.

3. **Pull-to-Action Trigger:**
   - In `ContactsScreen`, releasing pull-to-action past the threshold navigates directly to `ArchivedContactsRoute`.
   - Replaces `ContactsUiState.isArchivedSheetOpen` modal bottom sheet.

4. **Restoration & Undo Pipeline:**
   - Restoring a contact updates Room SQLite (`isArchived = false`), immediately reflected via reactive Flow.
   - Surfaces semantic Toast Pill with `"Undo"` action.
   - If user taps Undo, the contact is seamlessly returned to archive state.


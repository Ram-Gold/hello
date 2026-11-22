# 0037. Profile Reminders and Reliable Local Notification Delivery

Date: 2026-11-22
Status: Accepted

## Context

`FollowUp` reminders could previously only be created during initial Contact capture, could not be viewed, created, edited, completed, or deleted from a Contact's profile (`PersonDetailScreen`), and suffered from Android 13–16 delivery gaps (`POST_NOTIFICATIONS` never requested, no reboot/clock-change alarm rescheduling, disconnected `SettingsPreferencesScreen` delivery hour, stale `PendingIntent` PII snapshots, missing lock-screen redaction, and legacy system notification icons).

## Decision

1. **Unified Living Timeline Placement & Split Bottom-Docked Action Bar (amended for RAM-60):**
   - Present `FollowUp` entities in the UI as **Reminders** (`GLOSSARY.md`).
   - Support multiple one-time Reminders per Contact; recurring reminders remain explicitly out of scope.
   - Surface active and overdue Reminders inside `PersonDetailScreen`'s `LivingTimeline` as dashed-spine Future Horizon nodes above a `TODAY` divider.
   - **Single Thumb-Level Creation Entry Point (`Profile Reminder Pill`):** Remove both the inline `+ Remind` expandable composer from `LivingTimeline` and the `TopAppBar` bell icon button. Instead, morph the bottom dock on `PersonDetailScreen` into a split `60.dp` stadium-pill action bar pairing the left companion `Profile Reminder Pill` (`Remind`) with the right primary `Profile Encounter Pill` (`+ Catching up?`).
   - **Catching-Up-Style Reminder Bottom Sheet:** Tapping `Remind` (or tapping an existing Reminder node on the timeline to edit) opens a focused bottom-sheet card modeled after `FlowBLogEncounterSheet` (`+ Catching up?`): top `X` close button, right circular `->` save button (plus an error-tinted circular Trash icon button in the header when editing an existing Reminder), Contact Name header, borderless pill-to-squircle `"When"` chips (`Tomorrow`, `In 3 days`, `Next week`, `Custom...`), a compact Time chip + Quick-note suggestion chips, and a borderless auto-focused `"Remind me to..."` text field.
   - Each timeline Reminder node continues to offer quick Done and Snooze actions; Done and Delete both surface a 7-second Undo `Toast Pill`.
   - The reason (`note`) field is optional and defaults to `"Follow up with <Contact Name>"` when left blank.

2. **Schema & Settings Delivery-Hour Sync (`MIGRATION_7_8`):**
   - Add `hasExplicitTime: Boolean = false` to `FollowUp` and `FollowUpEntity` (and fix `toEntity()`/`toDomain()` to preserve `doneAt`), bumping `HelloDatabase` from version `7` to `8` via `MIGRATION_7_8` (`ALTER TABLE follow_ups ADD COLUMN hasExplicitTime INTEGER NOT NULL DEFAULT 0`).
   - Date-only Reminders (`hasExplicitTime == false`) snap to `UserPreferences.notificationHour` / `notificationMinute` in the device timezone and automatically shift and reschedule when the user updates their preferred delivery hour in `SettingsPreferencesScreen`. Custom-time Reminders (`hasExplicitTime == true`) remain pinned to their chosen clock time.
   - Include `hasExplicitTime` and completed `FollowUp` records in `ExportBundleGenerator` / `ExportDataWorker`.

3. **Android 13–16 Local Delivery & Privacy Architecture:**
   - **No `USE_EXACT_ALARM`:** Prohibited by Google Play policy for personal CRMs. Default to zero-permission `AlarmManager.setAndAllowWhileIdle()`, upgrading automatically to `setExactAndAllowWhileIdle()` whenever `canScheduleExactAlarms()` is true, with an optional *"Exact reminder timing"* opt-in row in `SettingsPreferencesScreen` (no nag dialog during reminder creation).
   - **Just-in-time `POST_NOTIFICATIONS` explainer (Prime Directive 3):** Trigger an in-app explainer sheet only when scheduling a Reminder or adjusting notification preferences on Android 13+, handling never-asked, rationale re-ask, and permanently denied / channel-muted (`IMPORTANCE_NONE`) states via Settings deep-links, with graceful in-app degradation if denied.
   - **Post-unlock rescheduling only:** Register a manifest `BroadcastReceiver` for `ACTION_BOOT_COMPLETED` (`RECEIVE_BOOT_COMPLETED`), `ACTION_MY_PACKAGE_REPLACED`, `ACTION_TIME_SET`, `ACTION_TIMEZONE_CHANGED`, and `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, plus app-launch reconciliation (`ApplicationStartInfo.wasForceStopped()` on API 35+). Intentionally avoid Direct Boot (`LOCKED_BOOT_COMPLETED`) so contact names and notes remain strictly inside Credential-Encrypted Room storage.
   - **Fire-time Room verification & lock-screen redaction:** `FollowUpAlarmReceiver` looks up fresh `FollowUp` and `Contact` rows from Room via `goAsync()` instead of trusting snapshotted `PendingIntent` extras (suppressing completed, deleted, or archived contacts). Notifications use a dedicated 24dp monochrome alpha-only vector icon, `VISIBILITY_PRIVATE` with a sanitized public lock-screen fallback (`"Hello"` / `"You have a reminder due"`), inline `"Done"` and `"Snooze (3d)"` actions, and warm deep-linking via `MainActivity.onNewIntent`.

4. **Contact Archive & Completion Lifecycle:**
   - Archiving a Contact cancels its scheduled alarms and hides its Reminders from Home and Timeline without deleting them; restoring the Contact re-arms any still-pending future/overdue Reminders automatically.
   - Completing a Reminder marks `doneAt` in Room, cancels its alarm, and removes it cleanly from `LivingTimeline` (reversible via the 7-second Undo `Toast Pill`).

## Consequences

- **Positive:** Turns Reminders into a first-class, reliable local-first workflow directly on Contact profiles while upholding zero-backend, Play-policy, and lock-screen privacy invariants.
- **Migration:** Existing v7 SQLite databases migrate non-destructively to v8 via `MIGRATION_7_8` with `hasExplicitTime = 0` for legacy rows.

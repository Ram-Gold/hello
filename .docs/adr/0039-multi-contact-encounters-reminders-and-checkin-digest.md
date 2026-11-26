# 0039. Multi-Contact Encounters, Reminders, and Opt-In Check-in Digest

Date: 2026-11-23
Status: Accepted (Amends ADR-0036 and ADR-0037)

## Context

Previously, each `Encounter` and `FollowUp` could only link to a single `Contact`, forcing users to duplicate records when meeting multiple people together ("Met Again") or scheduling a shared follow-up Reminder. Meanwhile, users also requested an occasional, low-pressure way to receive a batched OS notification nudging them to check in with multiple quiet Contacts without reintroducing manual per-contact cadences (forbidden by ADR-0036).

## Decision

1. **Many-to-Many Canonical Storage (`PersonEncounterCrossRef` & `PersonFollowUpCrossRef`):**
   - Store one canonical `EncounterEntity` / `FollowUpEntity` row linked to `1..N` Contacts via `person_encounters` and `person_follow_ups` join tables (`HelloDatabase` v9, `MIGRATION_8_9`), keeping `contactId` populated with the primary Contact for backward compatibility.
   - Deleting a shared `Encounter` or `FollowUp` removes the canonical record everywhere (never partially unlinking unless the Contact itself is deleted), with a confirmation warning whenever `> 1` Contact is attached.
   - Export Bundle JSON (`encounters.json`, `followups.json`), Markdown (`(with ...)` heading context), and CSV (`participant_ids` column) preserve multi-contact associations losslessly.

2. **Multi-Contact Picker UX (`FlowBLogEncounterSheet` & `ReminderEditorSheet`):**
   - Preserve fast single-tap capture in "Met Again" (`FlowBLogEncounterSheet`); long-pressing a Contact row enters multi-select mode in-place.
   - Both Encounter and Reminder forms display a horizontal `ContactParticipantsStrip` (ADR-0038 borderless pills) where tapping `+ Add` or long-pressing a selected Contact chip opens `MultiContactPickerDialog`.

3. **Timeline & Profile Display:**
   - Global Living Timeline (`TimelineScreen`) consolidates multi-contact Encounters and FollowUps into a single card with stacked avatars (`StackedAvatarBadges`) and formatted participant names (`"Alice & Bob"` / `"Alice, Bob & 2 others"`), offering a quick profile chooser dialog on tap when `> 1` participant exists.
   - Individual Contact profiles (`LivingTimeline`) display a subtle `"with ..."` tonal badge on shared items.

4. **Opt-In Check-in Digest (Amends ADR-0036):**
   - Preserve zero manual per-contact cadences and keep `SerendipityPicker` (`>= 14d` quiet Contacts) as the selection engine.
   - Add an opt-in `CheckInDigestFrequency` setting (`OFF` by default, `WEEKLY`, or `BIWEEKLY`) under Settings -> Preferences, scheduled via WorkManager (`CheckInDigestWorker`) at the user's preferred delivery hour on a dedicated `checkin_digest` notification channel with `VISIBILITY_PRIVATE` lock-screen redaction.
   - Keep `FollowUp` alarm delivery (`FollowUpAlarmReceiver`) multi-contact aware so group Reminders render all active participant names.

## Consequences

- **Positive:** Eliminates duplicate encounter/reminder entry drift, surfaces group context cleanly across timelines and alarms, and enables gentle periodic reconnection nudges without notification fatigue or manual cadence overhead.
- **Compatibility:** Legacy single-contact SQLite databases, Export Bundles, and UI previews continue operating seamlessly.

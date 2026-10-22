# 05: Follow-Up DAO & Upcoming Reminders Stream Integration

**What to build:** Actionable reminder persistence for stay-in-touch cadences. When the user checks the Upcoming feed on Home, active reminders are streamed from Room sorted by due date. Marking a reminder done or snoozing updates the database atomically in `<2ms` and reactively updates the Upcoming feed without mock state resets.

**Blocked by:** 02: Contact Entity, DAO & Contacts Screen Reactive StateFlow

**Status:** ready-for-agent

- [ ] `FollowUpEntity` and `FollowUpDao` are implemented with queries for pending follow-ups (`doneAt IS NULL`), mark done, and snooze date updates.
- [ ] Mappers preserve pure domain `FollowUp` models with ISO-8601 timestamps.
- [ ] `FollowUpRepository` provides reactive `Flow<List<FollowUp>>` queries and mutation methods.
- [ ] `HomeViewModel` binds `upcomingAlerts` to the repository stream and routes `OnReminderDone` and `OnReminderSnooze` events to database mutations.
- [ ] Unit tests verify state updates emit reactively and ACID update transactions execute in `<2ms`.

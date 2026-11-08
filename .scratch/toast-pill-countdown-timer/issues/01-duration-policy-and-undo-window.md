# 01: Duration Policy, Semantic Progress Contract, and 7-Second Repository Undo Window

**What to build:** Establish the duration resolution logic and data contract that maps user feedback messages to display durations (7000ms for recoverable actions containing "Deleted", "Archived", or "Restored"; 3500ms for standard informational notices) and provides the countdown progress calculations. Update the in-memory undo retention window across repositories to match the 7-second display lifecycle so that tapping Undo near the end of the countdown safely restores the entity in local SQLite.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Messages with an "Undo" action or destructive keywords ("Deleted", "Archived", "Restored") resolve to 7000ms duration.
- [ ] Informational notices without an undo action resolve to 3500ms duration.
- [ ] Progress fraction calculation accurately yields values from 1.0 down to 0.0 based on elapsed time vs total duration.
- [ ] Progress semantic color resolves to error tone for delete operations, secondary amber for archive operations, and primary brand tone for standard actions.
- [ ] In-memory undo cache in repositories remains valid and successfully executes rollback for up to 7 seconds.
- [ ] Unit tests in test suite verify duration mapping, progress math, and repository cache validity.


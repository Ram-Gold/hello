# 02: Dissolve Deletion Animation and Batch Selection Exit

**What to build:**
Implement the permanent deletion departure animation: contact card scales down ($1.0\times \rightarrow 0.92\times$) and fades out cleanly ($150\text{ms}$ `LinearOutSlowInEasing`), signaling permanent destruction. Support simultaneous multi-item exit animations when confirming batch deletion in the contacts selection action tray.

**Blocked by:** 01 (Core Exit Motion Tokens and Spatial Archive Tuck Animation)

**Status:** closed

- [x] Implement deletion dissolution animation ($0.92\times$ scale + alpha fade + row height collapse).
- [x] Connect delete animation to single-contact deletion confirmation in `PersonEditScreen` and detail views.
- [x] Connect batch deletion animation to `ContactsScreen` selection tray confirmation.
- [x] Ensure non-blocking completion so deletion persistence to Room SQLite executes seamlessly.


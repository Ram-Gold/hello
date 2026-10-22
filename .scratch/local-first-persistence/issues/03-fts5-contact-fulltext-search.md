# 03: FTS5 Contact Full-Text Search Virtual Table

**What to build:** Instant, sub-10ms full-text search across contacts. When a user types into the search bar in the Contacts screen, query execution leverages a dedicated SQLite FTS5 virtual table indexing names, notes, and aliases with prefix matching. Results update immediately as the user types without frame drops or noticeable latency.

**Blocked by:** 02: Contact Entity, DAO & Contacts Screen Reactive StateFlow

**Status:** ready-for-agent

- [ ] Room `@Fts4` / `@Fts5` virtual table entity (`ContactFtsEntity`) is configured with external content backing from `contacts`.
- [ ] Contact full-text search query in `ContactDao` indexes `name`, `notes`, and `aliases` with match tokenization.
- [ ] `ContactsViewModel` search event pipeline queries the FTS virtual table via repository and exposes matching results as a reactive `StateFlow`.
- [ ] Empty or cleared search queries gracefully fall back to default sorted contacts list.
- [ ] Unit tests and performance microbenchmarks confirm FTS query execution completes in `<10ms` over realistic contact datasets.
- [ ] Search correctly handles partial queries, whitespace, special symbols, and multi-word token queries.

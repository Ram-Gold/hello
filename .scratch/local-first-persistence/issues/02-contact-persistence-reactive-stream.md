# 02: Contact Entity, DAO & Contacts Screen Reactive StateFlow

**What to build:** An end-to-end reactive persistence loop for contacts. When users view the Contacts list or add a new contact via Quick Add, data reads and mutations flow through Room rather than transient in-memory mocks. The Contacts directory updates instantly in response to database changes, and newly created contacts persist durably with atomic ACID saves committing in `<2ms`.

**Blocked by:** 01: Room & KSP Database Infrastructure Setup

**Status:** ready-for-agent

- [ ] `ContactEntity` and `ContactDao` (providing `PersonDao` interface capability per domain glossary) are implemented with Room CRUD queries and reactive `Flow<List<ContactEntity>>` streams.
- [ ] Bidirectional domain mappers convert between pure domain `Contact` models and `ContactEntity` without leaking Room annotations into domain layer.
- [ ] `ContactRepository` is introduced to manage local queries and mutations.
- [ ] `ContactsViewModel` and `HomeViewModel` are wired to collect reactive `StateFlow` streams backed by Room queries rather than static mock lists.
- [ ] Quick Add actions commit contact inserts through Room in `<2ms` during ACID benchmarks.
- [ ] Automated tests verify end-to-end reactive emission to UI state upon contact creation and update.

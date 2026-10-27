# 6. SQLite as Canonical Truth with Multi-Format Portability (JSON, Markdown, CSV)

## Context
`AGENTS.md` Prime Directive 2 originally specified a file-centric architecture: "Markdown is Truth, Room is Disposable", requiring all CRM data to be stored primarily as individual `.md` files via Android Storage Access Framework (SAF), with Room serving solely as a rebuildable query cache. 

During the design and grilling of the System Hardware & Native Subsystems (Location, Alarms, Glance Widget, and Portability), this model was reassessed against real-world mobile usage:
1. Contact records frequently evolve with complex relational data: multi-line encounter notes, timestamps, tags, circles, and actionable `FollowUp` tasks. In a pure file-system canonical store, multi-table transactions (e.g. atomic saves across contacts, encounters, and follow-ups) introduce file locking, partial write risks, and significant SAF I/O overhead on low-end devices.
2. User evaluation identified that while Markdown is invaluable for external interoperability (such as reading notes in Obsidian or Logseq), managing raw SAF file trees as the primary live transaction log degrades cold-start speed and battery life without offering tangible day-to-day benefits over a high-performance local SQLite database.
3. Users require portable backups that are also usable for spreadsheet analysis (CSV) and full-fidelity restore (JSON), in addition to readable PKM notes (Markdown).

## Decision
1. **SQLite as Canonical Store**:
   Room (SQLite + FTS5) is established as the canonical, authoritative source of truth for all application data. All create, read, update, and delete operations execute directly against Room transactions with sub-2ms persistence guarantees. This supersedes the previous "Markdown is Truth, Room is Disposable" directive.
2. **On-Demand Multi-Format Export Bundle via WorkManager**:
   Data sovereignty and zero-backend portability are preserved through a user-initiated `WorkManager` background job. When triggered from Settings (or SAF file selection), the worker compiles a complete, timestamped `Export Bundle` ZIP containing:
   - **`*.json`**: Machine-readable full schema dumps (`contacts.json`, `encounters.json`, `places.json`, `tags.json`, `followups.json`) for lossless backup and future restore.
   - **`contacts/*.md`**: Individual human-readable Markdown files per `Contact` featuring YAML frontmatter (tags, circles, cadence, contact info) and chronologically formatted encounter notes for native Obsidian/Logseq compatibility.
   - **`*.csv`**: Tabular CSV exports (`contacts.csv`, `encounters.csv`) for spreadsheet review and data science workflows.
3. **Storage Access Framework (SAF) Delivery**:
   The generated Export Bundle is delivered to user-designated storage using `ACTION_CREATE_DOCUMENT`, ensuring zero cloud reliance, zero external network requests, and total user ownership.

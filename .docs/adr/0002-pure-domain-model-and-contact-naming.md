# 2. Pure Domain Models, UUIDs, and Canonical Contact Terminology

## Context
When designing the foundational backend models for "Hello", we evaluated entity naming, identifier strategies, and database separation. While external specifications often refer to CRM entries generically as "Person", our project domain rules in `GLOSSARY.md` explicitly enforce `Contact` (avoiding Person, Account, Client) to denote meaningful personal and professional relationships.

Furthermore, per the prime architectural directives in `AGENTS.md`, "Markdown is Truth, Room is Disposable": the domain layer must remain pure Kotlin with zero Android Room, SQLite, or database framework dependencies.

## Decision
1. **Canonical Entity Naming (`Contact`)**:
   We establish `Contact` as the core domain entity, with `contactId` as the foreign key in `Encounter` and `FollowUp`.
2. **Context Spheres vs Freeform Tags**:
   We retain `Circle` for relational contexts (e.g., Work, Friends, Family, Uni) and introduce `Tag` for flexible cross-cutting labels, removing hierarchical `folderId`.
3. **Identifier & Timestamp Strategy**:
   All entities utilize string UUIDs (`java.util.UUID`) as primary keys to support offline-first creation and deterministic file-based Markdown mapping. Timestamps utilize `java.time.Instant` for ISO-8601 serialization.
4. **Resilient Location Snapshotting**:
   Coordinates (`latitude`, `longitude`) are stored as nullable `Double?` on `Encounter` and `Place`, ensuring capture never breaks when location permissions are denied. Encounters may optionally link to a cataloged `Place` via `placeId`.
5. **Decoupled Purity**:
   Domain models contain no database annotations. Database mappings (Room entities, DAOs, SAF serializers) will map from these domain models in subsequent layers.

# 04: Encounter, Place, and Tag DAOs & Recent Encounters Feed Integration

**What to build:** Durable persistence and timeline rendering for recorded encounters, venues, and tags. When the user logs an encounter or views the Home dashboard, recent encounters and location/tag metadata stream directly from Room database joins. Logging an encounter updates the contact's recency and the home feed instantly, committing in `<2ms`.

**Blocked by:** 02: Contact Entity, DAO & Contacts Screen Reactive StateFlow

**Status:** ready-for-agent

- [ ] Room entities and DAOs are implemented: `EncounterEntity` & `EncounterDao`, `PlaceEntity` & `PlaceDao`, and `TagEntity` & `TagDao`.
- [ ] Relational queries support loading recent encounters with associated contact, place, and tag details.
- [ ] `EncounterRepository` coordinates saving encounters, updating contact `lastMetAt` / `meetCount`, and incrementing place/tag use counts within a single atomic database transaction.
- [ ] `HomeViewModel`'s `recentContacts` and recent encounter feed collect real reactive streams from the persistence layer.
- [ ] Unit tests verify ACID transactions commit in `<2ms` and maintain referential integrity with nullable coordinates/place IDs when location is unavailable.

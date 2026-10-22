# 01: Room & KSP Database Infrastructure Setup

**What to build:** The foundational local persistence engine and build toolchain that enables offline-first storage and high-speed SQLite access for Hello. When the app initializes, the Room database provides schema validation, converter support for ISO-8601 timestamps and UUIDs, and an isolated in-memory test harness for sub-millisecond query verification.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] KSP and AndroidX Room plugins and runtime dependencies (`room-runtime`, `room-ktx`, `room-compiler`) are configured in Gradle catalog and app build scripts.
- [x] `HelloDatabase` abstract class is declared with schema versioning and Room database builder helper.
- [x] Type converters are established for `java.time.Instant`, `java.util.UUID`, and string/enum lists (such as `Circle`).
- [x] Unit test harness supports fast in-memory SQLite instantiation for deterministic isolated testing.
- [x] `./gradlew testDebugUnitTest` and compile tasks pass cleanly without warnings.

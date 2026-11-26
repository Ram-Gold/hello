# AGENTS.md — Hello

Local-first Android personal CRM (Kotlin, Jetpack Compose, Material 3, Room SQLite + FTS5). Manual DI; check `app/build.gradle.kts` for exact dependencies.

## Context Pointers

- **Domain terms & ADRs**: Read `GLOSSARY.md` and `.docs/adr/` before designing features or naming symbols. See `.docs/agents/domain.md`.
- **Visual design & motion**: Read `.agents/DESIGN.md` for tokens, M3 Expressive connected surfaces, borderless tag pills (ADR-0038), and Emil Kowalski motion ("Everything moves with a purpose in animation and motion design").
- **Code review standards**: Read `CODING_STANDARDS.md` during review for Compose/IME, UDF, and UI copy judgement calls.
- **Build, test & lint commands**: Read `.agents/skills/android-workflows/SKILL.md`.
- **Issue tracker (opt-in only)**: Linear (`linear_linear` MCP, Team `Ramblings` / `RAM`, Project `Hello` / `P-RAM-5`) only when the user references a Linear issue or asks to use Linear. Transition referenced issues to `In Progress` on start and `In Review` on completion. See `.docs/agents/issue-tracker.md` and `.docs/agents/triage-labels.md`.

## Non-Negotiable Invariants

1. **Local-First, Zero Backend**: Keep all data on-device. Zero accounts, cloud sync, network calls, or remote analytics.
2. **SQLite Canonical Truth + Portable Export (ADR-0006)**: Persist all mutations transactionally in Room (SQLite + FTS5). Guarantee portability via the `WorkManager` `Export Bundle` pipeline (JSON, Obsidian Markdown + YAML frontmatter, CSV) saved via SAF (`ACTION_CREATE_DOCUMENT`). Preserve backward compatibility on existing export/frontmatter formats and write files atomically (temp file + rename).
3. **Strict Permission Discipline**: Never request `READ_CONTACTS`, `WRITE_CONTACTS`, or `ACCESS_BACKGROUND_LOCATION`. Location is foreground-only (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`), preceded by an in-app explainer sheet, with graceful degradation if denied.
4. **Sub-50ms Capture Startup**: Keep `QuickCaptureActivity` lean with zero main-thread disk I/O or heavy initializers.

## Codebase Map & Hidden Dependencies

- **App shell & bottom dock**: `app/src/main/java/com/example/hello/ui/MainAppShell.kt` hosts `QuickActionSheetHost` and `FloatingDockOverlay` (`HomeFloatingBottomDock.kt`).
- **Quick Capture**: `QuickCaptureComposerCard.kt` (shared by in-app `QuickActionSheetHost` and widget `QuickCaptureActivity`), state in `QuickActionUiContract.kt` and `QuickActionViewModel.kt`.
- **Persistence & seed modules**: `ContactRepository.kt` coordinates `FollowUpStore.kt`, `UpcomingAlertEngine.kt`, `UndoActionStore.kt`, and `domain/mock/SeedDataFactory.kt` (zero UI imports in `data/`). When adding fields to `ContactSummary`, update `ContactEntity.toSummary`, `Contact.toSummary`, `ContactRepository.updateContact`, and `SeedDataFactory.createSampleContactSummaries()` together.

## Agent skills

### Issue tracker

Linear (`linear_linear` MCP — Team `Ramblings` / `RAM`, Project `Hello` / `P-RAM-5`), **opt-in only** when the user references a Linear issue or asks to use Linear. Automatically transitions referenced issues to `In Progress` on start and `In Review` on completion. See `.docs/agents/issue-tracker.md`.

### Triage labels

Default canonical roles (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `.docs/agents/triage-labels.md`.

### Domain docs

Single-context (`GLOSSARY.md` and `.docs/adr/` at repo root). See `.docs/agents/domain.md`.

# AGENTS.md — Development Directives for "Hello"

> **Project:** Hello (Local-First Android Personal CRM)  
> **Core Identity:** Ultra-fast (<50ms cold start) relationship keeper and encounter capture tool.  
> **Stack:** 100% Native Kotlin, Jetpack Compose, Material Design 3, Room (SQLite + FTS5) + Local Markdown Storage.

---

## 1. Prime Directives & Architectural Invariants

Every AI agent and contributor working on this codebase **MUST** uphold these non-negotiable architectural rules:

1. **Local-First & Zero Backend:**
    - There are **no accounts, no cloud sync, no Supabase, and no remote servers**.
    - Do not introduce network calls, cloud authentication, or remote analytics.
    - The user's device is the sovereign home of all data.

2. **Storage Invariant: SQLite as Canonical Truth with Multi-Format Portability (ADR-0006):**
    - **Canonical Store:** Room DB (SQLite + FTS5) is the authoritative, zero-latency local store on device. All mutations execute transactionally with sub-2ms persistence.
    - **Portability & Export Bundle:** Absolute data sovereignty is guaranteed via an on-demand `WorkManager` export pipeline that packages all data into a timestamped `Export Bundle` (machine-readable JSON schemas, individual Obsidian-compatible `.md` notes with YAML frontmatter, and CSV tabular sheets) saved to user-designated storage via Storage Access Framework (`ACTION_CREATE_DOCUMENT`).

3. **Strict Permission Discipline:**
    - **NEVER** request `READ_CONTACTS` or `WRITE_CONTACTS`. Hello maintains a completely independent people directory to protect user trust.
    - **NEVER** request `ACCESS_BACKGROUND_LOCATION`. Location is foreground-only (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) captured strictly at the instant of an encounter.
    - Always display an in-app explainer sheet before triggering system permission dialogs. Degradation must be graceful—denying location never breaks capture.

4. **Extreme Capture Speed (<50ms Cold Start):**
    - The Quick Add surface (`QuickCaptureActivity`) must launch and display the soft keyboard in **under 50 milliseconds**.
    - Keep the capture activity tree lean. Do not inject heavy initializers or perform disk I/O on the main thread during startup.
    - Measure and maintain Baseline Profiles (`androidx.profileinstaller`).

---

## 2. Technology Stack & Key Libraries

| Component | Library / Tool | Implementation Notes |
| :--- | :--- | :--- |
| **Language** | Kotlin 100% | Coroutines, `StateFlow`, Kotlin DSL |
| **UI Framework** | Jetpack Compose (Compose BOM) | Declarative, state-driven UI |
| **Design System** | Material Design 3 (`androidx.compose.material3`) | Material You dynamic color + curated warm beige/orange fallback theme |
| **Persistence Cache** | Android Room (SQLite + FTS5) | Fast indexing, reactive `Flow` queries |
| **Canonical Storage** | Markdown + YAML Frontmatter | `kamaml` / `snakeyaml-engine` parser + deterministic writer |
| **App Widgets** | Jetpack Glance (`androidx.glance`) | Compose runtime over RemoteViews for instant launcher widgets |
| **Background Work** | Jetpack WorkManager | Scheduled auto-exports and local notifications |
| **Navigation** | Jetpack Navigation Compose | Type-safe routes, custom bottom dock |
| **Image Loading** | Coil 3 (`io.coil-kt.coil3:coil-compose`) | Local URIs from Android Photo Picker; no network caching |
| **Maps** | MapLibre Native Android SDK / Compose | Offline PMTiles / local vector tile caching; monotone custom style |

---

## 3. Design System & UI Principles

- **Core Philosophy:** **Ease of Use & Seamlessness** — minimalist, editorial, and tactile (Mallow & Vellum per `.agents/DESIGN.md`), prioritizing frictionless interaction and visual continuity over boxed borders.
- **Color Palette (Monochrome Minimalist):**
    - Backgrounds: Crisp Canvas (`#FFFFFF` light, `#121212` dark)
    - Cards & Surfaces: Borderless tonal surfaces (`surfaceContainer`, `surfaceContainerHigh`) grouped in connected Material 3 Expressive containers (`24.dp` outer / `4.dp` inner seams, `2.dp` item gaps)
    - Primary: `#121212` (light) / `#FFFFFF` (dark)
    - Foreground: `#161616` (light) / `#E4E4E7` (dark)
    - Foreground Muted: `#3A3A3A` (light) / `#A1A1AA` (dark)
- **Tags & Selectable Chips (ADR-0038):**
    - Strictly borderless (`0.dp` border; background fill only).
    - Full stadium pill (`CircleShape`) when unselected or static; smoothly morphs to an `8.dp` squircle (`RoundedCornerShape(8.dp)`) when selected.
    - Uniform `28.dp` height, proportional 2:1 padding (`horizontal = 12.dp, vertical = 6.dp`), and natural text-relative width across every screen.
- **Visual Personality:**
    - Typography: Mallow (Headings) and Vellum (Body / Captions).
    - Motion physics: Snappy spring animations (`spring(dampingRatio = ..., stiffness = ...)`).

---

## 4. State Management & Code Patterns

### Unidirectional Data Flow (UDF)
Every screen must follow the standard UDF contract:
```kotlin
// 1. Immutable State
data class HomeUiState(
    val userGreeting: String = "",
    val activeCard: ContactCardType = ContactCardType.WORK,
    val upcomingReminders: List<ReminderItem> = emptyList(),
    val recentEncounters: List<PersonSummary> = emptyList(),
    val isLoading: Boolean = false
)

// 2. Explicit User Intents / Events
sealed interface HomeUiEvent {
    data object OnToggleCard : HomeUiEvent
    data class OnSnoozeReminder(val reminderId: String) : HomeUiEvent
    data class OnCompleteReminder(val reminderId: String) : HomeUiEvent
    data class OnSelectPerson(val personId: String) : HomeUiEvent
    data object OnQuickAddClick : HomeUiEvent
}

// 3. ViewModel with StateFlow
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecentEncountersUseCase: GetRecentEncountersUseCase,
    private val getUpcomingRemindersUseCase: GetUpcomingRemindersUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            // handle events deterministically
        }
    }
}
```

### Clean Architecture Layers
1. **`ui/`**: Pure composables, ViewModels, and UI state models. Zero direct Room or file system dependencies.
2. **`domain/`**: Pure Kotlin business models (`Person`, `Encounter`, `Tag`, `Place`), repository interfaces, and use cases.
3. **`data/`**: Room DAOs, Markdown/YAML parser-serializer, SAF storage bridges, and repository implementations.

---

## 5. Engineering Safety & Agent Guardrails

- **Do NOT break file formats:** When adjusting frontmatter YAML keys, guarantee backward compatibility. Existing user notes must never be corrupted.
- **Atomic File Writes:** When saving Markdown files, always write to a temporary file first and atomically rename/replace to prevent corruption during app crashes or battery depletion.
- **No Git Sabotage:** Never execute destructive commands (`git reset --hard`, `git push --force`, `git clean -fd`) without explicit confirmation.
- **Maintain Test Coverage:** Write unit tests for all state reducers, YAML parsers, and sync reconciliation logic before touching UI implementations.

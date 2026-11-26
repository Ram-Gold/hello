# Android Performance & Optimization Audit: Hello

Date: 2026-11-25  
Scope: Cold start (`QuickCaptureActivity` <50ms target), memory & bitmap decoding, Jetpack Compose recomposition & list scrolling, Room SQLite schema & queries, release build (R8 & Baseline Profiles), and coroutines/background work.

---

## Critical

### 1. Release build ships unminified with `material-icons-extended` and no Baseline Profiles
- **Sources:**
  - [app/build.gradle.kts](file:///home/ram/Projects/Hello/app/build.gradle.kts#L26-L29): `release { isMinifyEnabled = false }` (no `isShrinkResources = true`, no `proguard-rules.pro`).
  - [app/build.gradle.kts](file:///home/ram/Projects/Hello/app/build.gradle.kts#L53): `implementation(libs.androidx.compose.material.icons.extended)`.
  - [gradle/libs.versions.toml](file:///home/ram/Projects/Hello/gradle/libs.versions.toml): no `androidx.profileinstaller` or Baseline Profile setup.
- **Impact:**
  - `material-icons-extended` adds ~10,000+ icon classes (~12-18 MB DEX) when R8 is disabled, inflating APK size by ~15-25 MB, slowing cold-start class loading, and leaving Compose/Kotlin bytecode unoptimized.
  - Without Baseline Profiles (`androidx.profileinstaller`), ART JIT-compiles Compose, Material 3, Room, and `QuickCaptureActivity` hot paths on first launch (~100-300 ms cold-start penalty).
- **Recommended fix:**
  1. Enable `isMinifyEnabled = true`, `isShrinkResources = true`, and `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")` in `app/build.gradle.kts`.
  2. Add `androidx.profileinstaller` and generate a Baseline Profile covering `MainActivity`, `QuickCaptureActivity`, and root tab switches (or replace `material-icons-extended` with the ~20-30 vector drawables actually used).

### 2. Per-Activity / per-composition repository creation and uncancelled `CoroutineScope` leaks
- **Sources:**
  - [QuickCaptureActivity.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/quickaction/QuickCaptureActivity.kt#L62-L66): `onCreate()` constructs a fresh `ContactRepository` and `QuickActionViewModel` on every widget launch.
  - [MainAppShell.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/MainAppShell.kt#L199-L226): `ContactRepository` and all root ViewModels (`QuickActionViewModel`, `HomeViewModel`, `TimelineViewModel`, `ContactsViewModel`) are created in `remember { ... }` instead of an `Application`-scoped container / `ViewModelProvider`, so every configuration change recreates them.
  - [ContactRepository.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/repository/ContactRepository.kt#L62-L93): Allocates an uncancelled `CoroutineScope(ioDispatcher + SupervisorJob())`, synchronously builds all mock/sample datasets on the **main thread** (`initialFullContacts()`, `ContactsViewModel.createSampleContacts()`, `HomeViewModel.createInitialState()`, `initialEncounters()`, `refreshFavorites()`) even when Room is present, and launches 4 perpetual Room `Flow` collectors ([ContactRepository.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/repository/ContactRepository.kt#L197-L251)).
  - [QuickActionViewModel.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/quickaction/QuickActionViewModel.kt#L33-L62): Creates a fresh `CaptureFlowRepository` (which starts its own uncancelled Room collector) and uses an uncancelled `CoroutineScope(ioDispatcher + SupervisorJob())` instead of `viewModelScope`.
  - [AndroidManifest.xml](file:///home/ram/Projects/Hello/app/src/main/AndroidManifest.xml#L21-L31): No `Application` subclass holding process-wide singletons.
- **Impact:**
  - Directly hurts the `<50ms` `QuickCaptureActivity` startup budget via main-thread sample-data allocation and redundant DB collectors.
  - Every `QuickCaptureActivity` open/close and every `MainActivity` configuration change leaks a `ContactRepository`, `CaptureFlowRepository`, `QuickActionViewModel`, and 5+ active Room `InvalidationTracker` collectors that all re-query on every future DB write.
- **Recommended fix:**
  1. Add a lean `HelloApplication : Application()` holding lazy singletons for `HelloDatabase`, `ContactRepository`, `CaptureFlowRepository`, and `DataStoreUserPreferencesRepository`.
  2. When `database != null`, initialize `ContactRepository`'s `MutableStateFlow`s to empty collections instead of building mock/sample lists on the main thread.
  3. Inject the shared `CaptureFlowRepository` into `QuickActionViewModel` and use `viewModelScope` (with `ViewModelProvider`).

---

## High

### 3. Duplicate startup DB mapping and O(N x T) per-contact allocations in `ContactRepository`
- **Sources:**
  - [ContactRepository.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/repository/ContactRepository.kt#L120-L137) vs [L200-L219](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/repository/ContactRepository.kt#L200-L219): `init` performs a full one-shot load + mapping + birthday/serendipity pass, then immediately calls `observeDatabaseStreams()` which repeats the exact same pass on initial Flow emission.
  - [ContactRepository.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/repository/ContactRepository.kt#L1935-L1969) (`ContactEntity.toSummary`): Inside the per-contact loop, allocates `availableTags.associateBy { it.id }`, `DateTimeFormatter.ofPattern("MMM d")`, `LocalDate.now()`, `ZoneId.systemDefault()`, and `Regex("\\s+")` via [Models.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/domain/model/Models.kt#L38). Also calls `toSummary` a second time for `_recentContacts`.
- **Impact:**
  - Doubles DB-to-domain work on startup and triggers O(N x T) map/regex/formatter allocations on every single contact write.
- **Recommended fix:**
  - Drop the redundant one-shot load at lines 123-135, hoist `tagsById`, `today`, `zoneId`, a cached `DateTimeFormatter`, and a precompiled `WHITESPACE_REGEX` out of `toSummary`, and derive `_recentContacts` from the already-mapped `ContactSummary` list.

### 4. Unindexed Room tables, full-table `OR id IN (SELECT ...)` scans, and N+1 startup alarm queries
- **Sources:**
  - Missing `@Index` on [EncounterEntity.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/local/entity/EncounterEntity.kt#L8-L19) (`contactId`, `placeId`, `occurredAt`), [FollowUpEntity.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/local/entity/FollowUpEntity.kt#L8-L17) (`contactId`, `doneAt`, `dueAt`), and [ContactEntity.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/local/entity/ContactEntity.kt#L12-L34) (`isArchived`, `name`, `lastMetAt`).
  - [EncounterDao.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/local/dao/EncounterDao.kt#L19-L27) and [FollowUpDao.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/data/local/dao/FollowUpDao.kt#L31): `WHERE contactId = :contactId OR id IN (SELECT ...)` forces full-table scans.
  - [FollowUpAlarmScheduler.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/platform/notification/FollowUpAlarmScheduler.kt#L49-L173) called on every [MainActivity.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/MainActivity.kt#L43): Runs `1 + P` Room queries per pending follow-up (N+1) AND re-arms `RTC_WAKEUP` alarms 1 second in the future (`coerceAtLeast(now + 1000L)`) for overdue reminders on every app launch.
- **Impact:**
  - Linear query slowdown as data grows, 10-100+ SQLite queries during cold start, and unnecessary alarm wakeups/notification bursts for overdue reminders.
- **Recommended fix:**
  - Add a Room migration creating indexes on foreign keys and sort/filter columns, rewrite `OR id IN (...)` as `UNION` (or query via cross-ref tables), batch-load follow-up participants in `rescheduleAllPendingFromDatabase()`, and skip re-arming already-overdue alarms on cold start.

### 5. `PersonDetailScreen` composes the entire `LivingTimeline` eagerly in one `LazyColumn` item
- **Sources:**
  - [PersonDetailScreen.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/detail/PersonDetailScreen.kt#L317-L377): Wraps the entire `LivingTimeline(...)` in a single `item(key = "timeline")`.
  - [LivingTimeline.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/detail/components/LivingTimeline.kt#L91-L160): Uses a plain `Column` with `forEach`, eagerly composing every encounter/reminder and calling `LocalDate.now()`, `sortedByDescending`, and `DateTimeFormatter.ofPattern(...)` inside composition.
- **Impact:**
  - Main-thread stall when opening a contact with dozens/hundreds of encounters or toggling multi-select mode.
- **Recommended fix:**
  - Convert `LivingTimeline` to a `LazyListScope` extension emitting individual keyed items with `contentType`, move sorting into `PersonDetailViewModel`, and hoist `DateTimeFormatter`s.

### 6. Unbounded byte-size `AvatarBitmapCache` (up to 64 MB) and oversized decoding for list thumbnails
- **Sources:**
  - [AvatarStorageAndCrop.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/platform/avatar/AvatarStorageAndCrop.kt#L281-L377): `LruCache<String, ImageBitmap>(64)` uses entry count instead of KB size, has no `onTrimMemory` eviction, always decodes at `maxDimension = 512` (~1 MB per ARGB_8888 bitmap = up to 64 MB retained even for `48.dp` list avatars), opens `content://` streams 3 times per decode, and does not `.recycle()` temporary rotated/cropped bitmaps.
- **Impact:**
  - High pinned bitmap memory and extra GC/LMK pressure on devices with many contact photos.
- **Recommended fix:**
  - Switch `AvatarBitmapCache` to a KB-sized `LruCache` (e.g. capped at 16 MB) with `onTrimMemory()` eviction in `HelloApplication`, decode list thumbnails at ~160px vs 512px for detail portraits, and `.recycle()` intermediate bitmaps.

---

## Medium

### 7. Missing `contentType` in `TimelineScreen` `LazyColumn` and unstable domain models
- **Sources:**
  - [TimelineScreen.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/timeline/TimelineScreen.kt#L346-L539): 6 distinct row layouts share one `LazyColumn` with no `contentType`.
  - [Models.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/domain/model/Models.kt#L162-L178), `Contact.kt`, `Encounter.kt`, `FollowUp.kt`: Contain `List<T>`, `Map`, and `java.time.*` without `@Immutable` or a Compose compiler stability config (`composeCompiler { stabilityConfigurationFiles ... }`), so Strong Skipping falls back to referential equality (`===`) and recomposes list items whenever `ContactRepository` emits a new list.
- **Recommended fix:**
  - Add explicit `contentType` values to all `TimelineScreen` `LazyColumn` items and configure a Compose stability config file marking `java.time.*` and immutable domain models as stable.

### 8. Per-frame recomposition and `BlurEffect` allocations in `HomeFloatingBottomDock`
- **Sources:**
  - [HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L621-L624): Reads `savedTransition.value` and `bellRotation.value` in composition instead of deferring reads into `graphicsLayer`.
  - [HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L482-L993): Allocates new `BlurEffect` instances per frame inside `graphicsLayer` alongside `CompositingStrategy.Offscreen`.
- **Recommended fix:**
  - Pass `() -> Float` state providers into `ProfileReminderPillContent` so animation reads stay in the draw phase, and quantize/cache `BlurEffect` radii.

### 9. `collectAsState()` used everywhere (29 sites) instead of `collectAsStateWithLifecycle()`
- **Sources:**
  - 29 `collectAsState(` calls across `app/src/main/java/` (including [MainActivity.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/MainActivity.kt#L61), [QuickCaptureActivity.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/quickaction/QuickCaptureActivity.kt#L71), and [MainAppShell.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/MainAppShell.kt#L228)); `androidx-lifecycle-runtime-compose` is not in [app/build.gradle.kts](file:///home/ram/Projects/Hello/app/build.gradle.kts).
- **Recommended fix:**
  - Add `androidx.lifecycle:lifecycle-runtime-compose` and switch screen/Activity flow collectors to `collectAsStateWithLifecycle()` so collection pauses when backgrounded.

### 10. Repeated `DateTimeFormatter.ofPattern(...)` allocations inside composables
- **Sources:**
  - [QuickCaptureComposerCard.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/quickaction/components/QuickCaptureComposerCard.kt#L1428-L1883) (11 sites), [FlowBLogEncounterSheet.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/quickaction/components/FlowBLogEncounterSheet.kt#L420-L473), [BirthdayInputUtils.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/components/BirthdayInputUtils.kt#L20-L182), and [TimelineViewModel.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/timeline/TimelineViewModel.kt#L658-L680).
- **Recommended fix:**
  - Hoist thread-safe `DateTimeFormatter` instances to `private val` constants.

---

## Low

### 11. `MainActivity.onCreate()` unconditionally re-enqueues `CheckInDigestWorker` with `UPDATE`
- **Sources:** [MainActivity.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/MainActivity.kt#L41-L53) -> [CheckInDigestWorker.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/platform/notification/CheckInDigestWorker.kt#L125-L152).
- **Recommended fix:** Use `ExistingPeriodicWorkPolicy.KEEP` during startup reconciliation (reserve `UPDATE` for Settings changes) and skip WorkManager init when digest is `OFF`.

### 12. `LocationService` uses deprecated `requestLocationUpdates` polling on API 30+
- **Sources:** [LocationService.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/platform/location/LocationService.kt#L124-L162) (note: listener cleanup via `invokeOnCancellation` is already correct).
- **Recommended fix:** Use `LocationManager.getCurrentLocation()` on API 30+ (`Build.VERSION_CODES.R`) for faster, lower-power single fixes, keeping `requestLocationUpdates` for API 24-29.

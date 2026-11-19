# 0035: Opt-In Notionist Avatar Playground, Bounded Network Exception, and Default User-Icon Contact Avatar

**Status:** Accepted  
**Date:** 2026-11-19  
**Amends:** [ADR-0006](0006-sqlite-canonical-truth-and-multi-format-portability.md), [ADR-0022](0022-local-first-contact-avatar-cropping-and-storage.md)

## Context

Users want the option to craft expressive, hand-drawn monochrome character illustrations (**Notionist Avatars**) for individual Contacts while preserving Hello's local-first sovereignty, zero-backend posture, and minimalist Mallow & Vellum visual language. Previously, Hello declared no `INTERNET` permission, rendered Contacts without custom photos as pastel initials monograms (`DoodleAvatar`), and supported only cropped local gallery photos via `PersonEditScreen`.

## Decision

1. **Bounded Network Exception for Opt-In `Avatar Playground`:**
   - Declare `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` strictly for the opt-in, user-initiated `Avatar Playground` screen.
   - **Zero-PII Guarantee:** Requests to the DiceBear `notionists` HTTP API (`api.dicebear.com`) MUST NEVER include any Contact PII (name, nickname, email, phone, notes). Generation always uses a random opaque seed combined with explicit trait query parameters.
   - Zero background network traffic, zero accounts, zero analytics, and zero third-party networking or SVG rendering libraries: fetch `256×256` PNGs directly over built-in `HttpURLConnection` / `ConnectivityManager` and decode natively via `BitmapFactory`.

2. **Local-First Persistence & Re-Editability (`AvatarStorageManager` + Room v6):**
   - Applying an avatar in `Avatar Playground` writes the fetched PNG bytes as a staged local file via `AvatarStorageManager` (`filesDir/avatars/staged_<uuid>.png`) alongside the serialized Notionist trait recipe in `PersonEditUiState`.
   - Committing **Save** on `PersonEditScreen` promotes the PNG to `filesDir/avatars/contact_<contactId>_<timestamp>.png` and persists the nullable `notionistConfig` recipe on `Contact` / `ContactEntity` (Room schema v6 migration: `ALTER TABLE contacts ADD COLUMN notionistConfig TEXT DEFAULT NULL`).
   - All runtime rendering (`DoodleAvatar` / `AvatarBitmapCache`) remains 100% offline from the local PNG file. Re-opening `Avatar Playground` for a Contact with a saved `notionistConfig` pre-loads their previous traits; switching to a gallery photo or tapping **Remove avatar** clears `notionistConfig`.
   - `ExportBundleGenerator` includes `notionistConfig` in `contacts.json` and Markdown frontmatter alongside `avatars/<contactId>.png`.

3. **Default `Contact Avatar` Shift (Monochrome User Icon):**
   - Replace pastel initials monograms app-wide so every Contact without a custom photo or `Notionist Avatar` renders the minimalist `HelloIconKey.USER` icon over a uniform neutral surface tone matching the Mallow & Vellum monochrome palette. `Notionist Avatar` remains strictly opt-in.

4. **Rate-Limit-Safe Native Playground UX:**
   - Because DiceBear's public API enforces a 10 req/s cap on raster PNG requests, trait tabs (`Hair`, `Eyes`, `Brows`, `Lips`, `Nose`, `Beard`, `Glasses`, `Body/Gesture`, `Background`) render selectable numbered/named chips with Prev/Next steppers (and visual swatches for Background) rather than per-cell live network thumbnails, updating the single live preview with ~200ms debounce and an in-memory LRU cache.

## Considered Options

- **SVG fetch + local 512×512 rasterization (`androidsvg`):** Rejected to avoid adding a third-party SVG dependency; 256×256 PNG is native, zero-dependency, and crisp across 48–96dp circular avatar slots.
- **Embedded WebView playground:** Rejected as off-brand against Mallow & Vellum typography/theming, brittle to scrape/export, and heavy.
- **Keep initials monograms or pastel backgrounds behind user icon:** Rejected in favor of a calm, uniform neutral user icon that keeps the focus on intentional photos and opt-in Notionist Avatars.

## Consequences

- **Positive:** Preserves 100% offline day-to-day rendering and `Export Bundle` portability while unlocking expressive opt-in Notionist customization with zero new Gradle dependencies and zero Contact PII leakage.
- **Negative / Tradeoffs:** Introducing `INTERNET` permission requires strict code-review vigilance to ensure no other feature introduces network calls; `256×256` PNG caps raw resolution below the `512×512` cropped-photo target and requires debounced chip selection to stay well inside DiceBear's 10 req/s PNG rate limit.

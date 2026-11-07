# 01: Local-First Avatar Storage Engine, Memory Cache, and Contact Avatar Rendering

**What to build:** End-to-end local-first avatar decoding, EXIF orientation normalization, 512×512 PNG flattening over Warm White (`#FFFFFF`), atomic staged/canonical file lifecycle management in `filesDir/avatars/`, and zero-dependency `LruCache` rendering inside the Contact Avatar component so any contact with a valid `photoUri` displays their custom portrait and automatically falls back to their warm pastel monogram when no photo is present or readable.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Selected media streams decode safely off the main thread with EXIF rotation/flip normalization and downsampling to prevent `OutOfMemoryError` on large images.
- [x] Cropped output renders onto a 512×512 bitmap pre-filled with solid Warm White (`#FFFFFF`) so transparent PNG regions and letterboxed margins flatten cleanly without dark voids.
- [x] Atomic file storage writes staged files (`staged_<uuid>.png`), promotes them to canonical contact avatar files (`contact_<id>_<timestamp>.png`) on commit, and deletes superseded or discarded files.
- [x] Contact Avatar (`DoodleAvatar`) accepts an optional `photoUri`, serves cached `ImageBitmap` instances synchronously from an in-memory `LruCache`, and falls back to the warm pastel initials monogram when `photoUri` is null or unreadable.
- [x] Unit tests verify crop coordinate math, `#FFFFFF` background flattening, and staged/canonical file lifecycle cleanup.

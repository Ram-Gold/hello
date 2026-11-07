# 0022: Local-First Contact Avatar Cropping, Atomic Storage, and Export Portability

Raw `content://` URIs returned by the Android Photo Picker (`PickVisualMedia`) carry ephemeral read permissions that expire across process restarts or break if the user deletes the source gallery item. To guarantee permanent local sovereignty and zero-latency rendering without introducing network-capable image loading libraries, Hello processes selected images through an in-app Compose 1:1 circular cropper (with pinch-to-zoom, pan clamping, and a "Fit whole image" toggle) and writes a 512×512px PNG flattened over solid Warm White (`#FFFFFF`) into app-private storage (`filesDir/avatars/`).

## Staged Edit Lifecycle & Portability

- **Staged Editing**: Confirming a crop in `PersonEditScreen` writes a staged file (`filesDir/avatars/staged_<uuid>.png`) so the preview updates immediately while preserving the original avatar if the user discards changes. Committing `Save` promotes the file to `filesDir/avatars/contact_<contactId>_<timestamp>.png` and deletes any superseded avatar file; discarding deletes the staged file.
- **Zero-Dependency Memory Cache**: `DoodleAvatar` renders custom avatars across all surfaces (Edit, Profile Header, Contact Cards on Home and Relationship tabs, and Living Timeline nodes) using an in-memory `LruCache<String, ImageBitmap>` backed by background `BitmapFactory` decoding, falling back smoothly to the warm pastel monogram when `photoUri` is null or unreadable.
- **Export Bundle Inclusion**: To uphold ADR-0006 data portability, `ExportBundleGenerator` includes `photoUri` in `contacts.json` and Markdown frontmatter and packages local avatar PNGs under `avatars/<contactId>.png` inside the ZIP bundle.


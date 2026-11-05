# 01: Minimal Pixel-Style Timeline Header

**What to build:** Refactor the Timeline screen header to be ultra-clean and minimal matching the People/Contacts screen typography and layout. Replace the small headline with the large Pixel-style headline (`headlineMedium`, bold "Timeline"), remove the secondary status text ("25 upcoming · 12 logged"), while preserving the Log Met button and the temporal filter strip (`All` | `Future` | `Past`).

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Timeline header displays prominent "Timeline" text with `headlineMedium` and bold font weight matching ContactsScreen/Settings.
- [x] Subtitle count text ("X upcoming · Y logged") is completely removed.
- [x] Top row cleanly aligns the "Timeline" title and the "Log Met" action button.
- [x] Temporal filter strip (`All` | `Future` | `Past`) sits seamlessly beneath with proper spacing.
- [x] All unit tests pass (`./gradlew testDebugUnitTest`).

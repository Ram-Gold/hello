# Android & Kotlin Conventions

Guidelines for developing Kotlin and Jetpack Compose code in this codebase.

## Language & Modern Practices
- Target modern Kotlin idiom: concise null-safety, standard functions (`let`, `apply`, `also`, `run`), sealed interfaces/classes for state modelling.
- Avoid raw mutable state exposed publicly: expose read-only state (e.g. `StateFlow`, `State<T>`) and mutate only internally (e.g. `MutableStateFlow`).

## Jetpack Compose Architecture
- **Stateless Composables**: Keep UI Composables stateless wherever possible by hoisting state and passing event lambdas upward.
- **State Holders**: Use `ViewModel` or dedicated domain models to manage business logic and screen state.
- **Previews**: Include `@Preview` annotations for reusable UI components to verify layout visual structure.
- **Material3**: Follow the project theme defined in `ui/theme/Theme.kt` and use Material 3 components.

## Testing & Quality
- Write fast, deterministic JVM unit tests in `app/src/test/` for domain and state logic.
- Verify tests pass with `./gradlew testDebugUnitTest`.

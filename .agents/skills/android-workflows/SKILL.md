---
name: android-workflows
description: >-
  Workflows and Gradle commands for building, testing, linting, and verifying the Android app in this repository.
  Use when running unit tests, creating debug builds, checking lint, or troubleshooting Gradle build errors.
---

# Android Workflows

Commands and runbooks for building, testing, and verifying the Android app in this repository.

## Common Gradle Commands

Always run `./gradlew` from the project root.

### 1. Running Tests
- **Run all unit tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Run a specific test class**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.example.hello.ExampleUnitTest"
  ```
- **Run a specific test method**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.example.hello.ExampleUnitTest.addition_isCorrect"
  ```

### 2. Building the App
- **Compile and assemble debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Check compile errors without building APK**:
  ```bash
  ./gradlew compileDebugKotlin
  ```

### 3. Code Quality & Linting
- **Run Android Lint**:
  ```bash
  ./gradlew lintDebug
  ```
- **Check all build and lint constraints**:
  ```bash
  ./gradlew check
  ```

## TDD & Verification Loop

When developing new features or fixing bugs test-first:
1. Write or update a unit test under `app/src/test/java/com/example/hello/`.
2. Run `./gradlew testDebugUnitTest --tests "<TestClass>"` to confirm red (failing test).
3. Implement minimal code under `app/src/main/java/com/example/hello/`.
4. Re-run `./gradlew testDebugUnitTest --tests "<TestClass>"` to confirm green.
5. Verify build with `./gradlew compileDebugKotlin`.

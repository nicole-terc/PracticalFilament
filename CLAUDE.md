# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About This Project

Sample codebase for a talk on Google's Filament 3D rendering framework, built as a **Kotlin Multiplatform (KMP)** project targeting Android and iOS using Compose Multiplatform.

## Build Commands

```bash
# Build everything
./gradlew build

# Android
./gradlew assembleDebug
./gradlew assembleRelease

# iOS (requires macOS)
./gradlew iosArm64Binaries           # Physical device
./gradlew iosSimulatorArm64Binaries  # Simulator

# Tests
./gradlew test
./gradlew testDebug                  # Android debug tests
./gradlew iosSimulatorArm64Test      # iOS simulator tests
```

For iOS app development, open `iosApp/iosApp.xcodeproj` in Xcode.

## Architecture

Single Gradle module (`:composeApp`) with four source sets:

- **`commonMain`** — shared Compose UI and business logic for both platforms
- **`androidMain`** — Android `Activity`, Android-specific `actual` implementations
- **`iosMain`** — `ComposeUIViewController` entry point, iOS-specific `actual` implementations
- **`commonTest`** — shared tests

Platform abstraction uses Kotlin's `expect`/`actual` pattern. The `Platform` interface is declared as `expect` in `commonMain` and implemented in each platform's source set.

Entry points:
- Android: `MainActivity` → `App()` composable
- iOS: `MainViewController` (called from Swift in `iosApp/`)

## Key Dependencies

Managed via `gradle/libs.versions.toml`:
- Kotlin 2.3.20 / Compose Multiplatform 1.10.3
- Material3 `1.10.0-alpha05`
- AndroidX Lifecycle 2.10.0, Activity 1.13.0
- Min SDK 24, Compile/Target SDK 36, JVM target 11

# AGENTS.md

This file is the canonical repository reference for coding agents working in this project.

## Project Summary

Practical Filament is sample code for a talk on Google's Filament 3D rendering framework. The codebase is a Kotlin Multiplatform mobile app targeting Android and iOS with shared UI built in Compose Multiplatform.

Current repo snapshot:
- Workspace git state is not stable; always check `git status` and `git branch --show-current` before assuming branch or cleanliness
- Gradle wrapper verified with `./gradlew help`
- Gradle 9.3.1
- Kotlin 2.3.20
- Compose Multiplatform 1.10.3
- Android Gradle Plugin 9.1.0
- Filament 1.70.1
- The current demo is a metadata-driven material viewer that introspects `.filamat` parameters at runtime and builds controls dynamically

## Repository Structure

- `composeApp/` shared Kotlin Multiplatform module
- `androidApp/` Android application host project
- `iosApp/` native iOS host project and Xcode project
- `iosApp/Frameworks/filament/` vendored iOS Filament SDK headers and static libraries used by the Xcode project
- `gradle/libs.versions.toml` centralized dependency and plugin versions

## Architecture

The shared application logic lives in `:composeApp` and is split across these source sets:

- `commonMain` shared Compose UI, Filament abstractions, theme, screens, typed material parameter models, and helper texture generators
- `androidMain` Android-specific implementations such as `MainActivity`, platform bindings, and the Filament Android engine/view integration
- `iosMain` iOS-specific implementations such as the Compose entry point, `CAMetalLayer` host, and Kotlin-side bridge protocol used by Swift/ObjC++
- `commonTest` shared tests

Platform-specific behavior uses Kotlin `expect`/`actual` declarations where needed.

The Filament integration is organized in three layers:
- Shared KMP API in `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/filament/` via `FilamentEngine`, `FilamentView`, `MaterialParameter`, and `MaterialParameterType`
- Android implementation in `composeApp/src/androidMain/kotlin/dev/nstv/practicalfilament/filament/AndroidFilamentEngine.kt` using Filament's Android/JVM API, `SurfaceView`, `UiHelper`, and a `Choreographer` render loop
- iOS implementation split between Kotlin (`IosFilamentEngine`, `FilamentView.ios.kt`), Swift (`FilamentBridgeAdapter.swift`), and ObjC++ (`iosApp/iosApp/FilamentBridge.h` / `.mm`) over a Metal-backed Filament engine

Runtime material editing is metadata-driven:
- `FilamentEngine.getMaterialParameters(materialHandle)` returns the declared Filament parameter definitions from the compiled `.filamat`
- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/components/ParameterInputField.kt` renders controls based on `MaterialParameterType`
- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/screen/MaterialViewerScreen.kt` loads a material, creates defaults from the metadata, and applies edits back through `setMaterialParameter` or `setTextureParameter`

## Entry Points

- Android: `composeApp/src/androidMain/kotlin/dev/nstv/practicalfilament/MainActivity.kt`
- Shared app root: `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/App.kt`
- Shared main screen: `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/screen/MainScreen.kt`
- Material viewer demo: `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/screen/MaterialViewerScreen.kt`
- iOS Compose host: `composeApp/src/iosMain/kotlin/dev/nstv/practicalfilament/MainViewController.kt`
- iOS Swift host: `iosApp/iosApp/ContentView.swift`
- iOS Swift adapter wiring: `iosApp/iosApp/FilamentBridgeAdapter.swift`

## Build And Test Commands

```bash
# Project help / wrapper verification
./gradlew help

# Build everything
./gradlew build

# Android
./gradlew assembleDebug
./gradlew assembleRelease

# iOS binaries
./gradlew iosArm64Binaries
./gradlew iosSimulatorArm64Binaries

# Tests
./gradlew test
./gradlew testDebug
./gradlew iosSimulatorArm64Test
```

For iOS app development, open `iosApp/iosApp.xcodeproj` in Xcode. The current project is linked against the vendored Filament SDK in `iosApp/Frameworks/filament`.

## Notable Implementation Areas

- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/screen/` main app screens, including the material viewer and material-selection workflow
- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/components/ParameterInputField.kt` dynamic parameter controls for scalars, vectors, matrices, booleans, integer types, arrays, and sampler inputs
- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/filament/FilamentEngine.kt` shared rendering/material/texture API
- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/filament/MaterialParameter.kt` shared material parameter definitions, typed parameter model, default values, and generated built-in textures
- `composeApp/src/androidMain/kotlin/dev/nstv/practicalfilament/filament/` Android Filament engine, material introspection, texture upload, and Android view integration
- `composeApp/src/iosMain/kotlin/dev/nstv/practicalfilament/filament/` Kotlin iOS engine hooks, Metal layer hosting, and the `FilamentBridgeProtocol`
- `iosApp/iosApp/FilamentBridge.mm` and related bridge files for native iOS interop with Filament C++
- `iosApp/iosApp/FilamentBridgeAdapter.swift` Swift adapter that conforms to the Kotlin-exported bridge protocol and delegates to the ObjC++ bridge
- `iosApp/Frameworks/filament/` local Filament iOS distribution consumed by the Xcode project

## Dependency Notes

Managed in `gradle/libs.versions.toml`:
- Kotlin 2.3.20
- Compose Multiplatform 1.10.3
- Material3 1.10.0-alpha05
- AndroidX Lifecycle 2.10.0
- AndroidX Activity 1.13.0
- Android compile SDK 36
- Android target SDK 36
- Android min SDK 25
- JVM target 11
- Filament 1.70.1

Filament dependency setup is platform-specific:
- Android uses the Filament Android artifacts from Gradle
- iOS currently uses the vendored SDK under `iosApp/Frameworks/filament` with Xcode `HEADER_SEARCH_PATHS` / `LIBRARY_SEARCH_PATHS`

## Known Tooling Note

Gradle configuration currently emits an Android Gradle Plugin deprecation warning related to `android.dependency.excludeLibraryComponentsFromConstraints=true`. It does not block initialization, but it should be cleaned up before moving to AGP 10.

## Filament Notes

- `MaterialViewerScreen` should derive editable controls from `getMaterialParameters(...)`; avoid reintroducing hardcoded material parameter lists in shared UI
- Sampler parameters are currently supported through generated built-in textures (`NONE`, `CHECKERBOARD`, `WHITE`, `RED`, `GRADIENT`) created in shared code and uploaded per-platform via `createTexture` / `setTextureParameter`
- The `mirror.filamat` sampler path currently uses static textures only. A real mirror effect would require render-to-texture / multi-pass rendering, which is not implemented
- Android supports typed scalar/vector/array material updates through `setMaterialParameter`
- iOS bridge support is narrower: scalar, vector, `mat3`, `mat4`, and texture/sampler binding are implemented, but repeated uniform arrays are still not supported by the ObjC++ bridge
- On iOS, `FilamentBridgeHolder.shared.bridge` must be assigned from Swift before composing `FilamentView`; `ContentView.swift` currently does this with `FilamentBridgeAdapter()`
- If you change bridge method signatures, keep the Kotlin `FilamentBridgeProtocol`, Swift adapter, ObjC++ header, and ObjC++ implementation in sync

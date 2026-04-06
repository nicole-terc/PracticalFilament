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
- `tools/filament/1.70.1/ios/` vendored iOS Filament SDK headers and static libraries used by the Xcode project
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

For iOS app development, open `iosApp/iosApp.xcodeproj` in Xcode. The current project is linked against the vendored Filament SDK in `tools/filament/1.70.1/ios`.

## Notable Implementation Areas

- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/screen/` main app screens, including the material viewer and material-selection workflow
- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/components/ParameterInputField.kt` dynamic parameter controls for scalars, vectors, matrices, booleans, integer types, arrays, and sampler inputs
- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/filament/FilamentEngine.kt` shared rendering/material/texture API
- `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/filament/MaterialParameter.kt` shared material parameter definitions, typed parameter model, default values, and generated built-in textures
- `composeApp/src/androidMain/kotlin/dev/nstv/practicalfilament/filament/` Android Filament engine, material introspection, texture upload, and Android view integration
- `composeApp/src/iosMain/kotlin/dev/nstv/practicalfilament/filament/` Kotlin iOS engine hooks, Metal layer hosting, and the `FilamentBridgeProtocol`
- `iosApp/iosApp/FilamentBridge.mm` and related bridge files for native iOS interop with Filament C++
- `iosApp/iosApp/FilamentBridgeAdapter.swift` Swift adapter that conforms to the Kotlin-exported bridge protocol and delegates to the ObjC++ bridge
- `tools/filament/1.70.1/ios/` local Filament iOS distribution consumed by the Xcode project

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
- iOS currently uses the vendored SDK under `tools/filament/1.70.1/ios` with Xcode `HEADER_SEARCH_PATHS` / `LIBRARY_SEARCH_PATHS`

## Known Tooling Note

Gradle configuration currently emits an Android Gradle Plugin deprecation warning related to `android.dependency.excludeLibraryComponentsFromConstraints=true`. It does not block initialization, but it should be cleaned up before moving to AGP 10.

## Adding a New Screen

All screens live in `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/screen/`. To add one:

1. Create `YourScreen.kt` in that directory.
2. Add an entry to the `Screen` enum in `MainScreen.kt`.
3. Add a `when` branch in `MainScreen.kt` mapping the enum to the composable.

Reference screen: `RedballScreen.kt` (full setup: IBL, lights, material loading, arcball, gesture light, parameter UI). Simpler reference: `HelloTriangleScreen.kt` (custom geometry + runtime material).

### Screen Composable Pattern

```kotlin
@Composable
fun YourScreen(modifier: Modifier = Modifier) {
    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var renderableHandle by remember { mutableIntStateOf(0) }

    // Reactive side-effects: one LaunchedEffect per logical concern
    LaunchedEffect(filamentEngine, renderableHandle, someState) {
        val engine = filamentEngine ?: return@LaunchedEffect
        // update engine state, then:
        engine.requestFrame()
    }

    // Continuous animation loop
    LaunchedEffect(filamentEngine, renderableHandle) {
        while (true) {
            val nanos = withFrameNanos { it }
            val time = nanos / 1_000_000_000f
            engine?.setRenderableRotation(renderableHandle, 0f, time * speed)
        }
    }

    FilamentView(
        modifier = modifier.fillMaxSize(),
        camera = CameraConfig(position = Float3(0f, 0f, 4f), lookAt = Float3(0f, 0f, 0f)),
        lights = listOf(LightConfig(type = LightType.DIRECTIONAL, intensity = 75_000f)),
        backgroundColor = Color(0f, 0f, 0f, 1f),
        onEngineReady = { engine ->
            filamentEngine = engine
            // create renderables, load materials, etc.
        },
    )
}
```

Use `SampleScreenLayout` (in the same `screen/` package) for a standard split-view with a scrollable controls panel below the 3D view.

## FilamentEngine API Reference

Full interface: `composeApp/src/commonMain/kotlin/dev/nstv/practicalfilament/filament/FilamentEngine.kt`

### Renderables

| Method | Notes |
|--------|-------|
| `createSphereRenderable(matInstance, radius)` | 24×24 parametric sphere |
| `createPlaneRenderable(matInstance, width, height)` | 2-triangle plane |
| `createCubeRenderable(matInstance, size)` | 6-face cube |
| `createMorphRenderable(matInstance, MorphRenderableGeometry)` | Morph-target mesh; see `MorphRenderableGeometry.kt` |
| `createCustomRenderable(CustomRenderableConfig)` | Fully custom vertex/index buffers; see below |
| `updateVertexData(handle, ByteArray)` | Replace vertex buffer contents per-frame for dynamic geometry |
| `setMorphWeights(handle, FloatArray)` | Blend morph targets per-frame |
| `setRenderableTransform(handle, FloatArray)` | Apply 4×4 column-major transform matrix |
| `setRenderableRotation(handle, xDeg, yDeg)` | Convenience rotation helper |
| `removeRenderable(handle)` | Remove from scene |

### Custom Geometry (`CustomRenderableConfig`)

Defined in `DataTypes.kt`. Key fields:
- `vertexData: ByteArray` — interleaved vertex buffer
- `vertexCount: Int`
- `strideBytes: Int` — bytes per vertex
- `attributes: List<VertexAttributeLayout>` — supported: `POSITION`, `TANGENTS`, `UV0`, `COLOR`
- `indices: ShortArray` — triangle/line/point indices
- `materialInstanceHandle: Int`
- `boundingBox: BoundingBox(center, halfExtent)`
- `primitiveType: PrimitiveType` — `TRIANGLES`, `LINES`, or `POINTS`

Attribute data types: `FLOAT2`, `FLOAT3`, `FLOAT4`, `UBYTE4`.

`PrimitiveType.POINTS` renders 1-pixel points. For visible point-sprites, use billboard quads (TRIANGLES) and expand in CPU.

### Materials

| Method | Notes |
|--------|-------|
| `loadMaterial(path)` | Load a compiled `.filamat` from `composeResources/files/materials/` |
| `buildMaterial(source, shadingModel)` | Runtime compile (Android only; check `supportsMaterialBuilder`). `source` is just the `material()` function body. `shadingModel`: `"unlit"`, `"lit"`, `"cloth"`, `"subsurface"` |
| `getMaterialParameters(matHandle)` | Returns `List<MaterialParameterDefinition>` for introspection |
| `createMaterialInstance(matHandle)` | Returns instance handle |
| `setMaterialParameter(instance, MaterialParameter)` | Update scalar/vector/matrix/array uniform |
| `setTextureParameter(instance, name, textureHandle)` | Bind a texture sampler |

Helper `loadMaterialOnEngine(engine, Material)` in `material/` package handles load + instantiate + parameter defaults in one call.

Available `.filamat` files in `composeResources/files/materials/`: `plastic.filamat`, `texturedSample.filamat`, `sandboxLitFade.filamat`, `sandboxCloth.filamat`, `mirror.filamat`, `pageCurl.filamat`, `image.filamat`, `aiDefaultMat.filamat`.

### Lighting & Environment

| Method | Notes |
|--------|-------|
| `addLight(LightConfig)` | Types: `DIRECTIONAL`, `POINT`, `SPOT`, `SUN`. Returns handle |
| `removeLight(handle)` / `clearLights()` | |
| `loadIndirectLight(path)` + `setIndirectLight(handle, intensity)` | IBL from `.ktx` |
| `loadSkybox(path)` + `setSkybox(handle)` | Skybox from `.ktx` |

IBL + skybox assets are in `composeResources/files/envs/pillars_2k/`.

### Camera

`CameraConfig` fields: `position`, `lookAt`, `up` (all `Float3`), `fovDegrees`, `near`, `far`, `projectionType` (`PERSPECTIVE` or `ORTHO`), `orthoZoom`.

Pass to `FilamentView(camera=…)` for initial setup, or call `engine.updateCamera(config)` at any time.

### Dynamic Per-Frame Animation

Use `withFrameNanos` inside a `LaunchedEffect`:
```kotlin
LaunchedEffect(filamentEngine, renderableHandle) {
    while (true) {
        val dt = withFrameNanos { it } / 1e9f
        // mutate Compose state or call engine APIs directly
        filamentEngine?.requestFrame()
    }
}
```

For dynamic vertex positions (e.g. particle systems), update a `ByteArray` and call `engine.updateVertexData(handle, bytes)` each frame. Index buffer is fixed and rebuilt only when topology changes.

### Morph Targets

See `MorphRenderableGeometry.kt`. Provide base positions + a list of target position arrays. Call `engine.setMorphWeights(handle, floatArrayOf(w0, w1, …))` per frame. Framework auto-computes tangents per target. Reference: `MorphingScreen.kt`.

### Textures

| Method | Notes |
|--------|-------|
| `createTexture(width, height, ByteArray)` | Upload RGBA8 pixel data |
| `loadTexture(path)` | Load `.ktx` or image from resources |

Built-in generated textures (`BuiltInTexture` enum): `NONE`, `CHECKERBOARD`, `WHITE`, `RED`, `GRADIENT`.

## Filament Notes

- `MaterialViewerScreen` should derive editable controls from `getMaterialParameters(...)`; avoid reintroducing hardcoded material parameter lists in shared UI
- Sampler parameters are currently supported through generated built-in textures (`NONE`, `CHECKERBOARD`, `WHITE`, `RED`, `GRADIENT`) created in shared code and uploaded per-platform via `createTexture` / `setTextureParameter`
- The `mirror.filamat` sampler path currently uses static textures only. A real mirror effect would require render-to-texture / multi-pass rendering, which is not implemented
- Android supports typed scalar/vector/array material updates through `setMaterialParameter`
- iOS bridge support is narrower: scalar, vector, `mat3`, `mat4`, and texture/sampler binding are implemented, but repeated uniform arrays are still not supported by the ObjC++ bridge
- On iOS, `FilamentBridgeHolder.shared.bridge` must be assigned from Swift before composing `FilamentView`; `ContentView.swift` currently does this with `FilamentBridgeAdapter()`
- If you change bridge method signatures, keep the Kotlin `FilamentBridgeProtocol`, Swift adapter, ObjC++ header, and ObjC++ implementation in sync

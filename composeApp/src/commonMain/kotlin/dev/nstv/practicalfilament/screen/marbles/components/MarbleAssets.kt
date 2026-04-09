package dev.nstv.practicalfilament.screen.marbles.components

import androidx.compose.ui.graphics.Color
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.DefaultSkyboxColor
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.Material.SimpleMaterial

internal val MarbleUiBackground = Color(0xFF101721)
internal val MarbleUiText = Color(0xFFF4EEE4)
internal val MarbleUiMuted = Color(0xFFB6C1CD)
internal val MarbleUiAccent = Color(0xFFF6D28A)

private const val GlassMaterialPath = "files/materials/marbleGlass.filamat"

internal val SingleMarbleCamera = CameraConfig(
    position = Float3(0f, 0.1f, 4.5f),
    lookAt = Float3(0f, 0f, 0f),
    fovDegrees = 34.0,
)

internal val ComparisonMarbleCamera = CameraConfig(
    position = Float3(0f, 0.08f, 4.08f),
    lookAt = Float3(0f, 0f, 0f),
    fovDegrees = 33.0,
)

internal val PickerMarbleCamera = CameraConfig(
    position = Float3(0f, 0.06f, 4.15f),
    lookAt = Float3(0f, 0f, 0f),
    fovDegrees = 32.0,
)

internal val ButtonSurfaceCamera = CameraConfig(
    position = Float3(0f, 0f, 3.35f),
    lookAt = Float3(0f, 0f, 0f),
    fovDegrees = 31.0,
)

internal val SphereStepLights = listOf(
    LightConfig(
        type = LightType.DIRECTIONAL,
        color = FilamentColor(1f, 0.97f, 0.93f),
        intensity = 72_000f,
        direction = Float3(-0.28f, -0.38f, -1f),
    ),
)

internal val MarbleAliveLights = listOf(
    LightConfig(
        type = LightType.SUN,
        color = FilamentColor(1f, 0.95f, 0.89f),
        intensity = 110_000f,
        direction = Float3(0.45f, -1f, -0.72f),
        sunAngularRadius = 1.7f,
        sunHaloSize = 12f,
        sunHaloFalloff = 90f,
    ),
    LightConfig(
        type = LightType.POINT,
        color = FilamentColor(0.88f, 0.94f, 1f),
        intensity = 120_000f,
        position = Float3(-1.6f, 1.2f, 2.6f),
        falloffRadius = 8f,
    ),
)

internal val ThemePickerLights = listOf(
    LightConfig(
        type = LightType.SUN,
        color = FilamentColor(1f, 0.95f, 0.9f),
        intensity = 100_000f,
        direction = Float3(0.35f, -1f, -0.9f),
        sunAngularRadius = 1.5f,
        sunHaloSize = 10f,
        sunHaloFalloff = 85f,
    ),
    LightConfig(
        type = LightType.POINT,
        color = FilamentColor(0.85f, 0.9f, 1f),
        intensity = 75_000f,
        position = Float3(0f, 1.3f, 3.8f),
        falloffRadius = 10f,
    ),
)

internal val MarbleClayMaterial = SimpleMaterial(
    materialPath = "files/materials/marbleClay.filamat",
    label = "Clay",
    overrides = mapOf(
        "baseColor" to Float3(0.76f, 0.42f, 0.26f),
        "roughness" to 0.85f,
    ),
)

internal val MarbleGlassMaterial = SimpleMaterial(
    materialPath = GlassMaterialPath,
    label = "Glass",
    overrides = mapOf(
        "baseColor" to Float3(0.92f, 0.96f, 1f),
        "roughness" to 0.04f,
        "reflectance" to 1f,
        "alpha" to 0.035f,
        "clearCoat" to 1f,
        "clearCoatRoughness" to 0.03f,
        "rimStrength" to 0.32f,
        "rimPower" to 2.1f,
    ),
)

internal val MarbleStoneMaterial = SimpleMaterial(
    materialPath = "files/materials/marbleStone.filamat",
    label = "Stone",
    overrides = mapOf(
        "baseColor" to Float3(0.88f, 0.86f, 0.82f),
        "roughness" to 0.35f,
        "subsurfaceColor" to Float3(0.9f, 0.85f, 0.7f),
        "thickness" to 0.5f,
        "subsurfacePower" to 12f,
    ),
)

internal val MarbleMetalMaterial = SimpleMaterial(
    materialPath = "files/materials/marbleMetal.filamat",
    label = "Metal",
    overrides = mapOf(
        "baseColor" to Float3(0.85f, 0.85f, 0.88f),
        "roughness" to 0.08f,
        "metallic" to 1f,
        "reflectance" to 0.9f,
    ),
)

internal val MarbleCeramicMaterial = SimpleMaterial(
    materialPath = "files/materials/marbleCeramic.filamat",
    label = "Ceramic",
    overrides = mapOf(
        "baseColor" to Float3(0.22f, 0.45f, 0.72f),
        "roughness" to 0.4f,
        "clearCoat" to 1f,
        "clearCoatRoughness" to 0.04f,
    ),
)

internal val MarbleVelvetMaterial = SimpleMaterial(
    materialPath = "files/materials/marbleVelvet.filamat",
    label = "Velvet",
    overrides = mapOf(
        "baseColor" to Float3(0.55f, 0.08f, 0.22f),
        "roughness" to 0.75f,
        "sheenColor" to Float3(0.8f, 0.3f, 0.4f),
        "subsurfaceColor" to Float3(0.7f, 0.15f, 0.25f),
    ),
)

internal val MarblePresets = listOf(
    MarbleClayMaterial,
    MarbleGlassMaterial,
    MarbleStoneMaterial,
    MarbleMetalMaterial,
    MarbleCeramicMaterial,
    MarbleVelvetMaterial,
)

internal val NeutralSphereMaterial = SimpleMaterial(
    materialPath = "files/materials/plastic.filamat",
    label = "Neutral Sphere",
    overrides = mapOf(
        "baseColor" to Float3(0.72f, 0.74f, 0.78f),
        "roughness" to 0.28f,
        "clearCoat" to 0.2f,
        "clearCoatRoughness" to 0.18f,
    ),
)

internal const val CeramicPresetIndex = 4

package dev.nstv.practicalfilament.components

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import dev.nstv.practicalfilament.filament.material.Material


val MaterialFilesList = listOf(
    "plastic.filamat",
    "aiDefaultMat.filamat",
    "image.filamat",
    "sandboxLitFade.filamat",
    "sandboxCloth.filamat",
    "mirror.filamat",
)

val RedballMaterial = Material(
    fileName = "plastic.filamat",
    label = "redball",
    description = "Red plastic sphere based on Filament's redball sample, with clear coat tuned for a tight glossy highlight.",
    overrides = mapOf(
        "baseColor" to Float3(0.80f, 0.07f, 0.15f),
        "roughness" to 0.14f,
        "clearCoat" to 1f,
        "clearCoatRoughness" to 0.04f,
    ),
)

val MaterialOverridesList = listOf(
    RedballMaterial,
    Material(
        fileName = "aiDefaultMat.filamat",
        label = "aiDefaultMat",
        description = "The bundled marble material wrapped onto a sphere for a softer stone-like look.",
        overrides = mapOf(
            "baseColor" to Float3(0.72f, 0.74f, 0.70f),
            "roughness" to 0.58f,
            "sheenColor" to Float3(0.32f, 0.34f, 0.31f),
            "emissive" to Float4(0f, 0f, 0f, 1f),
        ),
    ),
    Material(
        fileName = "sandboxLitFade.filamat",
        label = "sandboxLitFade",
        description = "Glossy lit material on a sphere with the existing runtime controls.",
        overrides = mapOf(
            "baseColor" to Float3(0.78f, 0.84f, 0.92f),
            "roughness" to 0.18f,
            "metallic" to 0f,
            "reflectance" to 0.85f,
            "sheenColor" to Float3(0.18f, 0.22f, 0.30f),
            "sheenRoughness" to 0.24f,
            "clearCoat" to 1f,
            "clearCoatRoughness" to 0.06f,
            "anisotropy" to 0.06f,
            "emissive" to Float4(0f, 0f, 0f, 1f),
        ),
    ),
    Material(
        fileName = "sandboxCloth.filamat",
        label = "sandboxCloth",
        description = "The bundled cloth material wrapped onto a sphere for a softer stone-like look.",
        overrides = mapOf(
            "baseColor" to Float3(0.72f, 0.74f, 0.70f),
            "roughness" to 0.58f,
            "sheenColor" to Float3(0.32f, 0.34f, 0.31f),
            "emissive" to Float4(0f, 0f, 0f, 1f),
        ),
    ),
    Material(
        fileName = "sandboxLitFade.filamat",
        label = "mirror",
        description = "Mirror-like glossy sphere driven by surface lighting. True background reflections would require render-to-texture or an environment map.",
        overrides = mapOf(
            "baseColor" to Float3(0.96f, 0.97f, 0.98f),
            "roughness" to 0.02f,
            "metallic" to 0f,
            "reflectance" to 1f,
            "clearCoat" to 1f,
            "clearCoatRoughness" to 0.02f,
            "sheenColor" to Float3(0f, 0f, 0f),
            "sheenRoughness" to 0f,
            "anisotropy" to 0f,
            "emissive" to Float4(0f, 0f, 0f, 1f),
        ),
    ),
)

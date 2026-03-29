package dev.nstv.practicalfilament.components

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import dev.nstv.practicalfilament.filament.material.BuiltInTexture
import dev.nstv.practicalfilament.filament.material.Material


val MaterialFilesList = listOf(
    "aiDefaultMat.filamat",
    "sandboxLitFade.filamat",
    "sandboxCloth.filamat",
    "mirror.filamat",
)

val MaterialOverridesList = listOf(
    Material(
        fileName = "aiDefaultMat.filamat",
        label = "Marble",
        description = "The bundled marble material wrapped onto a sphere for a softer stone-like look.",
        overrides = mapOf(
            "baseColor" to Float4(0.72f, 0.74f, 0.70f, 1f),
            "roughness" to 0.58f,
            "sheenColor" to Float3(0.32f, 0.34f, 0.31f),
            "emissive" to Float4(0f, 0f, 0f, 1f),
        ),
    ),
    Material(
        fileName = "sandboxLitFade.filamat",
        label = "Lit Fade",
        description = "Glossy lit material on a sphere with the existing runtime controls.",
        overrides = mapOf(
            "baseColor" to Float4(0.78f, 0.84f, 0.92f, 1f),
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
        label = "Cloth",
        description = "The bundled cloth material wrapped onto a sphere for a softer stone-like look.",
        overrides = mapOf(
            "baseColor" to Float4(0.72f, 0.74f, 0.70f, 1f),
            "roughness" to 0.58f,
            "sheenColor" to Float3(0.32f, 0.34f, 0.31f),
            "emissive" to Float4(0f, 0f, 0f, 1f),
        ),
    ),
    Material(
        fileName = "mirror.filamat",
        label = "Mirror",
        description = "The existing mirror material using its bundled static texture path.",
        overrides = mapOf(
            "baseColor" to Float4(1f, 1f, 1f, 1f),
            "clearCoat" to 1f,
            "clearCoatRoughness" to 0.03f,
            "emissive" to Float4(0f, 0f, 0f, 1f),
            "albedo" to BuiltInTexture.GRADIENT,
        ),
    ),
)

package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import dev.nstv.practicalfilament.filament.material.SimpleMaterial

fun sandboxLitFadeMaterial(
    baseColor: Float3 = Float3(0.78f, 0.84f, 0.92f),
    roughness: Float = 0.18f,
    metallic: Float = 0f,
    reflectance: Float = 0.85f,
    sheenColor: Float3 = Float3(0.18f, 0.22f, 0.30f),
    sheenRoughness: Float = 0.24f,
    clearCoat: Float = 1f,
    clearCoatRoughness: Float = 0.06f,
    anisotropy: Float = 0.06f,
    emissive: Float4 = Float4(0f, 0f, 0f, 1f),
) = SimpleMaterial(
    label = "sandboxLitFade",
    materialPath = "files/materials/sandboxLitFade.filamat",
    overrides = mapOf(
        "baseColor" to baseColor,
        "roughness" to roughness,
        "metallic" to metallic,
        "reflectance" to reflectance,
        "sheenColor" to sheenColor,
        "sheenRoughness" to sheenRoughness,
        "clearCoat" to clearCoat,
        "clearCoatRoughness" to clearCoatRoughness,
        "anisotropy" to anisotropy,
        "emissive" to emissive,
    ),
)

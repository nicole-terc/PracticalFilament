package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.material.SimpleMaterial

fun morphingMaterial(
    baseColor: Float3 = Float3(0.16f, 0.69f, 0.92f),
    roughness: Float = 0.12f,
    clearCoat: Float = 1f,
    clearCoatRoughness: Float = 0.08f,
    metallic: Float = 0f,
) = SimpleMaterial(
    label = "morphing",
    materialPath = "files/materials/plastic.filamat",
    overrides = mapOf(
        "baseColor" to baseColor,
        "roughness" to roughness,
        "clearCoat" to clearCoat,
        "clearCoatRoughness" to clearCoatRoughness,
        "metallic" to metallic,
    ),
)

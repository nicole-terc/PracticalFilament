package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.material.Material.SimpleMaterial

fun redballMaterial(
    baseColor: Float3 = Float3(0.80f, 0.07f, 0.15f),
    roughness: Float = 0.14f,
    clearCoat: Float = 1f,
    clearCoatRoughness: Float = 0.04f,
) = SimpleMaterial(
    label = "redball",
    materialPath = "files/materials/plastic.filamat",
    overrides = mapOf(
        "baseColor" to baseColor,
        "roughness" to roughness,
        "clearCoat" to clearCoat,
        "clearCoatRoughness" to clearCoatRoughness,
    ),
)

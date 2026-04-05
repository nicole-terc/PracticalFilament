package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.material.Material.SimpleMaterial

fun sheepFluffMaterial(
    baseColor: Float3 = Float3(0.21f, 0.1f, 0.61f),
    roughness: Float = 0.27f,
    sheenColor: Float3 = Float3(0.29f, 0.72f, 0.37f),
    subsurfaceColor: Float3 = Float3(0.41f, 0.42f, 0.23f),
) = SimpleMaterial(
    label = "sheep-fluff",
    materialPath = "files/materials/sheepFluff.filamat",
    overrides = mapOf(
        "baseColor" to baseColor,
        "roughness" to roughness,
        "sheenColor" to sheenColor,
        "subsurfaceColor" to subsurfaceColor,
    ),
)

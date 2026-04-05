package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.Float4
import dev.nstv.practicalfilament.filament.material.SimpleMaterial

fun aiDefaultMatMaterial(
    baseColor: Float3 = Float3(0.72f, 0.74f, 0.70f),
    roughness: Float = 0.58f,
    sheenColor: Float3 = Float3(0.32f, 0.34f, 0.31f),
    emissive: Float4 = Float4(0f, 0f, 0f, 1f),
) = SimpleMaterial(
    label = "aiDefaultMat",
    materialPath = "files/materials/aiDefaultMat.filamat",
    overrides = mapOf(
        "baseColor" to baseColor,
        "roughness" to roughness,
        "sheenColor" to sheenColor,
        "emissive" to emissive,
    ),
)

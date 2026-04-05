package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.material.Material.SimpleMaterial

fun sheepBodyMaterial(
    baseColor: Float3 = Float3(0.27f, 0.27f, 0.27f),
    roughness: Float = 0.6f,
    metallic: Float = 0f,
) = SimpleMaterial(
    label = "sheep-body",
    materialPath = "files/materials/sheepBody.filamat",
    overrides = mapOf(
        "baseColor" to baseColor,
        "roughness" to roughness,
        "metallic" to metallic,
    ),
)

package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.filament.material.Material.SimpleMaterial

fun mirrorMaterial() = SimpleMaterial(
    label = "mirror",
    materialPath = "files/materials/mirror.filamat",
)

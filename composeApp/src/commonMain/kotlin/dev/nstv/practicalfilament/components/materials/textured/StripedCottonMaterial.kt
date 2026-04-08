package dev.nstv.practicalfilament.components.materials.textured

import dev.nstv.practicalfilament.filament.TextureColorFormat
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial.TextureBinding


private const val MaterialPath = "files/materials/mossMaterial.filamat"
private const val TextureRoot = "files/textures/Striped_cotton_01"
private const val TexturePrefix = "Striped_cotton_01"

fun stripedCottonMaterial() = TextureMaterial(
    label = "Stripped Cotton",
    materialPath = MaterialPath,
    overrides = mapOf(
        "uvScale" to 1.0f,
        "normalStrength" to 1.5f,
        "metallic" to 0.0f,
    ),
    textureBindings = listOf(
        TextureBinding(
            parameterName = "albedo",
            texturePath = "$TextureRoot/${TexturePrefix}_Color.ktx",
            label = "Albedo",
        ),
        TextureBinding(
            parameterName = "normal",
            texturePath = "$TextureRoot/${TexturePrefix}_Normal.ktx",
            label = "Normal",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "ao",
            texturePath = "$TextureRoot/${TexturePrefix}_AO.ktx",
            label = "Ambient Occlusion",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "roughness",
            texturePath = "$TextureRoot/${TexturePrefix}_Roughness.ktx",
            label = "Roughness",
            colorFormat = TextureColorFormat.RGBA8,
        ),
    ),
)

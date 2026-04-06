package dev.nstv.practicalfilament.components.materials.textured

import dev.nstv.practicalfilament.filament.TextureColorFormat
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial.TextureBinding


private const val MossMaterialPath = "files/materials/mossMaterial.filamat"
private const val MossTextureRoot = "files/textures/Moss_01"

fun mossMaterial() = TextureMaterial(
    label = "Moss",
    materialPath = MossMaterialPath,
    overrides = mapOf(
        "uvScale" to 2.0f,
        "normalStrength" to 1.5f,
        "metallic" to 0.0f,
    ),
    textureBindings = listOf(
        TextureBinding(
            parameterName = "albedo",
            texturePath = "$MossTextureRoot/Moss_01_Color.ktx",
            label = "Albedo",
        ),
        TextureBinding(
            parameterName = "normal",
            texturePath = "$MossTextureRoot/Moss_01_Normal.ktx",
            label = "Normal",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "ao",
            texturePath = "$MossTextureRoot/Moss_01_AO.ktx",
            label = "Ambient Occlusion",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "roughness",
            texturePath = "$MossTextureRoot/Moss_01_Roughness.ktx",
            label = "Roughness",
            colorFormat = TextureColorFormat.RGBA8,
        ),
    ),
)

package dev.nstv.practicalfilament.components.materials

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
    ),
    textureBindings = listOf(
        TextureBinding(
            parameterName = "baseColor",
            texturePath = "$MossTextureRoot/Moss_01_Color.png",
            label = "Base Color",
        ),
        TextureBinding(
            parameterName = "normal",
            texturePath = "$MossTextureRoot/Moss_01_Normal.png",
            label = "Normal",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "ao",
            texturePath = "$MossTextureRoot/Moss_01_AO.png",
            label = "Ambient Occlusion",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "roughness",
            texturePath = "$MossTextureRoot/Moss_01_Roughness.png",
            label = "Roughness",
            colorFormat = TextureColorFormat.RGBA8,
        ),
    ),
)

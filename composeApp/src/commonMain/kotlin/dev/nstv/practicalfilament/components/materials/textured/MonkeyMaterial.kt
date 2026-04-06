package dev.nstv.practicalfilament.components.materials.textured

import dev.nstv.practicalfilament.filament.TextureColorFormat
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial.TextureBinding


private const val MonkeyMaterialPath = "files/materials/monkeyTexture.filamat"
private const val MonkeyTextureRoot = "files/textures/monkey"

fun monkeyMaterial() = TextureMaterial(
    label = "Monkey",
    materialPath = MonkeyMaterialPath,
    overrides = mapOf(
        "clearCoat" to 1.0f,
    ),
    textureBindings = listOf(
        TextureBinding(
            parameterName = "albedo",
            texturePath = "$MonkeyTextureRoot/color.ktx",
            label = "Albedo",
        ),
        TextureBinding(
            parameterName = "normal",
            texturePath = "$MonkeyTextureRoot/normal.ktx",
            label = "Normal",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "ao",
            texturePath = "$MonkeyTextureRoot/ao.ktx",
            label = "Ambient Occlusion",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "roughness",
            texturePath = "$MonkeyTextureRoot/roughness.ktx",
            label = "Roughness",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "metallic",
            texturePath = "$MonkeyTextureRoot/metallic.ktx",
            label = "Metallic",
            colorFormat = TextureColorFormat.RGBA8,
        ),
    ),
)

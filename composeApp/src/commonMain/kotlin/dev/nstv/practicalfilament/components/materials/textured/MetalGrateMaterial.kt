package dev.nstv.practicalfilament.components.materials.textured

import dev.nstv.practicalfilament.filament.TextureColorFormat
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial.TextureBinding


private const val MaterialPath = "files/materials/textureMaterial.filamat"
private const val TexturePrefix = "metal_grate_rusty"
private const val TextureRoot = "files/textures/$TexturePrefix"

fun metalGrateMaterial() = TextureMaterial(
    label = "Metal Grate",
    materialPath = MaterialPath,
    overrides = mapOf(
        "uvScale" to 4.5f,
        "normalStrength" to 1.8f,
        "metallic" to 0.0f,
    ),
    textureBindings = listOf(
        TextureBinding(
            parameterName = "albedo",
            texturePath = "$TextureRoot/${TexturePrefix}_diff_2k.ktx",
            label = "Albedo",
        ),
        TextureBinding(
            parameterName = "normal",
            texturePath = "$TextureRoot/${TexturePrefix}_nor_gl_2k.ktx",
            label = "Normal (GL)",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "roughness",
            texturePath = "$TextureRoot/${TexturePrefix}_rough_2k.ktx",
            label = "Roughness",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "ao",
            texturePath = "$TextureRoot/${TexturePrefix}_ao_2k.ktx",
            label = "Ambient Occlusion",
            colorFormat = TextureColorFormat.RGBA8,
        ),
    ),
)

package dev.nstv.practicalfilament.components.materials.textured

import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.TextureColorFormat
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial.TextureBinding


private const val MaterialPath = "files/materials/ratMaterial.filamat"
private const val TextureRoot = "files/textures/street_rat_2k"

fun ratMaterial() = TextureMaterial(
    label = "Rat",
    materialPath = MaterialPath,
    overrides = mapOf(
        "normalStrength" to 1.0f,
        "subsurfaceColor" to Float3(0.06f, 0.05f, 0.04f),
        "subsurfacePower" to 6.0f,
    ),
    textureBindings = listOf(
        TextureBinding(
            parameterName = "albedo",
            texturePath = "$TextureRoot/street_rat_diff_2k.ktx",
            label = "Albedo",
        ),
        TextureBinding(
            parameterName = "normal",
            texturePath = "$TextureRoot/street_rat_nor_gl_2k.ktx",
            label = "Normal (GL)",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "roughness",
            texturePath = "$TextureRoot/street_rat_rough_2k.ktx",
            label = "Roughness",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "ao",
            texturePath = "$TextureRoot/street_rat_ao_2k.ktx",
            label = "Ambient Occlusion",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "thickness",
            texturePath = "$TextureRoot/street_rat_sss_2k.ktx",
            label = "Thickness (SSS)",
            colorFormat = TextureColorFormat.RGBA8,
        ),
    ),
)

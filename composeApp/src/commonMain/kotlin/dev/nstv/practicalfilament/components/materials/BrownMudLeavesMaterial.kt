package dev.nstv.practicalfilament.components.materials

import dev.nstv.practicalfilament.filament.TextureColorFormat
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial
import dev.nstv.practicalfilament.filament.material.Material.TextureMaterial.TextureBinding


private const val TextureMaterialPath = "files/materials/textureMaterial.filamat"
private const val BrownMudLeavesTextureRoot = "files/textures/brown_mud_leaves_01"

fun brownMudLeavesMaterial() = TextureMaterial(
    label = "Brown Mud Leaves",
    materialPath = TextureMaterialPath,
    overrides = mapOf(
        "uvScale" to 4.5f,
        "normalStrength" to 1.8f,
        "metallic" to 0.0f,
    ),
    textureBindings = listOf(
        TextureBinding(
            parameterName = "albedo",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_diff_2k.ktx",
            label = "Albedo",
        ),
        TextureBinding(
            parameterName = "normal",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_nor_gl_2k.ktx",
            label = "Normal (GL)",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "roughness",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_rough_2k.ktx",
            label = "Roughness",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "ao",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_ao_2k.ktx",
            label = "Ambient Occlusion",
            colorFormat = TextureColorFormat.RGBA8,
        ),
    ),
)

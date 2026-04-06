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
        "uvScale" to 5.5f,
        "normalStrength" to 2.4f,
    ),
    textureBindings = listOf(
        TextureBinding(
            parameterName = "albedoTex",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_diff_2k.jpg",
            label = "Base Color",
        ),
        TextureBinding(
            parameterName = "normalTex",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_nor_gl_2k.jpg",
            label = "Normal (GL)",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "roughnessTex",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_rough_2k.jpg",
            label = "Roughness",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "aoTex",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_ao_2k.jpg",
            label = "Ambient Occlusion",
            colorFormat = TextureColorFormat.RGBA8,
        ),
    ),
)

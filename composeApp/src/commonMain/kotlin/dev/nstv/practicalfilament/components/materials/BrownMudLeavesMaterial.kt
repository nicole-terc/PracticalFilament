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
    ),
    textureBindings = listOf(
        TextureBinding(
            parameterName = "baseColor",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_diff_2k.jpg",
            label = "Base Color",
        ),
        TextureBinding(
            parameterName = "normal",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_nor_gl_2k.jpg",
            label = "Normal (GL)",
            colorFormat = TextureColorFormat.RGBA8,
        ),
        TextureBinding(
            parameterName = "aoRoughnessMetallic",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_arm_2k.jpg",
            label = "ARM",
            colorFormat = TextureColorFormat.RGBA8,
        ),
    ),
)

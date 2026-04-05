package dev.nstv.practicalfilament.components.textures

private const val TextureMaterialPath = "files/materials/textureMaterial.filamat"
private const val BrownMudLeavesTextureRoot = "files/textures/brown_mud_leaves_01"

internal val BrownMudLeavesMaterial = TexturedSphereMaterial(
    label = "Brown Mud Leaves",
    materialPath = TextureMaterialPath,
    textureBindings = listOf(
        TextureBinding(
            parameterName = "baseColorMap",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_diff_2k.jpg",
            label = "Base Color",
        ),
        TextureBinding(
            parameterName = "normalMap",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_nor_gl_2k.jpg",
            label = "Normal (GL)",
        ),
        TextureBinding(
            parameterName = "roughnessMap",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_rough_2k.jpg",
            label = "Roughness",
        ),
        TextureBinding(
            parameterName = "aoMap",
            texturePath = "$BrownMudLeavesTextureRoot/brown_mud_leaves_01_ao_2k.jpg",
            label = "Ambient Occlusion",
        ),
    ),
)

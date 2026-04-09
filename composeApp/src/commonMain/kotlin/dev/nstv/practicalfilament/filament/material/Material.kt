package dev.nstv.practicalfilament.filament.material

import dev.nstv.practicalfilament.filament.TextureColorFormat

sealed interface Material {
    val label: String
    val materialPath: String
    val overrides: Map<String, Any>

    data class SimpleMaterial(
        override val label: String,
        override val materialPath: String,
        override val overrides: Map<String, Any> = emptyMap(),
    ) : Material

    data class TextureMaterial(
        override val label: String,
        override val materialPath: String,
        override val overrides: Map<String, Any> = emptyMap(),
        val textureBindings: List<TextureBinding>,
    ) : Material {
        data class TextureBinding(
            val parameterName: String,
            val texturePath: String,
            val label: String,
            val colorFormat: TextureColorFormat = TextureColorFormat.SRGB8_A8,
        )
    }
}

data class LoadedMaterial(
    val instanceHandle: Int,
    val definitions: List<MaterialParameterDefinition>,
    val parameters: Map<String, MaterialParameter>,
    val textureHandles: Map<String, Int> = emptyMap(),
    val isTexturedMaterial: Boolean = textureHandles.isNotEmpty(),
)

data class LoadedTextureParameterValue(
    val textureHandle: Int,
    val texturePath: String,
) {
    val displayName: String = texturePath
        .substringAfterLast('/')
        .substringBeforeLast('.')
}

internal fun buildMaterialParameters(
    definitions: List<MaterialParameterDefinition>,
    material: Material,
    loadedTextureParameters: Map<String, LoadedTextureParameterValue> = emptyMap(),
): Map<String, MaterialParameter> {
    return definitions.associate { definition ->
        val defaultValue = when {
            definition.name in material.overrides -> coerceOverrideValue(
                definition = definition,
                value = material.overrides.getValue(definition.name),
            ) ?: defaultMaterialParameter(definition).value

            definition.name in loadedTextureParameters -> loadedTextureParameters.getValue(definition.name)

            else -> defaultMaterialParameter(definition).value
        }
        definition.name to MaterialParameter(definition.name, defaultValue)
    }
}

internal fun coerceOverrideValue(
    definition: MaterialParameterDefinition,
    value: Any,
): Any? {
    return when (val type = definition.type) {
        is MaterialParameterType.Bool -> value as? Boolean
        is MaterialParameterType.Bool2 -> value as? dev.nstv.practicalfilament.filament.Bool2
        is MaterialParameterType.Bool3 -> value as? dev.nstv.practicalfilament.filament.Bool3
        is MaterialParameterType.Bool4 -> value as? dev.nstv.practicalfilament.filament.Bool4
        is MaterialParameterType.Float -> (value as? Number)?.toFloat()
        is MaterialParameterType.Float2 -> value as? dev.nstv.practicalfilament.filament.Float2
        is MaterialParameterType.Float3 -> coerceFloat3(value)
        is MaterialParameterType.Float4 -> coerceFloat4(value)
        is MaterialParameterType.Int -> (value as? Number)?.toInt()
        is MaterialParameterType.Int2 -> value as? dev.nstv.practicalfilament.filament.Int2
        is MaterialParameterType.Int3 -> value as? dev.nstv.practicalfilament.filament.Int3
        is MaterialParameterType.Int4 -> value as? dev.nstv.practicalfilament.filament.Int4
        is MaterialParameterType.UInt -> (value as? Number)?.toInt()?.takeIf { it >= 0 }?.toUInt()
        is MaterialParameterType.UInt2 -> value as? dev.nstv.practicalfilament.filament.UInt2
        is MaterialParameterType.UInt3 -> value as? dev.nstv.practicalfilament.filament.UInt3
        is MaterialParameterType.UInt4 -> value as? dev.nstv.practicalfilament.filament.UInt4
        is MaterialParameterType.Float3x3 -> value as? FloatArray
        is MaterialParameterType.Float4x4 -> value as? FloatArray
        is MaterialParameterType.Sampler2d,
        is MaterialParameterType.Sampler2dArray,
        is MaterialParameterType.SamplerCubemap,
        is MaterialParameterType.SamplerExternal -> when (value) {
            is BuiltInTexture -> value
            is LoadedTextureParameterValue -> value
            else -> null
        }
    }
}

private fun coerceFloat3(value: Any): dev.nstv.practicalfilament.filament.Float3? {
    return when (value) {
        is dev.nstv.practicalfilament.filament.Float3 -> value
        is dev.nstv.practicalfilament.filament.Float4 -> dev.nstv.practicalfilament.filament.Float3(
            value.x,
            value.y,
            value.z
        )

        else -> null
    }
}

private fun coerceFloat4(value: Any): dev.nstv.practicalfilament.filament.Float4? {
    return when (value) {
        is dev.nstv.practicalfilament.filament.Float3 -> dev.nstv.practicalfilament.filament.Float4(
            value.x,
            value.y,
            value.z,
            1f
        )

        is dev.nstv.practicalfilament.filament.Float4 -> value
        else -> null
    }
}

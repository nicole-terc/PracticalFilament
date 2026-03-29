package dev.nstv.practicalfilament.filament.material

import dev.nstv.practicalfilament.filament.FilamentEngine
import practicalfilament.composeapp.generated.resources.Res

data class Material(
    val fileName: String,
    val label: String,
    val description: String,
    val overrides: Map<String, Any>,
)

fun loadMaterialOnEngine(
    engine: FilamentEngine,
    fileName: String,
): Triple<Int, List<MaterialParameterDefinition>, Map<String, MaterialParameter>> {
    val materialHandle = engine.loadMaterial(Res.getUri("files/$fileName"))
    val definitions = engine.getMaterialParameters(materialHandle)
    val instanceHandle = engine.createMaterialInstance(materialHandle)
    val parameters = definitions.associate { definition ->
        definition.name to defaultMaterialParameter(definition)
    }
    return Triple(instanceHandle, definitions, parameters)
}

fun loadMaterialOnEngine(
    engine: FilamentEngine,
    material: Material,
): Triple<Int, List<MaterialParameterDefinition>, Map<String, MaterialParameter>> {
    val materialHandle = engine.loadMaterial(Res.getUri("files/${material.fileName}"))
    val definitions = engine.getMaterialParameters(materialHandle)
    val instanceHandle = engine.createMaterialInstance(materialHandle)
    return Triple(instanceHandle, definitions, buildMaterialParameters(definitions, material))
}

private fun buildMaterialParameters(
    definitions: List<MaterialParameterDefinition>,
    material: Material,
): Map<String, MaterialParameter> {
    return definitions.associate { definition ->
        val defaultValue = when {
            definition.name in material.overrides -> coerceOverrideValue(
                definition = definition,
                value = material.overrides.getValue(definition.name),
            ) ?: defaultMaterialParameter(definition).value
            definition.type is MaterialParameterType.Sampler2d ||
                    definition.type is MaterialParameterType.Sampler2dArray ||
                    definition.type is MaterialParameterType.SamplerCubemap ||
                    definition.type is MaterialParameterType.SamplerExternal -> BuiltInTexture.CHECKERBOARD

            else -> defaultMaterialParameter(definition).value
        }
        definition.name to MaterialParameter(definition.name, defaultValue)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
internal fun coerceOverrideValue(
    definition: MaterialParameterDefinition,
    value: Any,
): Any? {
    return when (val type = definition.type) {
        is MaterialParameterType.Bool -> if (type.arraySize == 1) {
            value as? Boolean
        } else {
            value as? BooleanArray
        }

        is MaterialParameterType.Bool2 -> if (type.arraySize == 1) {
            when (value) {
                is dev.nstv.practicalfilament.filament.Bool2 -> value
                is BooleanArray -> value.takeIf { it.size == 2 }?.let { bools ->
                    dev.nstv.practicalfilament.filament.Bool2(bools[0], bools[1])
                }
                else -> null
            }
        } else {
            value as? BooleanArray
        }

        is MaterialParameterType.Bool3 -> if (type.arraySize == 1) {
            when (value) {
                is dev.nstv.practicalfilament.filament.Bool3 -> value
                is BooleanArray -> value.takeIf { it.size == 3 }?.let { bools ->
                    dev.nstv.practicalfilament.filament.Bool3(bools[0], bools[1], bools[2])
                }
                else -> null
            }
        } else {
            value as? BooleanArray
        }

        is MaterialParameterType.Bool4 -> if (type.arraySize == 1) {
            when (value) {
                is dev.nstv.practicalfilament.filament.Bool4 -> value
                is BooleanArray -> value.takeIf { it.size == 4 }?.let { bools ->
                    dev.nstv.practicalfilament.filament.Bool4(bools[0], bools[1], bools[2], bools[3])
                }
                else -> null
            }
        } else {
            value as? BooleanArray
        }

        is MaterialParameterType.Float -> if (type.arraySize == 1) {
            (value as? Number)?.toFloat()
        } else {
            value as? FloatArray
        }

        is MaterialParameterType.Float2 -> if (type.arraySize == 1) {
            coerceFloat2(value)
        } else {
            value as? FloatArray
        }

        is MaterialParameterType.Float3 -> if (type.arraySize == 1) {
            coerceFloat3(value)
        } else {
            value as? FloatArray
        }

        is MaterialParameterType.Float4 -> if (type.arraySize == 1) {
            coerceFloat4(value)
        } else {
            value as? FloatArray
        }

        is MaterialParameterType.Int -> if (type.arraySize == 1) {
            (value as? Number)?.toInt()
        } else {
            value as? IntArray
        }

        is MaterialParameterType.Int2 -> if (type.arraySize == 1) {
            when (value) {
                is dev.nstv.practicalfilament.filament.Int2 -> value
                is IntArray -> value.takeIf { it.size == 2 }?.let { ints ->
                    dev.nstv.practicalfilament.filament.Int2(ints[0], ints[1])
                }
                else -> null
            }
        } else {
            value as? IntArray
        }

        is MaterialParameterType.Int3 -> if (type.arraySize == 1) {
            when (value) {
                is dev.nstv.practicalfilament.filament.Int3 -> value
                is IntArray -> value.takeIf { it.size == 3 }?.let { ints ->
                    dev.nstv.practicalfilament.filament.Int3(ints[0], ints[1], ints[2])
                }
                else -> null
            }
        } else {
            value as? IntArray
        }

        is MaterialParameterType.Int4 -> if (type.arraySize == 1) {
            when (value) {
                is dev.nstv.practicalfilament.filament.Int4 -> value
                is IntArray -> value.takeIf { it.size == 4 }?.let { ints ->
                    dev.nstv.practicalfilament.filament.Int4(ints[0], ints[1], ints[2], ints[3])
                }
                else -> null
            }
        } else {
            value as? IntArray
        }

        is MaterialParameterType.UInt -> if (type.arraySize == 1) {
            (value as? Number)?.toInt()?.takeIf { it >= 0 }?.toUInt()
        } else {
            value as? UIntArray
        }

        is MaterialParameterType.UInt2 -> if (type.arraySize == 1) {
            when (value) {
                is dev.nstv.practicalfilament.filament.UInt2 -> value
                is UIntArray -> value.takeIf { it.size == 2 }?.let { ints ->
                    dev.nstv.practicalfilament.filament.UInt2(ints[0], ints[1])
                }
                else -> null
            }
        } else {
            value as? UIntArray
        }

        is MaterialParameterType.UInt3 -> if (type.arraySize == 1) {
            when (value) {
                is dev.nstv.practicalfilament.filament.UInt3 -> value
                is UIntArray -> value.takeIf { it.size == 3 }?.let { ints ->
                    dev.nstv.practicalfilament.filament.UInt3(ints[0], ints[1], ints[2])
                }
                else -> null
            }
        } else {
            value as? UIntArray
        }

        is MaterialParameterType.UInt4 -> if (type.arraySize == 1) {
            when (value) {
                is dev.nstv.practicalfilament.filament.UInt4 -> value
                is UIntArray -> value.takeIf { it.size == 4 }?.let { ints ->
                    dev.nstv.practicalfilament.filament.UInt4(ints[0], ints[1], ints[2], ints[3])
                }
                else -> null
            }
        } else {
            value as? UIntArray
        }

        is MaterialParameterType.Float3x3 -> (value as? FloatArray)?.takeIf { it.size == 9 }
        is MaterialParameterType.Float4x4 -> (value as? FloatArray)?.takeIf { it.size == 16 }
        is MaterialParameterType.Sampler2d,
        is MaterialParameterType.Sampler2dArray,
        is MaterialParameterType.SamplerCubemap,
        is MaterialParameterType.SamplerExternal -> value as? BuiltInTexture
    }
}

private fun coerceFloat2(value: Any): dev.nstv.practicalfilament.filament.Float2? {
    return when (value) {
        is dev.nstv.practicalfilament.filament.Float2 -> value
        is dev.nstv.practicalfilament.filament.Float3 -> dev.nstv.practicalfilament.filament.Float2(value.x, value.y)
        is dev.nstv.practicalfilament.filament.Float4 -> dev.nstv.practicalfilament.filament.Float2(value.x, value.y)
        is FloatArray -> value.takeIf { it.size >= 2 }?.let { floats ->
            dev.nstv.practicalfilament.filament.Float2(floats[0], floats[1])
        }
        else -> null
    }
}

private fun coerceFloat3(value: Any): dev.nstv.practicalfilament.filament.Float3? {
    return when (value) {
        is dev.nstv.practicalfilament.filament.Float2 -> dev.nstv.practicalfilament.filament.Float3(value.x, value.y, 0f)
        is dev.nstv.practicalfilament.filament.Float3 -> value
        is dev.nstv.practicalfilament.filament.Float4 -> dev.nstv.practicalfilament.filament.Float3(value.x, value.y, value.z)
        is FloatArray -> value.takeIf { it.size >= 3 }?.let { floats ->
            dev.nstv.practicalfilament.filament.Float3(floats[0], floats[1], floats[2])
        }
        else -> null
    }
}

private fun coerceFloat4(value: Any): dev.nstv.practicalfilament.filament.Float4? {
    return when (value) {
        is dev.nstv.practicalfilament.filament.Float2 -> dev.nstv.practicalfilament.filament.Float4(value.x, value.y, 0f, 1f)
        is dev.nstv.practicalfilament.filament.Float3 -> dev.nstv.practicalfilament.filament.Float4(value.x, value.y, value.z, 1f)
        is dev.nstv.practicalfilament.filament.Float4 -> value
        is FloatArray -> value.takeIf { it.size >= 4 }?.let { floats ->
            dev.nstv.practicalfilament.filament.Float4(floats[0], floats[1], floats[2], floats[3])
        }
        else -> null
    }
}

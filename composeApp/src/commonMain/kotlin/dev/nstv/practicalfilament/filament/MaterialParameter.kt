package dev.nstv.practicalfilament.filament

data class MaterialParameter(
    val name: String,
    val value: Any,
)

enum class MaterialParameterPrecision {
    LOW,
    MEDIUM,
    HIGH,
    DEFAULT,
}

enum class SamplerFormat {
    INT,
    FLOAT,
}

sealed class MaterialParameterType(open val arraySize: kotlin.Int = 1) {
    data class Bool(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Bool2(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Bool3(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Bool4(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Float(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Float2(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Float3(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Float4(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Int(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Int2(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Int3(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Int4(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class UInt(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class UInt2(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class UInt3(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class UInt4(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Float3x3(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Float4x4(override val arraySize: kotlin.Int = 1) : MaterialParameterType(arraySize)
    data class Sampler2d(
        val format: SamplerFormat = SamplerFormat.FLOAT,
        val multisample: Boolean = false,
        val filterable: Boolean = true,
    ) : MaterialParameterType(1)
    data class Sampler2dArray(
        val format: SamplerFormat = SamplerFormat.FLOAT,
        val multisample: Boolean = false,
        val filterable: Boolean = true,
    ) : MaterialParameterType(1)
    data class SamplerExternal(
        val format: SamplerFormat = SamplerFormat.FLOAT,
        val multisample: Boolean = false,
        val filterable: Boolean = true,
    ) : MaterialParameterType(1)
    data class SamplerCubemap(
        val format: SamplerFormat = SamplerFormat.FLOAT,
        val multisample: Boolean = false,
        val filterable: Boolean = true,
    ) : MaterialParameterType(1)

    companion object {
        fun fromRawTypeName(rawTypeName: String, arraySize: kotlin.Int = 1): MaterialParameterType {
            return when (rawTypeName.trim().uppercase()) {
                "BOOL" -> Bool(arraySize)
                "BOOL2" -> Bool2(arraySize)
                "BOOL3" -> Bool3(arraySize)
                "BOOL4" -> Bool4(arraySize)
                "FLOAT" -> Float(arraySize)
                "FLOAT2" -> Float2(arraySize)
                "FLOAT3" -> Float3(arraySize)
                "FLOAT4" -> Float4(arraySize)
                "INT" -> Int(arraySize)
                "INT2" -> Int2(arraySize)
                "INT3" -> Int3(arraySize)
                "INT4" -> Int4(arraySize)
                "UINT" -> UInt(arraySize)
                "UINT2" -> UInt2(arraySize)
                "UINT3" -> UInt3(arraySize)
                "UINT4" -> UInt4(arraySize)
                "MAT3", "FLOAT3X3" -> Float3x3(arraySize)
                "MAT4", "FLOAT4X4" -> Float4x4(arraySize)
                "SAMPLER_2D", "SAMPLER2D" -> Sampler2d()
                "SAMPLER_2D_ARRAY", "SAMPLER2DARRAY" -> Sampler2dArray()
                "SAMPLER_EXTERNAL", "SAMPLEREXTERNAL" -> SamplerExternal()
                "SAMPLER_CUBEMAP", "SAMPLERCUBEMAP" -> SamplerCubemap()
                else -> error("Unsupported material parameter type: $rawTypeName")
            }
        }
    }
}

data class MaterialParameterDefinition(
    val name: String,
    val type: MaterialParameterType,
    val precision: MaterialParameterPrecision = MaterialParameterPrecision.DEFAULT,
)


@OptIn(ExperimentalUnsignedTypes::class)
fun defaultMaterialParameter(definition: MaterialParameterDefinition): MaterialParameter {
    val value = when (val type = definition.type) {
        is MaterialParameterType.Bool -> if (type.arraySize == 1) false else BooleanArray(type.arraySize)
        is MaterialParameterType.Bool2 -> if (type.arraySize == 1) Bool2(
            false,
            false
        ) else BooleanArray(type.arraySize * 2)

        is MaterialParameterType.Bool3 -> if (type.arraySize == 1) Bool3(
            false,
            false,
            false
        ) else BooleanArray(type.arraySize * 3)

        is MaterialParameterType.Bool4 -> if (type.arraySize == 1) Bool4(
            false,
            false,
            false,
            false
        ) else BooleanArray(type.arraySize * 4)

        is MaterialParameterType.Float -> if (type.arraySize == 1) 0.5f else FloatArray(type.arraySize) { 0.5f }
        is MaterialParameterType.Float2 -> if (type.arraySize == 1) Float2(
            0.5f,
            0.5f
        ) else FloatArray(type.arraySize * 2) { 0.5f }

        is MaterialParameterType.Float3 -> if (type.arraySize == 1) Float3(
            0.5f,
            0.5f,
            0.5f
        ) else FloatArray(type.arraySize * 3) { 0.5f }

        is MaterialParameterType.Float4 -> if (type.arraySize == 1) Float4(
            0.5f,
            0.5f,
            0.5f,
            0.5f
        ) else FloatArray(type.arraySize * 4) { 0.5f }

        is MaterialParameterType.Int -> if (type.arraySize == 1) 0 else IntArray(type.arraySize)
        is MaterialParameterType.Int2 -> if (type.arraySize == 1) Int2(
            0,
            0
        ) else IntArray(type.arraySize * 2)

        is MaterialParameterType.Int3 -> if (type.arraySize == 1) Int3(
            0,
            0,
            0
        ) else IntArray(type.arraySize * 3)

        is MaterialParameterType.Int4 -> if (type.arraySize == 1) Int4(0, 0, 0, 0) else IntArray(
            type.arraySize * 4
        )

        is MaterialParameterType.UInt -> if (type.arraySize == 1) 0u else UIntArray(type.arraySize)
        is MaterialParameterType.UInt2 -> if (type.arraySize == 1) UInt2(
            0u,
            0u
        ) else UIntArray(type.arraySize * 2)

        is MaterialParameterType.UInt3 -> if (type.arraySize == 1) UInt3(0u, 0u, 0u) else UIntArray(
            type.arraySize * 3
        )

        is MaterialParameterType.UInt4 -> if (type.arraySize == 1) UInt4(
            0u,
            0u,
            0u,
            0u
        ) else UIntArray(type.arraySize * 4)

        is MaterialParameterType.Float3x3 -> identityMat3()
        is MaterialParameterType.Float4x4 -> identityMat4()
        is MaterialParameterType.Sampler2d,
        is MaterialParameterType.Sampler2dArray,
        is MaterialParameterType.SamplerExternal,
        is MaterialParameterType.SamplerCubemap -> FloatArray(0)
    }
    return MaterialParameter(definition.name, value)
}

fun identityMat3(): FloatArray = floatArrayOf(
    1f, 0f, 0f,
    0f, 1f, 0f,
    0f, 0f, 1f,
)

fun identityMat4(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)


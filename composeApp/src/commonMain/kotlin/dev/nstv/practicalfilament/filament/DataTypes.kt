package dev.nstv.practicalfilament.filament

import androidx.compose.ui.graphics.Color

data class Float3(val x: Float, val y: Float, val z: Float)

data class Float4(val x: Float, val y: Float, val z: Float, val w: Float)

data class Float2(val x: Float, val y: Float)

data class Int2(val x: Int, val y: Int)

data class Int3(val x: Int, val y: Int, val z: Int)

data class Int4(val x: Int, val y: Int, val z: Int, val w: Int)

data class UInt2(val x: UInt, val y: UInt)

data class UInt3(val x: UInt, val y: UInt, val z: UInt)

data class UInt4(val x: UInt, val y: UInt, val z: UInt, val w: UInt)

data class Bool2(val x: Boolean, val y: Boolean)

data class Bool3(val x: Boolean, val y: Boolean, val z: Boolean)

data class Bool4(val x: Boolean, val y: Boolean, val z: Boolean, val w: Boolean)

data class FilamentColor(val r: Float, val g: Float, val b: Float, val a: Float = 1f)

enum class ProjectionType {
    PERSPECTIVE,
    ORTHO,
}

enum class VertexAttribute {
    POSITION,
    TANGENTS,
    UV0,
    COLOR,
}

enum class AttributeDataType {
    FLOAT2,
    FLOAT3,
    FLOAT4,
    UBYTE4,
}

enum class PrimitiveType {
    TRIANGLES,
    LINES,
    POINTS,
}

enum class TextureColorFormat {
    RGBA8,
    SRGB8_A8,
}

enum class MaterialBlendingMode {
    OPAQUE,
    TRANSPARENT,
    ADD,
    MASKED,
    FADE,
    MULTIPLY,
    SCREEN,
}

data class VertexAttributeLayout(
    val attribute: VertexAttribute,
    val type: AttributeDataType,
    val offsetBytes: Int,
    val normalized: Boolean = false,
)

data class BoundingBox(
    val center: Float3,
    val halfExtent: Float3,
)

data class FilamentPickResult(
    val renderableHandle: Int,
    val depth: Float,
    val fragCoords: Float3,
)

data class CustomRenderableConfig(
    val vertexData: ByteArray,
    val vertexCount: Int,
    val strideBytes: Int,
    val attributes: List<VertexAttributeLayout>,
    val indices: ShortArray,
    val materialInstanceHandle: Int,
    val boundingBox: BoundingBox,
    val primitiveType: PrimitiveType = PrimitiveType.TRIANGLES,
)

data class ViewportConfig(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

fun Color.toFilamentColor(): FilamentColor = FilamentColor(
    r = red,
    g = green,
    b = blue,
    a = alpha,
)

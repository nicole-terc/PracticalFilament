package dev.nstv.practicalfilament.filament

import android.opengl.Matrix
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder


private const val FILAMESH_FILE_IDENTIFIER = "FILAMESH"
private const val SUPPORTED_VERSION = 1

private const val FLAG_SNORM16_UV = 0x2
private const val FLAG_COMPRESSED = 0x4

object MeshLoader {
    data class FilameshData(
        val entity: Int,
        val indexBuffer: IndexBuffer,
        val vertexBuffer: VertexBuffer,
    )

    private class Header {
        var partCount = 0
        var aabb = Box()
        var isSnorm16UV = false
        var offsetPosition = 0;
        var stridePosition = 0
        var offsetTangents = 0;
        var strideTangents = 0
        var offsetColor = 0;
        var strideColor = 0
        var offsetUV0 = 0;
        var strideUV0 = 0
        var vertexCount = 0
        var vertexSizeBytes = 0
        var indexType = 0
        var indexCount = 0
        var indexSizeBytes = 0
    }

    private data class Part(
        val offset: Int,
        val count: Int,
        val minIndex: Int,
        val maxIndex: Int
    )

    fun loadFilamesh(
        buffer: ByteBuffer,
        engine: Engine,
        materialInstance: MaterialInstance,
        scale: Float = 1f,
    ): FilameshData? {
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val header = readHeader(buffer) ?: return null

        val vertexData = readSlice(buffer, header.vertexSizeBytes)
        val indexData = readSlice(buffer, header.indexSizeBytes)
        val parts = readParts(buffer, header.partCount)

        val indexBuffer = createIndexBuffer(engine, header, indexData)
        val vertexBuffer = createVertexBuffer(engine, header, vertexData)
        val entity =
            createRenderable(engine, header, indexBuffer, vertexBuffer, parts, materialInstance)

        if (scale != 1f) {
            val ti = engine.transformManager.getInstance(entity)
            val transform = FloatArray(16)
            Matrix.setIdentityM(transform, 0)
            Matrix.scaleM(transform, 0, scale, scale, scale)
            engine.transformManager.setTransform(ti, transform)
        }

        return FilameshData(entity, indexBuffer, vertexBuffer)
    }

    private fun readHeader(buffer: ByteBuffer): Header? {
        val magicBytes = ByteArray(FILAMESH_FILE_IDENTIFIER.length)
        buffer.get(magicBytes)
        if (String(magicBytes, Charsets.US_ASCII) != FILAMESH_FILE_IDENTIFIER) return null

        val version = buffer.int
        if (version != SUPPORTED_VERSION) return null

        val flags: Int

        return Header().apply {
            partCount = buffer.int
            val cx = buffer.float;
            val cy = buffer.float;
            val cz = buffer.float
            val hx = buffer.float;
            val hy = buffer.float;
            val hz = buffer.float
            aabb = Box(cx, cy, cz, hx, hy, hz)
            flags = buffer.int
            if (flags and FLAG_COMPRESSED != 0) return null
            isSnorm16UV = flags and FLAG_SNORM16_UV != 0
            offsetPosition = buffer.int; stridePosition = buffer.int
            offsetTangents = buffer.int; strideTangents = buffer.int
            offsetColor = buffer.int; strideColor = buffer.int
            offsetUV0 = buffer.int; strideUV0 = buffer.int
            buffer.int; buffer.int // offsetUV1, strideUV1 — unused
            vertexCount = buffer.int
            vertexSizeBytes = buffer.int
            indexType = buffer.int
            indexCount = buffer.int
            indexSizeBytes = buffer.int
        }
    }

    private fun readSlice(buffer: ByteBuffer, sizeBytes: Int): ByteBuffer {
        val slice = ByteBuffer.allocateDirect(sizeBytes).order(ByteOrder.LITTLE_ENDIAN)
        val src = buffer.duplicate().apply { limit(buffer.position() + sizeBytes) }
        slice.put(src)
        slice.flip()
        buffer.position(buffer.position() + sizeBytes)
        return slice
    }

    private fun readParts(buffer: ByteBuffer, count: Int): List<Part> {
        return List(count) {
            val offset = buffer.int
            val indexCount = buffer.int
            val minIndex = buffer.int
            val maxIndex = buffer.int
            buffer.int                          // materialID — we apply a single material instance
            buffer.position(buffer.position() + 24) // skip per-part aabb
            Part(offset, indexCount, minIndex, maxIndex)
        }
    }

    private fun createIndexBuffer(engine: Engine, header: Header, data: ByteBuffer): IndexBuffer {
        val indexType = if (header.indexType == 1) {
            IndexBuffer.Builder.IndexType.USHORT
        } else {
            IndexBuffer.Builder.IndexType.UINT
        }
        return IndexBuffer.Builder()
            .indexCount(header.indexCount)
            .bufferType(indexType)
            .build(engine)
            .apply { setBuffer(engine, data) }
    }

    private fun createVertexBuffer(engine: Engine, header: Header, data: ByteBuffer): VertexBuffer {
        val uvAttrType = if (header.isSnorm16UV) {
            VertexBuffer.AttributeType.SHORT2
        } else {
            VertexBuffer.AttributeType.HALF2
        }
        return VertexBuffer.Builder()
            .vertexCount(header.vertexCount)
            .bufferCount(1)
            .attribute(
                VertexBuffer.VertexAttribute.POSITION,
                0,
                VertexBuffer.AttributeType.HALF4,
                header.offsetPosition,
                header.stridePosition
            )
            .normalized(VertexBuffer.VertexAttribute.TANGENTS)
            .attribute(
                VertexBuffer.VertexAttribute.TANGENTS,
                0,
                VertexBuffer.AttributeType.SHORT4,
                header.offsetTangents,
                header.strideTangents
            )
            .normalized(VertexBuffer.VertexAttribute.COLOR)
            .attribute(
                VertexBuffer.VertexAttribute.COLOR,
                0,
                VertexBuffer.AttributeType.UBYTE4,
                header.offsetColor,
                header.strideColor
            )
            .attribute(
                VertexBuffer.VertexAttribute.UV0,
                0,
                uvAttrType,
                header.offsetUV0,
                header.strideUV0
            )
            .normalized(VertexBuffer.VertexAttribute.UV0, header.isSnorm16UV)
            .build(engine)
            .apply { setBufferAt(engine, 0, data) }
    }

    private fun createRenderable(
        engine: Engine,
        header: Header,
        indexBuffer: IndexBuffer,
        vertexBuffer: VertexBuffer,
        parts: List<Part>,
        materialInstance: MaterialInstance,
    ): Int {
        val builder = RenderableManager.Builder(header.partCount).boundingBox(header.aabb)
        parts.forEachIndexed { i, part ->
            builder.geometry(
                i,
                RenderableManager.PrimitiveType.TRIANGLES,
                vertexBuffer,
                indexBuffer,
                part.offset,
                part.minIndex,
                part.maxIndex,
                part.count
            )
            builder.material(i, materialInstance)
        }
        return EntityManager.get().create().apply { builder.build(engine, this) }
    }
}

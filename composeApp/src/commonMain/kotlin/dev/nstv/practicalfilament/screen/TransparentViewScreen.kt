// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-transparent-view
package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.ProjectionType
import dev.nstv.practicalfilament.filament.PrimitiveType
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.theme.Grid
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import practicalfilament.composeapp.generated.resources.Res

private const val TransparentViewMaterialPath = "files/materials/bakedColor.filamat"

@Composable
fun TransparentViewScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine, renderableHandle) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        while (true) {
            val elapsedNanos = withFrameNanos { it }
            val angleRadians = -((elapsedNanos % TransparentRotationDurationNanos).toDouble() /
                TransparentRotationDurationNanos.toDouble() * PI * 2.0)
            currentEngine.setRenderableTransform(renderableHandle, rotationZMatrix(angleRadians.toFloat()))
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Transparent View",
        view = {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Compose",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Grid.Two),
                    style = MaterialTheme.typography.headlineMedium,
                )
                FilamentView(
                    modifier = Modifier.fillMaxSize(),
                    camera = CameraConfig(
                        position = Float3(0f, 0f, 4f),
                        lookAt = Float3(0f, 0f, 0f),
                        projectionType = ProjectionType.ORTHO,
                        orthoZoom = 1.5,
                        near = 0.0,
                        far = 10.0,
                    ),
                    lights = emptyList(),
                    backgroundColor = Color(0f, 0f, 0f, 0f),
                    isOpaque = false,
                    onEngineReady = { readyEngine ->
                        engine = readyEngine
                        val materialHandle = readyEngine.loadMaterial(Res.getUri(TransparentViewMaterialPath))
                        if (materialHandle <= 0) {
                            notice = "Failed to load the baked vertex-color material."
                            return@FilamentView
                        }
                        val instanceHandle = readyEngine.createMaterialInstance(materialHandle)
                        renderableHandle = readyEngine.createCustomRenderable(
                            CustomRenderableConfig(
                                vertexData = buildTransparentTriangleVertexData(),
                                vertexCount = 3,
                                strideBytes = 16,
                                attributes = listOf(
                                    VertexAttributeLayout(
                                        attribute = VertexAttribute.POSITION,
                                        type = AttributeDataType.FLOAT3,
                                        offsetBytes = 0,
                                    ),
                                    VertexAttributeLayout(
                                        attribute = VertexAttribute.COLOR,
                                        type = AttributeDataType.UBYTE4,
                                        offsetBytes = 12,
                                        normalized = true,
                                    ),
                                ),
                                indices = shortArrayOf(0, 1, 2),
                                materialInstanceHandle = instanceHandle,
                                boundingBox = BoundingBox(
                                    center = Float3(0f, 0f, 0f),
                                    halfExtent = Float3(1f, 1f, 0.01f),
                                ),
                                primitiveType = PrimitiveType.TRIANGLES,
                            ),
                        )
                        if (renderableHandle <= 0) {
                            notice = "Custom renderables are not available on this platform yet."
                        }
                    },
                )
            }
        },
        controls = {
            notice?.let { SampleNotice(it) }
        },
    )
}

private const val TransparentRotationDurationNanos = 4_000_000_000L

private fun buildTransparentTriangleVertexData(): ByteArray {
    val vertexSize = 16
    val bytes = ByteArray(vertexSize * 3)
    val angle120 = (PI * 2.0 / 3.0).toFloat()
    val angle240 = (PI * 4.0 / 3.0).toFloat()

    putVertex(bytes, 0, 1f, 0f, 0f, 0xffff0000.toInt())
    putVertex(bytes, vertexSize, cos(angle120), sin(angle120), 0f, 0xff00ff00.toInt())
    putVertex(bytes, vertexSize * 2, cos(angle240), sin(angle240), 0f, 0xff0000ff.toInt())
    return bytes
}

private fun putVertex(bytes: ByteArray, offset: Int, x: Float, y: Float, z: Float, color: Int) {
    putFloat(bytes, offset, x)
    putFloat(bytes, offset + 4, y)
    putFloat(bytes, offset + 8, z)
    putInt(bytes, offset + 12, color)
}

private fun putFloat(bytes: ByteArray, offset: Int, value: Float) {
    putInt(bytes, offset, value.toBits())
}

private fun putInt(bytes: ByteArray, offset: Int, value: Int) {
    bytes[offset] = (value and 0xFF).toByte()
    bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    bytes[offset + 2] = ((value ushr 16) and 0xFF).toByte()
    bytes[offset + 3] = ((value ushr 24) and 0xFF).toByte()
}

private fun rotationZMatrix(angleRadians: Float): FloatArray {
    val cosine = cos(angleRadians)
    val sine = sin(angleRadians)
    return floatArrayOf(
        cosine, sine, 0f, 0f,
        -sine, cosine, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    )
}

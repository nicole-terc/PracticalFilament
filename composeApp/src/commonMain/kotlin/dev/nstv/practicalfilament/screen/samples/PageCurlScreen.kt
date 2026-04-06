// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-page-curl
package dev.nstv.practicalfilament.screen.samples

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import dev.nstv.practicalfilament.filament.AttributeDataType
import dev.nstv.practicalfilament.filament.BoundingBox
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.CustomRenderableConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.VertexAttribute
import dev.nstv.practicalfilament.filament.VertexAttributeLayout
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterType
import dev.nstv.practicalfilament.filament.toByteArray
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val PageSegmentsX = 20
private const val PageSegmentsY = 20
private const val PageMaterialPath = "files/materials/pageCurl.filamat"
private const val PageTexturePath = "files/image.jpg"
private const val PageImageMargin = 0.12f

@Composable
fun PageCurlScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var curlAmount by remember { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var dragStartCurl by remember { mutableFloatStateOf(0f) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine, renderableHandle, curlAmount) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        val pageState = buildPageState(curlAmount)
        currentEngine.updateVertexData(renderableHandle, buildPageVertexData(pageState).toByteArray())
        currentEngine.setRenderableTransform(renderableHandle, buildPageTransform(pageState.rigidAngleRadians))
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Page Curl",
        view = {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(viewportSize) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                dragStartCurl = curlAmount
                            },
                        ) { change, _ ->
                            val width = viewportSize.width.toFloat().coerceAtLeast(1f)
                            val deltaX = (change.position.x - dragStartX) / width
                            curlAmount = (dragStartCurl - (deltaX * 3f)).coerceIn(0f, 1f)
                            change.consume()
                        }
                    },
                camera = CameraConfig(
                    position = Float3(0f, 0f, 3f),
                    lookAt = Float3(0f, 0f, 0f),
                    fovDegrees = 60.0,
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        intensity = 15f,
                        direction = Float3(0f, -0.54f, -0.89f),
                    ),
                ),
                backgroundColor = FilamentColor(0.42f, 0.55f, 0.74f, 1f),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    readyEngine.setCameraExposure(1f, 1f, 1f)
                    val materialHandle = readyEngine.loadMaterial(Res.getUri(PageMaterialPath))
                    if (materialHandle <= 0) {
                        notice = "The page material could not be loaded."
                        return@FilamentView
                    }
                    val definitions = readyEngine.getMaterialParameters(materialHandle)
                    val instanceHandle = readyEngine.createMaterialInstance(materialHandle)
                    if (definitions.any { it.name == "backgroundColor" }) {
                        readyEngine.setMaterialParameter(
                            instanceHandle,
                            MaterialParameter("backgroundColor", Float3(0.96f, 0.95f, 0.93f)),
                        )
                    }
                    val samplerName = definitions
                        .firstOrNull {
                            it.type is MaterialParameterType.Sampler2d ||
                                    it.type is MaterialParameterType.SamplerExternal
                        }
                        ?.name
                    if (samplerName == null) {
                        notice = "The page material does not expose a sampler parameter."
                        return@FilamentView
                    }
                    val textureHandle = readyEngine.loadTexture(Res.getUri(PageTexturePath))
                    if (textureHandle <= 0) {
                        notice = "The page texture could not be loaded."
                        return@FilamentView
                    }
                    readyEngine.setTextureParameter(instanceHandle, samplerName, textureHandle)
                    val pageState = buildPageState(curlAmount)
                    renderableHandle = readyEngine.createCustomRenderable(
                        CustomRenderableConfig(
                            vertexData = buildPageVertexData(pageState).toByteArray(),
                            vertexCount = (PageSegmentsX + 1) * (PageSegmentsY + 1),
                            strideBytes = 20,
                            attributes = listOf(
                                VertexAttributeLayout(
                                    VertexAttribute.POSITION,
                                    AttributeDataType.FLOAT3,
                                    0
                                ),
                                VertexAttributeLayout(
                                    VertexAttribute.UV0,
                                    AttributeDataType.FLOAT2,
                                    12
                                ),
                            ),
                            indices = buildPageIndices(),
                            materialInstanceHandle = instanceHandle,
                            boundingBox = BoundingBox(
                                center = Float3(0.2f, 0f, 0f),
                                halfExtent = Float3(1.2f, 1.2f, 1.2f),
                            ),
                        ),
                    )
                    if (renderableHandle <= 0) {
                        notice = "The platform engine does not expose dynamic custom geometry yet."
                    } else {
                        readyEngine.setRenderableTransform(
                            renderableHandle,
                            buildPageTransform(pageState.rigidAngleRadians)
                        )
                    }
                },
            )
        },
        controls = {
            notice?.let { SampleNotice(it) }
            Text("Curl Amount")
            Slider(
                value = curlAmount,
                valueRange = 0f..1f,
                onValueChange = { curlAmount = it },
            )
        },
    )
}

private data class PageState(
    val theta: Float,
    val apex: Float,
    val rigidAngleRadians: Float,
)

private fun buildPageState(t: Float, rigidity: Float = 0.1f): PageState {
    val clampedT = t.coerceIn(0f, 1f)
    val clampedRigidity = rigidity.coerceIn(0f, 1f)
    val baseDeformation = 0.15f * (1f + clampedRigidity)
    val deformation = baseDeformation + (1f - sin(clampedT * PI.toFloat())) * (1f - baseDeformation)
    return PageState(
        theta = -deformation * (PI.toFloat() / 2f),
        apex = -15f * deformation,
        rigidAngleRadians = PI.toFloat() * clampedT,
    )
}

private fun buildPageVertexData(state: PageState): FloatArray {
    val vertexData = FloatArray((PageSegmentsX + 1) * (PageSegmentsY + 1) * 5)
    var index = 0
    for (y in 0..PageSegmentsY) {
        val v = y.toFloat() / PageSegmentsY
        for (x in 0..PageSegmentsX) {
            val u = x.toFloat() / PageSegmentsX
            val position = deformPoint(state.theta, state.apex, u, v)
            vertexData[index++] = position.x - 0.5f
            vertexData[index++] = (position.y - 0.5f) * 1.45f
            vertexData[index++] = position.z * 0.6f
            vertexData[index++] = remapUvToImageBounds(u)
            vertexData[index++] = remapUvToImageBounds(v)
        }
    }
    return vertexData
}

private fun buildPageIndices(): ShortArray {
    val indices = ShortArray(PageSegmentsX * PageSegmentsY * 6)
    var index = 0
    for (y in 0 until PageSegmentsY) {
        for (x in 0 until PageSegmentsX) {
            val topLeft = y * (PageSegmentsX + 1) + x
            val topRight = topLeft + 1
            val bottomLeft = topLeft + PageSegmentsX + 1
            val bottomRight = bottomLeft + 1
            indices[index++] = topLeft.toShort()
            indices[index++] = bottomLeft.toShort()
            indices[index++] = topRight.toShort()
            indices[index++] = topRight.toShort()
            indices[index++] = bottomLeft.toShort()
            indices[index++] = bottomRight.toShort()
        }
    }
    return indices
}

private fun remapUvToImageBounds(value: Float): Float {
    val imageScale = 1f / (1f - (PageImageMargin * 2f))
    return (value - PageImageMargin) * imageScale
}

private fun deformPoint(theta: Float, apex: Float, u: Float, v: Float): Float3 {
    val r = sqrt((u * u) + ((v - apex) * (v - apex)))
    val d = r * sin(theta)
    val alpha = asin((u / r).coerceIn(-1f, 1f))
    val sinTheta = if (kotlin.math.abs(sin(theta)) < 1e-4f) {
        if (theta >= 0f) 1e-4f else -1e-4f
    } else {
        sin(theta)
    }
    val beta = alpha / sinTheta

    val x = d * sin(beta)
    val y = r + apex - d * (1f - cos(beta)) * sinTheta
    val z = d * (1f - cos(beta)) * cos(theta)
    return Float3(x, y, z)
}

private fun buildPageTransform(rigidAngleRadians: Float): FloatArray {
    val scale = 1.35f
    val cosAngle = cos(rigidAngleRadians)
    val sinAngle = sin(rigidAngleRadians)
    return floatArrayOf(
        scale * cosAngle, 0f, scale * sinAngle, 0f,
        0f, scale, 0f, 0f,
        -scale * sinAngle, 0f, scale * cosAngle, 0f,
        0f, -0.5f, 0f, 1f,
    )
}

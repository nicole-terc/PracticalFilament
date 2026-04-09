package dev.nstv.practicalfilament.screen.otherViewers

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import dev.nstv.practicalfilament.filament.withFrameSeconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.materials.morphingMaterial
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.MorphRenderableGeometry
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.CheckBoxLabel
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val MorphingIndirectLightPath = "files/envs/pillars_2k/pillars_2k_ibl.ktx"
private const val MorphingSkyboxPath = "files/envs/pillars_2k/pillars_2k_skybox.ktx"
private const val MorphingEnvironmentIntensity = 45_000f
private val MorphingBaseLights = listOf(
    LightConfig(
        type = LightType.SUN,
        color = FilamentColor(0.98f, 0.98f, 1f),
        intensity = 85_000f,
        direction = Float3(0.35f, -1f, -0.6f),
        sunAngularRadius = 1.8f,
        sunHaloSize = 8f,
        sunHaloFalloff = 60f,
    ),
)

@Composable
fun MorphingScreen(
    modifier: Modifier = Modifier,
) {
    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var lastDragPoint by remember { mutableStateOf<Float3?>(null) }
    var orientation by remember { mutableStateOf(initialMorphingOrientation()) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var morphWeightA by remember { mutableFloatStateOf(0.18f) }
    var morphWeightB by remember { mutableFloatStateOf(0.82f) }
    var autoPlay by remember { mutableStateOf(true) }
    var animationSpeed by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(
        filamentEngine,
        renderableHandle,
        morphWeightA,
        morphWeightB,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        currentEngine.setMorphWeights(renderableHandle, floatArrayOf(morphWeightA, morphWeightB))
        currentEngine.requestFrame()
    }

    LaunchedEffect(
        filamentEngine,
        orientation,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        currentEngine.updateCamera(morphingCameraForOrientation(orientation))
        currentEngine.requestFrame()
    }

    LaunchedEffect(autoPlay, animationSpeed) {
        if (!autoPlay) return@LaunchedEffect
        withFrameSeconds { elapsed, _ ->
            val phase = elapsed * animationSpeed
            morphWeightA = 0.5f + 0.5f * sin(phase * 1.2f)
            morphWeightB = 0.5f + 0.5f * cos((phase * 0.75f) + (PI.toFloat() * 0.35f))
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(renderableHandle, viewportSize) {
                        detectDragGestures(
                            onDragStart = { start ->
                                lastDragPoint = start.toMorphingArcballPoint(viewportSize)
                            },
                            onDragEnd = {
                                lastDragPoint = null
                            },
                            onDragCancel = {
                                lastDragPoint = null
                            },
                        ) { change, _ ->
                            val previousPoint = lastDragPoint
                                ?: change.previousPosition.toMorphingArcballPoint(viewportSize)
                            val currentPoint = change.position.toMorphingArcballPoint(viewportSize)
                            if (previousPoint != null && currentPoint != null) {
                                orientation = morphingArcballDelta(previousPoint, currentPoint) * orientation
                                lastDragPoint = currentPoint
                            }
                            change.consume()
                        }
                    },
                camera = morphingCameraForOrientation(orientation),
                lights = MorphingBaseLights,
                onEngineReady = { engine ->
                    val loaded = engine.loadMaterial(morphingMaterial())
                    val indirectLightHandle =
                        engine.loadIndirectLight(Res.getUri(MorphingIndirectLightPath))
                    if (indirectLightHandle > 0) {
                        engine.setIndirectLight(
                            handle = indirectLightHandle,
                            intensity = MorphingEnvironmentIntensity,
                        )
                    }
                    val skyboxHandle = engine.loadSkybox(Res.getUri(MorphingSkyboxPath))
                    if (skyboxHandle > 0) {
                        engine.setSkybox(skyboxHandle)
                    }
                    filamentEngine = engine
                    orientation = initialMorphingOrientation()
                    lastDragPoint = null
                    renderableHandle = engine.createMorphRenderable(
                        materialInstanceHandle = loaded.instanceHandle,
                        geometry = buildMorphingCubeGeometry(size = 0.45f),
                    )
                },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(Grid.Two)
        ) {
            Text(
                text = "Morphing",
                style = MaterialTheme.typography.headlineSmall,
            )
            CheckBoxLabel(
                text = "Autoplay",
                checked = autoPlay,
                onCheckedChange = { autoPlay = it },
            )
            MorphingSlider(
                modifier = Modifier.padding(top = Grid.One),
                label = "Animation Speed",
                value = animationSpeed,
                enabled = autoPlay,
                valueRange = 0.2f..2.4f,
                onValueChange = { animationSpeed = it },
            )
            MorphingSlider(
                modifier = Modifier.padding(top = Grid.One),
                label = "Target A",
                value = morphWeightA,
                enabled = !autoPlay,
                onValueChange = { morphWeightA = it },
            )
            MorphingSlider(
                modifier = Modifier.padding(top = Grid.One),
                label = "Target B",
                value = morphWeightB,
                enabled = !autoPlay,
                onValueChange = { morphWeightB = it },
            )
        }
    }
}

@Composable
private fun MorphingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = value.toMorphingLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            modifier = Modifier.padding(top = Grid.Half),
            value = value,
            enabled = enabled,
            valueRange = valueRange,
            onValueChange = onValueChange,
        )
    }
}

private data class MorphingQuaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    operator fun times(other: MorphingQuaternion): MorphingQuaternion {
        return multiplyRaw(other).normalized()
    }

    private fun multiplyRaw(other: MorphingQuaternion): MorphingQuaternion {
        return MorphingQuaternion(
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w,
            w = w * other.w - x * other.x - y * other.y - z * other.z,
        )
    }

    fun normalized(): MorphingQuaternion {
        val magnitude = sqrt(x * x + y * y + z * z + w * w)
        if (magnitude <= 1e-6f) return MorphingQuaternion(0f, 0f, 0f, 1f)
        return MorphingQuaternion(x / magnitude, y / magnitude, z / magnitude, w / magnitude)
    }

    fun conjugate(): MorphingQuaternion = MorphingQuaternion(-x, -y, -z, w)

    fun rotate(vector: Float3): Float3 {
        val quaternionVector = MorphingQuaternion(vector.x, vector.y, vector.z, 0f)
        val rotated = multiplyRaw(quaternionVector).multiplyRaw(conjugate())
        return Float3(rotated.x, rotated.y, rotated.z)
    }
}

private fun initialMorphingOrientation(): MorphingQuaternion {
    return MorphingQuaternion(0f, 0f, 0f, 1f)
}

private fun morphingCameraForOrientation(orientation: MorphingQuaternion): CameraConfig {
    val orbit = orientation.conjugate()
    return CameraConfig(
        position = orbit.rotate(Float3(0f, 0f, 5f)),
        lookAt = Float3(0f, 0f, 0f),
        up = orbit.rotate(Float3(0f, 1f, 0f)),
        fovDegrees = 30.0,
    )
}

private fun morphingArcballDelta(from: Float3, to: Float3): MorphingQuaternion {
    val dot = (from morphingDot to).coerceIn(-1f, 1f)
    val cross = from morphingCross to
    val magnitude = cross.lengthMorphing()
    if (magnitude <= 1e-6f) {
        return if (dot < 0f) {
            val fallbackAxis = if (abs(from.x) < 0.9f) {
                Float3(1f, 0f, 0f) morphingCross from
            } else {
                Float3(0f, 1f, 0f) morphingCross from
            }.normalizedMorphing()
            MorphingQuaternion(fallbackAxis.x, fallbackAxis.y, fallbackAxis.z, 0f)
        } else {
            MorphingQuaternion(0f, 0f, 0f, 1f)
        }
    }
    return MorphingQuaternion(
        x = cross.x,
        y = cross.y,
        z = cross.z,
        w = 1f + dot,
    ).normalized()
}

private fun Offset.toMorphingArcballPoint(size: IntSize): Float3? {
    if (size.width <= 0 || size.height <= 0) return null
    val scale = minOf(size.width, size.height).toFloat()
    val x = (2f * this.x - size.width) / scale
    val y = (size.height - 2f * this.y) / scale
    val lengthSquared = x * x + y * y
    return if (lengthSquared <= 1f) {
        Float3(x, y, sqrt(1f - lengthSquared))
    } else {
        val invLength = 1f / sqrt(lengthSquared)
        Float3(x * invLength, y * invLength, 0f)
    }
}

private infix fun Float3.morphingDot(other: Float3): Float = x * other.x + y * other.y + z * other.z

private infix fun Float3.morphingCross(other: Float3): Float3 = Float3(
    x = y * other.z - z * other.y,
    y = z * other.x - x * other.z,
    z = x * other.y - y * other.x,
)

private fun Float3.lengthMorphing(): Float = sqrt(x * x + y * y + z * z)

private fun Float3.normalizedMorphing(): Float3 {
    val length = lengthMorphing()
    if (length <= 1e-6f) return Float3(0f, 0f, 1f)
    return Float3(x / length, y / length, z / length)
}

private fun Float.toMorphingLabel(): String {
    return ((this * 100f).roundToInt() / 100f).toString()
}

private fun buildMorphingCubeGeometry(size: Float): MorphRenderableGeometry {
    val faceCornerIndices = intArrayOf(
        1, 2, 6, 5,
        4, 7, 3, 0,
        3, 7, 6, 2,
        4, 0, 1, 5,
        4, 5, 6, 7,
        1, 0, 3, 2,
    )
    val faceUvs = floatArrayOf(
        0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
        0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
        0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
        0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
        0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
        0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
    )
    val baseCorners = arrayOf(
        Float3(-1f, -1f, -1f),
        Float3(1f, -1f, -1f),
        Float3(1f, 1f, -1f),
        Float3(-1f, 1f, -1f),
        Float3(-1f, -1f, 1f),
        Float3(1f, -1f, 1f),
        Float3(1f, 1f, 1f),
        Float3(-1f, 1f, 1f),
    ).map { it.scaled(size) }.toTypedArray()
    val targetACorners = arrayOf(
        Float3(-1.8f, -1.2f, -0.35f),
        Float3(0.35f, -1.7f, -1.65f),
        Float3(1.7f, 0.55f, -0.4f),
        Float3(-0.45f, 1.6f, -1.85f),
        Float3(-1.15f, -0.45f, 1.85f),
        Float3(1.85f, -1.55f, 0.3f),
        Float3(0.55f, 1.85f, 1.45f),
        Float3(-1.75f, 0.35f, 0.8f),
    ).map { it.scaled(size) }.toTypedArray()
    val targetBCorners = arrayOf(
        Float3(-0.25f, -1.85f, -1.55f),
        Float3(1.85f, -0.35f, -0.25f),
        Float3(0.25f, 1.75f, -1.85f),
        Float3(-1.85f, 0.45f, -0.55f),
        Float3(-1.85f, -0.25f, 0.55f),
        Float3(0.35f, -1.8f, 1.85f),
        Float3(1.55f, 0.25f, 0.35f),
        Float3(-0.35f, 1.85f, 1.8f),
    ).map { it.scaled(size) }.toTypedArray()

    return MorphRenderableGeometry(
        positions = expandCubePositions(baseCorners, faceCornerIndices),
        uvs = faceUvs,
        indices = buildCubeIndices(),
        morphTargetPositions = listOf(
            expandCubePositions(targetACorners, faceCornerIndices),
            expandCubePositions(targetBCorners, faceCornerIndices),
        ),
    )
}

private fun expandCubePositions(corners: Array<Float3>, faceCornerIndices: IntArray): FloatArray {
    val positions = FloatArray(faceCornerIndices.size * 3)
    faceCornerIndices.forEachIndexed { vertexIndex, cornerIndex ->
        val corner = corners[cornerIndex]
        val offset = vertexIndex * 3
        positions[offset] = corner.x
        positions[offset + 1] = corner.y
        positions[offset + 2] = corner.z
    }
    return positions
}

private fun buildCubeIndices(): ShortArray {
    val indices = ShortArray(6 * 6)
    repeat(6) { face ->
        val vertexOffset = (face * 4).toShort()
        val indexOffset = face * 6
        indices[indexOffset] = vertexOffset
        indices[indexOffset + 1] = (vertexOffset + 1).toShort()
        indices[indexOffset + 2] = (vertexOffset + 2).toShort()
        indices[indexOffset + 3] = vertexOffset
        indices[indexOffset + 4] = (vertexOffset + 2).toShort()
        indices[indexOffset + 5] = (vertexOffset + 3).toShort()
    }
    return indices
}

private fun Float3.scaled(scale: Float): Float3 = Float3(x * scale, y * scale, z * scale)

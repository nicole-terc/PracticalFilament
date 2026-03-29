package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.components.RedballMaterial
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.loadMaterialOnEngine
import dev.nstv.practicalfilament.theme.Grid
import kotlin.math.sqrt

@Composable
fun RedballScreen(
    modifier: Modifier = Modifier,
) {
    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var lastDragPoint by remember { mutableStateOf<Float3?>(null) }
    var orientation by remember { mutableStateOf(initialRedballOrientation()) }
    var gestureLightHandle by remember { mutableIntStateOf(0) }
    var gestureLightPosition by remember { mutableStateOf(Float3(-0.9f, 1.1f, 3.2f)) }
    var gestureLightScreenPosition by remember { mutableStateOf<Offset?>(null) }
    var gestureLightVersion by remember { mutableLongStateOf(1L) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var materialInstanceHandle by remember { mutableIntStateOf(0) }
    var materialParameterDefinitions by remember {
        mutableStateOf<List<MaterialParameterDefinition>>(emptyList())
    }
    var materialParameters by remember { mutableStateOf<Map<String, MaterialParameter>>(emptyMap()) }

    LaunchedEffect(
        filamentEngine,
        materialInstanceHandle,
        materialParameters,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (materialInstanceHandle == 0) return@LaunchedEffect

        materialParameters.values.forEach { parameter ->
            currentEngine.setMaterialParameter(materialInstanceHandle, parameter)
        }
        currentEngine.requestFrame()
    }

    LaunchedEffect(
        filamentEngine,
        renderableHandle,
        orientation,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        currentEngine.setRenderableTransform(
            handle = renderableHandle,
            transform = orientation.toMatrix4(),
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(
        filamentEngine,
        gestureLightVersion,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (gestureLightHandle != 0) {
            currentEngine.removeLight(gestureLightHandle)
            gestureLightHandle = 0
        }
        val position = gestureLightPosition
        gestureLightHandle = currentEngine.addLight(
            LightConfig(
                type = LightType.POINT,
                color = Color(1f, 0.96f, 0.94f),
                intensity = 750_000f,
                position = position,
                falloffRadius = 12f,
            )
        )
        currentEngine.requestFrame()
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
                    .pointerInput(viewportSize) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                val worldLightPosition =
                                    tapOffset.toRedballGestureLightPosition(viewportSize)
                                if (worldLightPosition != null) {
                                    gestureLightScreenPosition = tapOffset
                                    gestureLightPosition = worldLightPosition
                                    gestureLightVersion += 1L
                                }
                            }
                        )
                    }
                    .pointerInput(renderableHandle) {
                        detectDragGestures(
                            onDragStart = { start ->
                                lastDragPoint = start.toRedballArcballPoint(viewportSize)
                            },
                            onDragEnd = {
                                lastDragPoint = null
                            },
                            onDragCancel = {
                                lastDragPoint = null
                            },
                        ) { change, _ ->
                            val previousPoint = lastDragPoint
                                ?: change.previousPosition.toRedballArcballPoint(viewportSize)
                            val currentPoint = change.position.toRedballArcballPoint(viewportSize)
                            if (previousPoint != null && currentPoint != null) {
                                orientation =
                                    redballArcballDelta(previousPoint, currentPoint) * orientation
                                lastDragPoint = currentPoint
                            }
                            change.consume()
                        }
                    },
                camera = CameraConfig(
                    position = Float3(0f, 0.1f, 4.1f),
                    lookAt = Float3(0f, 0f, 0f),
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        color = Color(1f, 0.96f, 0.92f),
                        intensity = 95_000f,
                        direction = Float3(-0.18f, -0.36f, -1f),
                    ),
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        color = Color(0.70f, 0.82f, 1f),
                        intensity = 20_000f,
                        direction = Float3(0.8f, 0.1f, -0.55f),
                    ),
                ),
                backgroundColor = Color(0.03f, 0.03f, 0.04f, 1f),
                onEngineReady = { engine ->
                    val (instanceHandle, definitions, parameters) = loadMaterialOnEngine(
                        engine,
                        RedballMaterial,
                    )
                    filamentEngine = engine
                    materialParameterDefinitions = definitions
                    materialParameters = parameters
                    materialInstanceHandle = instanceHandle
                    orientation = initialRedballOrientation()
                    lastDragPoint = null
                    gestureLightHandle = 0
                    renderableHandle = engine.createSphereRenderable(
                        materialInstanceHandle = instanceHandle,
                        radius = 1f,
                    )
                },
            )
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val marker = gestureLightScreenPosition ?: return@Canvas
                drawCircle(
                    color = ComposeColor.White.copy(alpha = 0.16f),
                    radius = 28f,
                    center = marker,
                )
                drawCircle(
                    color = ComposeColor(0xFFFF8A80).copy(alpha = 0.92f),
                    radius = 10f,
                    center = marker,
                )
                drawCircle(
                    color = ComposeColor.White.copy(alpha = 0.8f),
                    radius = 18f,
                    center = marker,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(Grid.Two)
        ) {
            Text(
                text = "Red Ball",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                modifier = Modifier.padding(top = Grid.Half, bottom = Grid.One),
                text = "Custom plastic material based on Filament's redball sample. Drag to rotate the sphere and double-tap the viewport to reposition the highlight light.",
                style = MaterialTheme.typography.bodyMedium,
            )

            materialParameterDefinitions.forEach { definition ->
                val parameter = materialParameters[definition.name] ?: return@forEach
                ParameterInputField(
                    name = definition.name,
                    type = definition.type,
                    value = parameter.value,
                ) { updatedValue ->
                    materialParameters = materialParameters + (
                        definition.name to MaterialParameter(definition.name, updatedValue)
                    )
                }
            }
        }
    }
}

private data class RedballQuaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    operator fun times(other: RedballQuaternion): RedballQuaternion {
        return RedballQuaternion(
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w,
            w = w * other.w - x * other.x - y * other.y - z * other.z,
        ).normalized()
    }

    fun normalized(): RedballQuaternion {
        val magnitude = sqrt(x * x + y * y + z * z + w * w)
        if (magnitude <= 1e-6f) return RedballQuaternion(0f, 0f, 0f, 1f)
        return RedballQuaternion(x / magnitude, y / magnitude, z / magnitude, w / magnitude)
    }

    fun toMatrix4(): FloatArray {
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z
        return floatArrayOf(
            1f - 2f * (yy + zz), 2f * (xy + wz), 2f * (xz - wy), 0f,
            2f * (xy - wz), 1f - 2f * (xx + zz), 2f * (yz + wx), 0f,
            2f * (xz + wy), 2f * (yz - wx), 1f - 2f * (xx + yy), 0f,
            0f, 0f, 0f, 1f,
        )
    }
}

private fun initialRedballOrientation(): RedballQuaternion {
    val pitch = redballQuaternionFromAxisAngle(Float3(1f, 0f, 0f), -10f)
    val yaw = redballQuaternionFromAxisAngle(Float3(0f, 1f, 0f), 16f)
    return yaw * pitch
}

private fun redballQuaternionFromAxisAngle(axis: Float3, angleDegrees: Float): RedballQuaternion {
    val halfAngleRadians = angleDegrees * (kotlin.math.PI.toFloat() / 180f) * 0.5f
    val sinHalf = kotlin.math.sin(halfAngleRadians)
    val normalizedAxis = axis.normalizedRedball()
    return RedballQuaternion(
        x = normalizedAxis.x * sinHalf,
        y = normalizedAxis.y * sinHalf,
        z = normalizedAxis.z * sinHalf,
        w = kotlin.math.cos(halfAngleRadians),
    ).normalized()
}

private fun redballArcballDelta(from: Float3, to: Float3): RedballQuaternion {
    val dot = (from redballDot to).coerceIn(-1f, 1f)
    val cross = from redballCross to
    val magnitude = cross.lengthRedball()
    if (magnitude <= 1e-6f) {
        return if (dot < 0f) {
            val fallbackAxis = if (kotlin.math.abs(from.x) < 0.9f) {
                Float3(1f, 0f, 0f) redballCross from
            } else {
                Float3(0f, 1f, 0f) redballCross from
            }.normalizedRedball()
            RedballQuaternion(fallbackAxis.x, fallbackAxis.y, fallbackAxis.z, 0f)
        } else {
            RedballQuaternion(0f, 0f, 0f, 1f)
        }
    }
    return RedballQuaternion(
        x = cross.x,
        y = cross.y,
        z = cross.z,
        w = 1f + dot,
    ).normalized()
}

private fun Offset.toRedballArcballPoint(size: IntSize): Float3? {
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

private fun Offset.toRedballGestureLightPosition(size: IntSize): Float3? {
    if (size.width <= 0 || size.height <= 0) return null
    val scale = minOf(size.width, size.height).toFloat()
    val normalizedX = ((2f * x) - size.width) / scale
    val normalizedY = (size.height - (2f * y)) / scale
    val radial = normalizedX * normalizedX + normalizedY * normalizedY
    val hemisphereZ = if (radial <= 1f) {
        sqrt(1f - radial)
    } else {
        0f
    }
    val length =
        sqrt(normalizedX * normalizedX + normalizedY * normalizedY + hemisphereZ * hemisphereZ)
            .coerceAtLeast(1e-6f)
    val radius = 3.8f
    return Float3(
        x = (normalizedX / length) * radius,
        y = (normalizedY / length) * radius,
        z = (hemisphereZ / length) * radius,
    )
}

private infix fun Float3.redballDot(other: Float3): Float = x * other.x + y * other.y + z * other.z

private infix fun Float3.redballCross(other: Float3): Float3 = Float3(
    x = y * other.z - z * other.y,
    y = z * other.x - x * other.z,
    z = x * other.y - y * other.x,
)

private fun Float3.lengthRedball(): Float = sqrt(x * x + y * y + z * z)

private fun Float3.normalizedRedball(): Float3 {
    val length = lengthRedball()
    if (length <= 1e-6f) return Float3(0f, 0f, 1f)
    return Float3(x / length, y / length, z / length)
}

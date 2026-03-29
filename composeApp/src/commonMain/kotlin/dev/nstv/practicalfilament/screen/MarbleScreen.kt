package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.MaterialOverridesList
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.BuiltInTexture
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.generateTexturePixels
import dev.nstv.practicalfilament.filament.material.loadMaterialOnEngine
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import kotlin.math.sqrt


@Composable
fun MarbleScreen(
    modifier: Modifier = Modifier,
) {
    val marbleMaterials = MaterialOverridesList

    var filamentEngine by remember {
        mutableStateOf<FilamentEngine?>(null)
    }
    var selectedMaterialIndex by remember { mutableIntStateOf(0) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var lastDragPoint by remember { mutableStateOf<Float3?>(null) }
    var orientation by remember { mutableStateOf(initialArcballOrientation()) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var materialInstanceHandle by remember { mutableIntStateOf(0) }
    var materialParameterDefinitions by remember {
        mutableStateOf<List<MaterialParameterDefinition>>(emptyList())
    }
    var materialParameters by remember { mutableStateOf<Map<String, MaterialParameter>>(emptyMap()) }
    var textureHandles by remember { mutableStateOf<Map<BuiltInTexture, Int>>(emptyMap()) }

    LaunchedEffect(
        filamentEngine,
        materialInstanceHandle,
        materialParameters,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (materialInstanceHandle == 0) return@LaunchedEffect

        var updatedTextureHandles = textureHandles
        materialParameters.values.forEach { parameter ->
            when (val value = parameter.value) {
                is BuiltInTexture -> {
                    val textureHandle = updatedTextureHandles[value] ?: currentEngine.createTexture(
                        width = 256,
                        height = 256,
                        pixels = generateTexturePixels(value),
                    ).also { handle ->
                        updatedTextureHandles = updatedTextureHandles + (value to handle)
                    }
                    currentEngine.setTextureParameter(
                        materialInstanceHandle,
                        parameter.name,
                        textureHandle
                    )
                }

                else -> currentEngine.setMaterialParameter(materialInstanceHandle, parameter)
            }
        }
        if (updatedTextureHandles != textureHandles) {
            textureHandles = updatedTextureHandles
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
                    .pointerInput(renderableHandle) {
                        detectDragGestures(
                            onDragStart = { start ->
                                lastDragPoint = start.toArcballPoint(viewportSize)
                            },
                            onDragEnd = {
                                lastDragPoint = null
                            },
                            onDragCancel = {
                                lastDragPoint = null
                            },
                        ) { change, _ ->
                            val previousPoint = lastDragPoint ?: change.previousPosition.toArcballPoint(viewportSize)
                            val currentPoint = change.position.toArcballPoint(viewportSize)
                            if (previousPoint != null && currentPoint != null) {
                                orientation = arcballDelta(previousPoint, currentPoint) * orientation
                                lastDragPoint = currentPoint
                            }
                            change.consume()
                        }
                    },
                camera = CameraConfig(
                    position = Float3(0f, 0.05f, 4.25f),
                    lookAt = Float3(0f, 0f, 0f),
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        color = Color(1f, 0.99f, 0.97f),
                        intensity = 95_000f,
                        // Filament treats this as the direction the light emits toward.
                        // With the camera on +Z, negative Z lights the front of the sphere.
                        direction = Float3(-0.18f, -0.32f, -1f),
                    ),
                ),
                backgroundColor = Color(0.16f, 0.18f, 0.27f, 1f),
                onEngineReady = { engine ->
                    val (instanceHandle, definitions, parameters) = loadMaterialOnEngine(
                        engine,
                        marbleMaterials[selectedMaterialIndex],
                    )
                    filamentEngine = engine
                    textureHandles = emptyMap()
                    materialParameterDefinitions = definitions
                    materialParameters = parameters
                    materialInstanceHandle = instanceHandle
                    orientation = initialArcballOrientation()
                    lastDragPoint = null
                    renderableHandle = engine.createSphereRenderable(
                        materialInstanceHandle = instanceHandle,
                        radius = 1f,
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
                text = "Marble",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                modifier = Modifier.padding(top = Grid.Half, bottom = Grid.One),
                text = marbleMaterials[selectedMaterialIndex].description,
                style = MaterialTheme.typography.bodyMedium,
            )

            DropDownWithArrows(
                options = marbleMaterials.map { it.label },
                selectedIndex = selectedMaterialIndex,
                label = "Material",
                onSelectionChanged = { index ->
                    val engine = filamentEngine ?: run {
                        selectedMaterialIndex = index
                        return@DropDownWithArrows
                    }
                    selectedMaterialIndex = index

                    if (renderableHandle != 0) {
                        engine.removeRenderable(renderableHandle)
                        renderableHandle = 0
                    }

                    val (instanceHandle, definitions, parameters) = loadMaterialOnEngine(
                        engine,
                        marbleMaterials[index],
                    )
                    textureHandles = emptyMap()
                    materialParameterDefinitions = definitions
                    materialParameters = parameters
                    materialInstanceHandle = instanceHandle
                    orientation = initialArcballOrientation()
                    lastDragPoint = null
                    renderableHandle = engine.createSphereRenderable(
                        materialInstanceHandle = instanceHandle,
                        radius = 1f,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Grid.One),
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

private data class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    operator fun times(other: Quaternion): Quaternion {
        return Quaternion(
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w,
            w = w * other.w - x * other.x - y * other.y - z * other.z,
        ).normalized()
    }

    fun normalized(): Quaternion {
        val magnitude = sqrt(x * x + y * y + z * z + w * w)
        if (magnitude <= 1e-6f) return Quaternion(0f, 0f, 0f, 1f)
        return Quaternion(x / magnitude, y / magnitude, z / magnitude, w / magnitude)
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

private fun initialArcballOrientation(): Quaternion {
    val pitch = quaternionFromAxisAngle(Float3(1f, 0f, 0f), -12f)
    val yaw = quaternionFromAxisAngle(Float3(0f, 1f, 0f), 20f)
    return yaw * pitch
}

private fun quaternionFromAxisAngle(axis: Float3, angleDegrees: Float): Quaternion {
    val halfAngleRadians = angleDegrees * (kotlin.math.PI.toFloat() / 180f) * 0.5f
    val sinHalf = kotlin.math.sin(halfAngleRadians)
    val normalizedAxis = axis.normalized()
    return Quaternion(
        x = normalizedAxis.x * sinHalf,
        y = normalizedAxis.y * sinHalf,
        z = normalizedAxis.z * sinHalf,
        w = kotlin.math.cos(halfAngleRadians),
    ).normalized()
}

private fun arcballDelta(from: Float3, to: Float3): Quaternion {
    val dot = (from dot to).coerceIn(-1f, 1f)
    val cross = from cross to
    val magnitude = cross.length()
    if (magnitude <= 1e-6f) {
        return if (dot < 0f) {
            val fallbackAxis = if (kotlin.math.abs(from.x) < 0.9f) {
                Float3(1f, 0f, 0f) cross from
            } else {
                Float3(0f, 1f, 0f) cross from
            }.normalized()
            Quaternion(fallbackAxis.x, fallbackAxis.y, fallbackAxis.z, 0f)
        } else {
            Quaternion(0f, 0f, 0f, 1f)
        }
    }
    return Quaternion(
        x = cross.x,
        y = cross.y,
        z = cross.z,
        w = 1f + dot,
    ).normalized()
}

private fun Offset.toArcballPoint(size: IntSize): Float3? {
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

private infix fun Float3.dot(other: Float3): Float = x * other.x + y * other.y + z * other.z

private infix fun Float3.cross(other: Float3): Float3 = Float3(
    x = y * other.z - z * other.y,
    y = z * other.x - x * other.z,
    z = x * other.y - y * other.x,
)

private fun Float3.length(): Float = sqrt(x * x + y * y + z * z)

private fun Float3.normalized(): Float3 {
    val length = length()
    if (length <= 1e-6f) return Float3(0f, 0f, 1f)
    return Float3(x / length, y / length, z / length)
}

package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.components.materials.MaterialOverridesList
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
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import kotlin.math.sqrt
import androidx.compose.ui.graphics.Color as ComposeColor


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
    var gestureLightHandle by remember { mutableIntStateOf(0) }
    var gestureLightPosition by remember { mutableStateOf<Float3?>(null) }
    var gestureLightScreenPosition by remember { mutableStateOf<Offset?>(null) }
    var gestureLightVersion by remember { mutableLongStateOf(0L) }
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
        gestureLightVersion,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (gestureLightHandle != 0) {
            currentEngine.removeLight(gestureLightHandle)
            gestureLightHandle = 0
        }
        val position = gestureLightPosition ?: return@LaunchedEffect
        gestureLightHandle = currentEngine.addLight(
            LightConfig(
                type = LightType.POINT,
                color = Color(1f, 1f, 1f),
                intensity = 600_000f,
                position = position,
                falloffRadius = 10f,
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
                                    tapOffset.toGestureLightPosition(viewportSize)
                                if (worldLightPosition != null) {
                                    gestureLightScreenPosition = tapOffset
                                    gestureLightPosition = worldLightPosition
                                    gestureLightVersion += 1L
                                }
                            }
                        )
                    },
                camera = CameraConfig(
                    position = Float3(0f, 0.05f, 4.25f),
                    lookAt = Float3(0f, 0f, 0f),
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        color = Color(1f, 0.98f, 0.95f),
                        intensity = 75_000f,
                        // Filament treats this as the direction the light emits toward.
                        // With the camera on +Z, negative Z lights the front of the sphere.
                        direction = Float3(-0.24f, -0.38f, -1f),
                    ),
                ),
                backgroundColor = Color(0.16f, 0.18f, 0.27f, 1f),
                onEngineReady = { engine ->
                    val loaded = engine.loadMaterial(marbleMaterials[selectedMaterialIndex])
                    filamentEngine = engine
                    textureHandles = emptyMap()
                    materialParameterDefinitions = loaded.definitions
                    materialParameters = loaded.parameters
                    materialInstanceHandle = loaded.instanceHandle
                    gestureLightHandle = 0
                    renderableHandle = engine.createSphereRenderable(
                        materialInstanceHandle = loaded.instanceHandle,
                        radius = 1f,
                    )
                },
            )
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val marker = gestureLightScreenPosition ?: return@Canvas
                drawCircle(
                    color = ComposeColor.White.copy(alpha = 0.2f),
                    radius = 26f,
                    center = marker,
                )
                drawCircle(
                    color = ComposeColor.White.copy(alpha = 0.95f),
                    radius = 12f,
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

                    val loaded = engine.loadMaterial(marbleMaterials[index])
                    textureHandles = emptyMap()
                    materialParameterDefinitions = loaded.definitions
                    materialParameters = loaded.parameters
                    materialInstanceHandle = loaded.instanceHandle
                    if (gestureLightHandle != 0) {
                        engine.removeLight(gestureLightHandle)
                        gestureLightHandle = 0
                    }
                    renderableHandle = engine.createSphereRenderable(
                        materialInstanceHandle = loaded.instanceHandle,
                        radius = 1f,
                    )
                    gestureLightPosition?.let {
                        gestureLightVersion += 1L
                    }
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

private fun Offset.toGestureLightPosition(size: IntSize): Float3? {
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
    val radius = 3.6f
    return Float3(
        x = (normalizedX / length) * radius,
        y = (normalizedY / length) * radius,
        z = (hemisphereZ / length) * radius,
    )
}

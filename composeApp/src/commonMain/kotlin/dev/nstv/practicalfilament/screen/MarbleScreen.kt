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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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


@Composable
fun MarbleScreen(
    modifier: Modifier = Modifier,
) {
    val marbleMaterials = MaterialOverridesList

    var filamentEngine by remember {
        mutableStateOf<FilamentEngine?>(null)
    }
    var selectedMaterialIndex by remember { mutableIntStateOf(0) }
    var rotationXDegrees by remember { mutableFloatStateOf(-12f) }
    var rotationYDegrees by remember { mutableFloatStateOf(20f) }
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
        rotationXDegrees,
        rotationYDegrees,
    ) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        currentEngine.setRenderableRotation(
            handle = renderableHandle,
            rotationXDegrees = rotationXDegrees,
            rotationYDegrees = rotationYDegrees,
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
                    .pointerInput(renderableHandle) {
                        detectDragGestures { _, dragAmount ->
                            rotationYDegrees -= dragAmount.x * 0.35f
                            rotationXDegrees = (rotationXDegrees - dragAmount.y * 0.35f)
                                .coerceIn(-85f, 85f)
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

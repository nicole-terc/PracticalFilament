package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.components.materials.MaterialFilesList
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

@Composable
fun MaterialViewerScreen(
    modifier: Modifier = Modifier,
) {
    val availableMaterials = MaterialFilesList
    var filamentEngine by remember {
        mutableStateOf<FilamentEngine?>(null)
    }
    var selectedMaterialIndex by remember { mutableIntStateOf(0) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var materialInstanceHandle by remember { mutableIntStateOf(0) }
    var materialParameterDefinitions by remember {
        mutableStateOf<List<MaterialParameterDefinition>>(
            emptyList()
        )
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

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = CameraConfig(
                    position = Float3(0f, 0f, 4f),
                    lookAt = Float3(0f, 0f, 0f),
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        color = Color(1f, 1f, 1f),
                        intensity = 110_000f,
                        direction = Float3(0f, -1f, -1f),
                    ),
                    LightConfig(
                        type = LightType.POINT,
                        color = Color(1f, 0.9f, 0.8f),
                        intensity = 50_000f,
                        position = Float3(2f, 2f, 2f),
                    ),
                ),
                onEngineReady = { engine ->
                    val loaded = engine.loadMaterial(availableMaterials[selectedMaterialIndex])
                    filamentEngine = engine
                    textureHandles = emptyMap()
                    materialParameterDefinitions = loaded.definitions
                    materialParameters = loaded.parameters
                    materialInstanceHandle = loaded.instanceHandle
                    renderableHandle = engine.createPlaneRenderable(loaded.instanceHandle)
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
            DropDownWithArrows(
                options = availableMaterials.map { it.label },
                selectedIndex = selectedMaterialIndex,
                label = "Material",
                onSelectionChanged = { index ->
                    val engine = filamentEngine ?: return@DropDownWithArrows
                    selectedMaterialIndex = index

                    // Remove old renderable
                    if (renderableHandle != 0) {
                        engine.removeRenderable(renderableHandle)
                        renderableHandle = 0
                    }

                    val loaded = engine.loadMaterial(availableMaterials[index])
                    materialParameterDefinitions = loaded.definitions
                    materialParameters = loaded.parameters
                    materialInstanceHandle = loaded.instanceHandle
                    renderableHandle = engine.createPlaneRenderable(loaded.instanceHandle)
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = Grid.One),
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

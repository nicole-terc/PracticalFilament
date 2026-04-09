package dev.nstv.practicalfilament.screen.filament.steps.step4

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.components.materials.AllMaterialsList
import dev.nstv.practicalfilament.components.materials.MaterialOverridesList
import dev.nstv.practicalfilament.components.materials.texturedSampleMaterial
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.material.BuiltInTexture
import dev.nstv.practicalfilament.filament.material.LoadedMaterial
import dev.nstv.practicalfilament.filament.material.Material
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.generateTexturePixels
import dev.nstv.practicalfilament.screen.marbles.components.EnvironmentSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackgroundFilament
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera
import dev.nstv.practicalfilament.screen.marbles.components.SphereStepLights
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import kotlin.collections.plus

private val FilamentStepMaterials = AllMaterialsList

@Composable
internal fun FilamentStepFour(
    modifier: Modifier = Modifier,
) {
    var showLight by remember { mutableStateOf(true) }
    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var selectedMaterialIndex by remember { mutableIntStateOf(0) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var loadedMaterial by remember { mutableStateOf<LoadedMaterial?>(null) }
    var materialParameterDefinitions by remember {
        mutableStateOf<List<MaterialParameterDefinition>>(emptyList())
    }
    var materialParameters by remember { mutableStateOf<Map<String, MaterialParameter>>(emptyMap()) }
    var textureHandles by remember { mutableStateOf<Map<BuiltInTexture, Int>>(emptyMap()) }

    fun loadSelectedMaterial(engine: FilamentEngine, material: Material) {
        if (renderableHandle != 0) {
            engine.removeRenderable(renderableHandle)
            renderableHandle = 0
        }

        val loaded = engine.loadMaterial(material)
        loadedMaterial = loaded
        textureHandles = emptyMap()
        materialParameterDefinitions = loaded.definitions
        materialParameters = loaded.parameters
        renderableHandle = if (loaded.instanceHandle > 0) {
            engine.createSphereRenderable(
                materialInstanceHandle = loaded.instanceHandle,
                radius = 1.1f,
            )
        } else {
            0
        }
    }

    LaunchedEffect(filamentEngine, showLight) {
        val engine = filamentEngine ?: return@LaunchedEffect
        engine.clearLights()
        if (showLight) {
            SphereStepLights.forEach(engine::addLight)
        }
        engine.requestFrame()
    }

    LaunchedEffect(filamentEngine, loadedMaterial, materialParameters) {
        val engine = filamentEngine ?: return@LaunchedEffect
        loadedMaterial?.let {
            if (!it.isTexturedMaterial) {
                var updatedTextureHandles = textureHandles
                materialParameters.values.forEach { parameter ->
                    when (val value = parameter.value) {
                        is BuiltInTexture -> {
                            val textureHandle = updatedTextureHandles[value] ?: engine.createTexture(
                                width = 256,
                                height = 256,
                                pixels = generateTexturePixels(value),
                            ).also { handle ->
                                updatedTextureHandles = updatedTextureHandles + (value to handle)
                            }
                            engine.setTextureParameter(
                                it.instanceHandle,
                                parameter.name,
                                textureHandle,
                            )
                        }

                        else -> engine.setMaterialParameter(it.instanceHandle, parameter)
                    }
                }
                if (updatedTextureHandles != textureHandles) {
                    textureHandles = updatedTextureHandles
                }
            }
            engine.requestFrame()
        }
    }

    SampleScreenLayout(
        title = "4. Material",
        modifier = modifier,
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = SingleMarbleCamera,
                backgroundColor = MarbleUiBackgroundFilament,
                onEngineReady = { engine ->
                    filamentEngine = engine
                    loadSelectedMaterial(engine, FilamentStepMaterials[selectedMaterialIndex])
                },
            )
        },
        controls = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showLight, onCheckedChange = { showLight = it })
                Text(
                    text = "Show light",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = Grid.One),
                )
            }
            EnvironmentSelectionField(filamentEngine = filamentEngine)
            DropDownWithArrows(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Grid.One),
                options = FilamentStepMaterials.map { it.label },
                selectedIndex = selectedMaterialIndex,
                label = "Material",
                onSelectionChanged = { index ->
                    selectedMaterialIndex = index
                    filamentEngine?.let { engine ->
                        loadSelectedMaterial(engine, FilamentStepMaterials[index])
                    }
                },
            )
            if (materialParameterDefinitions.isEmpty()) {
                Text(
                    text = "No editable material parameters were found for this material.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
        },
    )
}

package dev.nstv.practicalfilament.screen.filament.steps.step5

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.components.materials.AllMaterialsList
import dev.nstv.practicalfilament.components.utils.OrbitQuaternion
import dev.nstv.practicalfilament.components.utils.orbitCameraConfig
import dev.nstv.practicalfilament.components.utils.orbitCameraControls
import dev.nstv.practicalfilament.components.utils.orbitDistance
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.material.BuiltInTexture
import dev.nstv.practicalfilament.filament.material.LoadedMaterial
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.filament.material.generateTexturePixels
import dev.nstv.practicalfilament.screen.lights.components.LightPresets
import dev.nstv.practicalfilament.screen.lights.components.LightSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.EnvironmentSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackgroundFilament
import dev.nstv.practicalfilament.screen.marbles.components.MeshSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera
import dev.nstv.practicalfilament.screen.marbles.components.SphereMesh
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import practicalfilament.composeapp.generated.resources.Res

private val FilamentStepMaterials = AllMaterialsList
private const val FilamentStepFiveMinCameraDistance = 2.2f
private const val FilamentStepFiveMaxCameraDistance = 8f

@Composable
internal fun FilamentStepFive(
    modifier: Modifier = Modifier,
) {
    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var selectedMesh by remember { mutableStateOf(SphereMesh) }
    var selectedMaterialIndex by remember { mutableIntStateOf(0) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var orientation by remember { mutableStateOf(OrbitQuaternion.Identity) }
    var cameraDistance by remember { mutableStateOf(SingleMarbleCamera.orbitDistance()) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var loadedMaterial by remember { mutableStateOf<LoadedMaterial?>(null) }
    var materialParameterDefinitions by remember {
        mutableStateOf<List<MaterialParameterDefinition>>(emptyList())
    }
    var materialParameters by remember { mutableStateOf<Map<String, MaterialParameter>>(emptyMap()) }
    var textureHandles by remember { mutableStateOf<Map<BuiltInTexture, Int>>(emptyMap()) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun refreshScene(engine: FilamentEngine) {
        if (renderableHandle != 0) {
            engine.removeRenderable(renderableHandle)
            renderableHandle = 0
        }

        val material = FilamentStepMaterials[selectedMaterialIndex]
        val loaded = engine.loadMaterial(material)
        loadedMaterial = loaded
        textureHandles = emptyMap()
        materialParameterDefinitions = loaded.definitions
        materialParameters = loaded.parameters
        notice = when {
            loaded.instanceHandle <= 0 -> "The material could not be loaded."
            else -> null
        }
        renderableHandle = if (loaded.instanceHandle > 0) {
            loaded.parameters.values
                .filter { parameter -> parameter.value !is BuiltInTexture }
                .forEach { parameter ->
                    engine.setMaterialParameter(loaded.instanceHandle, parameter)
                }
            engine.loadMesh(
                path = Res.getUri(selectedMesh.path),
                materialInstanceHandle = loaded.instanceHandle,
                scale = selectedMesh.scale,
            )
        } else {
            0
        }
        if (loaded.instanceHandle > 0 && renderableHandle <= 0) {
            notice = "The ${selectedMesh.name} mesh could not be loaded."
        }
        engine.requestFrame()
    }

    LaunchedEffect(filamentEngine, orientation, cameraDistance) {
        val engine = filamentEngine ?: return@LaunchedEffect
        engine.updateCamera(
            orbitCameraConfig(
                baseCamera = SingleMarbleCamera,
                orientation = orientation,
                distance = cameraDistance,
            )
        )
        engine.requestFrame()
    }

    LaunchedEffect(filamentEngine, loadedMaterial, materialParameters) {
        val engine = filamentEngine ?: return@LaunchedEffect
        loadedMaterial?.let {
            var updatedTextureHandles = textureHandles
            materialParameters.values.forEach { parameter ->
                when (val value = parameter.value) {
                    is BuiltInTexture -> {
                        if (!it.isTexturedMaterial) {
                            val textureHandle =
                                updatedTextureHandles[value] ?: engine.createTexture(
                                    width = 256,
                                    height = 256,
                                    pixels = generateTexturePixels(value),
                                ).also { handle ->
                                    updatedTextureHandles =
                                        updatedTextureHandles + (value to handle)
                                }
                            engine.setTextureParameter(
                                it.instanceHandle,
                                parameter.name,
                                textureHandle,
                            )
                        }
                    }

                    else -> engine.setMaterialParameter(it.instanceHandle, parameter)
                }
            }
            if (updatedTextureHandles != textureHandles) {
                textureHandles = updatedTextureHandles
            }
            engine.requestFrame()
        }
    }

    SampleScreenLayout(
        title = "5. Lights",
        modifier = modifier,
        showControlsTitle = false,
        view = {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .orbitCameraControls(
                        viewportSize = viewportSize,
                        orientation = orientation,
                        onOrientationChange = { orientation = it },
                        distance = cameraDistance,
                        onDistanceChange = { cameraDistance = it },
                        minDistance = FilamentStepFiveMinCameraDistance,
                        maxDistance = FilamentStepFiveMaxCameraDistance,
                        enabled = renderableHandle > 0,
                    ),
                camera = orbitCameraConfig(
                    baseCamera = SingleMarbleCamera,
                    orientation = orientation,
                    distance = cameraDistance,
                ),
                lights = emptyList(),
                backgroundColor = MarbleUiBackgroundFilament,
                onEngineReady = { engine ->
                    filamentEngine = engine
                    refreshScene(engine)
                },
            )
        },
        controls = {
            notice?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Grid.One),
                )
            }
            LightSelectionField(
                filamentEngine = filamentEngine,
                presets = LightPresets,
                initialSelectedIndex = 3,
            )
            MeshSelectionField(
                selectedMesh = selectedMesh,
                onMeshSelectionChanged = { mesh ->
                    selectedMesh = mesh
                    filamentEngine?.let(::refreshScene)
                },
            )
            EnvironmentSelectionField(
                filamentEngine = filamentEngine,
                updateNotice = { notice = it },
            )
            DropDownWithArrows(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Grid.One),
                options = FilamentStepMaterials.map { it.label },
                selectedIndex = selectedMaterialIndex,
                label = "Material",
                onSelectionChanged = { index ->
                    selectedMaterialIndex = index
                    filamentEngine?.let(::refreshScene)
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

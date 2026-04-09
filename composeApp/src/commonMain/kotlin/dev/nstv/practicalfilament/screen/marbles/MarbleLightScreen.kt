package dev.nstv.practicalfilament.screen.marbles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.ParameterInputField
import dev.nstv.practicalfilament.components.materials.MaterialOverridesList
import dev.nstv.practicalfilament.components.utils.OrbitQuaternion
import dev.nstv.practicalfilament.components.utils.orbitCameraConfig
import dev.nstv.practicalfilament.components.utils.orbitCameraControls
import dev.nstv.practicalfilament.components.utils.orbitDistance
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.BuiltInTexture
import dev.nstv.practicalfilament.filament.material.MaterialParameter
import dev.nstv.practicalfilament.filament.material.MaterialParameterDefinition
import dev.nstv.practicalfilament.screen.HideOptions
import dev.nstv.practicalfilament.screen.lights.components.LightPresets
import dev.nstv.practicalfilament.screen.lights.components.LightSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.EnvironmentSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.MeshList
import dev.nstv.practicalfilament.screen.marbles.components.MeshSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.SphereMesh
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.sqrt

private const val LightDemoMinCameraDistance = 2.2f
private const val LightDemoMaxCameraDistance = 8f
private val LightDemoBaseCamera = CameraConfig(
    position = Float3(0f, 0.04f, 3.35f),
    lookAt = Float3(0f, 0f, 0f),
)

@Composable
fun MarbleLightScreen(
    modifier: Modifier = Modifier,
    showControls: Boolean = !HideOptions,
) {
    val materials = MaterialOverridesList

    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var selectedMesh by remember { mutableStateOf(SphereMesh) }
    var selectedMaterialIndex by remember { mutableIntStateOf(0) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var orientation by remember { mutableStateOf(OrbitQuaternion.Identity) }
    var cameraDistance by remember { mutableStateOf(LightDemoBaseCamera.orbitDistance()) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var materialInstanceHandle by remember { mutableIntStateOf(0) }
    var materialParameterDefinitions by remember {
        mutableStateOf<List<MaterialParameterDefinition>>(emptyList())
    }
    var materialParameters by remember { mutableStateOf<Map<String, MaterialParameter>>(emptyMap()) }
    var gestureLightHandle by remember { mutableIntStateOf(0) }
    var gestureLightPosition by remember { mutableStateOf<Float3?>(null) }
    var gestureLightScreenPosition by remember { mutableStateOf<Offset?>(null) }
    var gestureLightVersion by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filamentEngine, materialInstanceHandle, materialParameters) {
        val engine = filamentEngine ?: return@LaunchedEffect
        if (materialInstanceHandle == 0) return@LaunchedEffect
        materialParameters.values.forEach { parameter ->
            if (parameter.value !is BuiltInTexture) {
                engine.setMaterialParameter(materialInstanceHandle, parameter)
            }
        }
        engine.requestFrame()
    }

    LaunchedEffect(filamentEngine, gestureLightVersion) {
        val engine = filamentEngine ?: return@LaunchedEffect
        if (gestureLightHandle != 0) {
            engine.removeLight(gestureLightHandle)
            gestureLightHandle = 0
        }
        val position = gestureLightPosition ?: return@LaunchedEffect
        gestureLightHandle = engine.addLight(
            LightConfig(
                type = LightType.POINT,
                color = FilamentColor(1f, 1f, 1f),
                intensity = 600_000f,
                position = position,
                falloffRadius = 10f,
            ),
        )
        engine.requestFrame()
    }

    LaunchedEffect(filamentEngine, orientation, cameraDistance) {
        val engine = filamentEngine ?: return@LaunchedEffect
        engine.updateCamera(
            orbitCameraConfig(
                baseCamera = LightDemoBaseCamera,
                orientation = orientation,
                distance = cameraDistance,
            )
        )
        engine.requestFrame()
    }

    fun refreshScene() {
        val engine = filamentEngine ?: return

        if (renderableHandle != 0) {
            engine.removeRenderable(renderableHandle)
            renderableHandle = 0
        }

        val material = materials[selectedMaterialIndex]
        val loaded = engine.loadMaterial(material)
        notice = when {
            loaded.instanceHandle <= 0 -> "The material could not be loaded."
            else -> null
        }
        if (loaded.instanceHandle <= 0) return

        materialParameterDefinitions = loaded.definitions
        materialParameters = loaded.parameters
        materialInstanceHandle = loaded.instanceHandle

        renderableHandle = engine.loadMesh(
            path = Res.getUri(selectedMesh.path),
            materialInstanceHandle = loaded.instanceHandle,
            scale = selectedMesh.scale,
        )
        if (renderableHandle <= 0) {
            notice = "The ${selectedMesh.name} mesh could not be created on this platform."
            return
        }
        if (gestureLightHandle != 0) {
            engine.removeLight(gestureLightHandle)
            gestureLightHandle = 0
        }
        if (gestureLightPosition != null) {
            gestureLightVersion += 1L
        }
        engine.requestFrame()
    }

    SampleScreenLayout(
        modifier = modifier,
        showControls = showControls,
        title = "Light Types",
        view = {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(viewportSize) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                tapOffset.toGestureLightPosition(viewportSize)
                                    ?.let { worldLightPosition ->
                                        gestureLightScreenPosition = tapOffset
                                        gestureLightPosition = worldLightPosition
                                        gestureLightVersion += 1L
                                    }
                            },
                        )
                    }
                    .orbitCameraControls(
                        viewportSize = viewportSize,
                        orientation = orientation,
                        onOrientationChange = { orientation = it },
                        distance = cameraDistance,
                        onDistanceChange = { cameraDistance = it },
                        minDistance = LightDemoMinCameraDistance,
                        maxDistance = LightDemoMaxCameraDistance,
                        enabled = renderableHandle > 0,
                    ),
                camera = orbitCameraConfig(
                    baseCamera = LightDemoBaseCamera,
                    orientation = orientation,
                    distance = cameraDistance,
                ),
                lights = emptyList(),
                backgroundColor = FilamentColor(0.05f, 0.05f, 0.08f, 1f),
                onEngineReady = { engine ->
                    filamentEngine = engine
                    refreshScene()
                },
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val marker = gestureLightScreenPosition ?: return@Canvas
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = 26f,
                    center = marker,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    radius = 12f,
                    center = marker,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 18f,
                    center = marker,
                    style = Stroke(width = 3f),
                )
            }
        },
        controls = {
            notice?.let { SampleNotice(it) }

            LightSelectionField(
                filamentEngine = filamentEngine,
                presets = LightPresets,
                initialSelectedIndex = 1,
            )

            EnvironmentSelectionField(
                filamentEngine = filamentEngine,
                updateNotice = { notice = it },
            )

            MeshSelectionField(
                selectedMesh = selectedMesh,
                onMeshSelectionChanged = {
                    selectedMesh = it
                    refreshScene()
                },
            )

            DropDownWithArrows(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Grid.One),
                options = materials.map { it.label },
                selectedIndex = selectedMaterialIndex,
                label = "Material",
                onSelectionChanged = { index ->
                    selectedMaterialIndex = index
                    refreshScene()
                },
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

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Grid.One),
                text = "Double-tap to place a point light. Drag to orbit, pinch to zoom.",
            )
        },
    )
}

private fun Offset.toGestureLightPosition(size: IntSize): Float3? {
    if (size.width <= 0 || size.height <= 0) return null
    val scale = minOf(size.width, size.height).toFloat()
    val normalizedX = ((2f * x) - size.width) / scale
    val normalizedY = (size.height - (2f * y)) / scale
    val radial = normalizedX * normalizedX + normalizedY * normalizedY
    val hemisphereZ = if (radial <= 1f) sqrt(1f - radial) else 0f
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

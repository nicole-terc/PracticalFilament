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
import dev.nstv.practicalfilament.components.materials.textured.brownMudLeavesMaterial
import dev.nstv.practicalfilament.components.materials.textured.monkeyMaterial
import dev.nstv.practicalfilament.components.materials.textured.mossMaterial
import dev.nstv.practicalfilament.components.materials.textured.ratMaterial
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
import practicalfilament.composeapp.generated.resources.Res
import dev.nstv.practicalfilament.screen.marbles.components.EnvironmentSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.MeshSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.MeshList
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import kotlin.math.sqrt

private const val MarbleTextureMinCameraDistance = 2.2f
private const val MarbleTextureMaxCameraDistance = 8f
private val MarbleTextureBaseCamera = CameraConfig(
    position = Float3(0f, 0.04f, 3.35f),
    lookAt = Float3(0f, 0f, 0f),
)


private val MarbleTextureMaterials = listOf(
    brownMudLeavesMaterial(),
    mossMaterial(),
    monkeyMaterial(),
    ratMaterial(),
)

@Composable
fun MarbleTextureScreen(
    modifier: Modifier = Modifier,
) {
    var filamentEngine by remember { mutableStateOf<FilamentEngine?>(null) }
    var selectedMesh by remember { mutableStateOf(MeshList.first()) }
    var selectedMaterialIndex by remember { mutableIntStateOf(0) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var orientation by remember { mutableStateOf(OrbitQuaternion.Identity) }
    var cameraDistance by remember { mutableStateOf(MarbleTextureBaseCamera.orbitDistance()) }
    var renderableHandle by remember { mutableIntStateOf(0) }
    var gestureLightHandle by remember { mutableIntStateOf(0) }
    var gestureLightPosition by remember { mutableStateOf<Float3?>(null) }
    var gestureLightScreenPosition by remember { mutableStateOf<Offset?>(null) }
    var gestureLightVersion by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filamentEngine, gestureLightVersion) {
        val currentEngine = filamentEngine ?: return@LaunchedEffect
        if (gestureLightHandle != 0) {
            currentEngine.removeLight(gestureLightHandle)
            gestureLightHandle = 0
        }
        val position = gestureLightPosition ?: return@LaunchedEffect
        gestureLightHandle = currentEngine.addLight(
            LightConfig(
                type = LightType.POINT,
                color = FilamentColor(1f, 1f, 1f),
                intensity = 600_000f,
                position = position,
                falloffRadius = 10f,
            ),
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(filamentEngine, orientation, cameraDistance) {
        val engine = filamentEngine ?: return@LaunchedEffect
        engine.updateCamera(
            orbitCameraConfig(
                baseCamera = MarbleTextureBaseCamera,
                orientation = orientation,
                distance = cameraDistance,
            )
        )
        engine.requestFrame()
    }


    fun refreshScene(){
        val engine = filamentEngine ?: return

        if (renderableHandle != 0) {
            engine.removeRenderable(renderableHandle)
            renderableHandle = 0
        }

        val material = MarbleTextureMaterials[selectedMaterialIndex]
        val loaded = engine.loadMaterial(material)
        notice = when {
            loaded.instanceHandle <= 0 -> "The textured material could not be loaded."
            loaded.textureHandles.size != material.textureBindings.size ->
                "Some textures could not be loaded."

            else -> null
        }
        if (loaded.instanceHandle <= 0) {
            return
        }
        loaded.parameters.values
            .filter { parameter -> parameter.value !is BuiltInTexture }
            .forEach { parameter ->
                engine.setMaterialParameter(loaded.instanceHandle, parameter)
            }
        renderableHandle = engine.loadMesh(
            path = Res.getUri(selectedMesh.path),
            materialInstanceHandle = loaded.instanceHandle,
            scale = selectedMesh.scale,
        )
        if (renderableHandle <= 0) {
            notice =
                "The textured ${selectedMesh.name} could not be created on this platform."
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
        title = "Marble Texture",
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
                        minDistance = MarbleTextureMinCameraDistance,
                        maxDistance = MarbleTextureMaxCameraDistance,
                        enabled = renderableHandle > 0,
                    ),
                camera = orbitCameraConfig(
                    baseCamera = MarbleTextureBaseCamera,
                    orientation = orientation,
                    distance = cameraDistance,
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        color = FilamentColor(1f, 0.96f, 0.92f),
                        intensity = 110_000f,
                        direction = Float3(-0.5f, -0.72f, -0.42f),
                    ),
                    LightConfig(
                        type = LightType.DIRECTIONAL,
                        color = FilamentColor(0.82f, 0.88f, 1f),
                        intensity = 28_000f,
                        direction = Float3(0.72f, -0.18f, -0.66f),
                    ),
                ),
                backgroundColor = FilamentColor(0.16f, 0.18f, 0.27f, 1f),
                onEngineReady = { engine ->
                    filamentEngine = engine
                    val material = MarbleTextureMaterials[selectedMaterialIndex]
                    val loaded = engine.loadMaterial(material)
                    notice = when {
                        loaded.instanceHandle <= 0 -> "The textured material could not be loaded."
                        loaded.textureHandles.size != material.textureBindings.size ->
                            "Some textures could not be loaded."

                        else -> null
                    }
                    if (loaded.instanceHandle > 0) {
                        loaded.parameters.values
                            .filter { parameter -> parameter.value !is BuiltInTexture }
                            .forEach { parameter ->
                                engine.setMaterialParameter(loaded.instanceHandle, parameter)
                            }
                    }
                    renderableHandle = if (loaded.instanceHandle > 0) {
                        engine.loadMesh(
                            path = Res.getUri(selectedMesh.path),
                            materialInstanceHandle = loaded.instanceHandle,
                            scale = selectedMesh.scale,
                        )
                    } else {
                        -1
                    }
                    if (renderableHandle <= 0 && loaded.instanceHandle > 0) {
                        notice = "The textured sphere could not be created on this platform."
                    }
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
            MeshSelectionField(
                onMeshSelectionChanged = {
                    selectedMesh = it
                    refreshScene()
                },
            )
            DropDownWithArrows(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Grid.One, bottom = Grid.One),
                options = MarbleTextureMaterials.map { it.label },
                selectedIndex = selectedMaterialIndex,
                label = "Material",
                onSelectionChanged = { index ->
                    selectedMaterialIndex = index
                    refreshScene()
                },
            )

            EnvironmentSelectionField(
                filamentEngine = filamentEngine,
                updateNotice = { notice = it },
                selectedBackground = 8,
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Grid.One),
                text = "Double-tap the sphere to place a gesture light.",
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Grid.One),
                text = "Drag to orbit and pinch to zoom.",
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

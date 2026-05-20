package dev.nstv.practicalfilament.screen.otherViewers

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.nstv.practicalfilament.filament.withFrameSeconds
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.utils.OrbitQuaternion
import dev.nstv.practicalfilament.components.utils.orbitCameraControls
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.screen.marbles.components.EnvironmentSelectionField
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import dev.nstv.practicalfilament.theme.components.SampleNotice
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import practicalfilament.composeapp.generated.resources.Res

private enum class SampleGltfAsset(
    val label: String,
    val path: String,
) {
    HELMET(
        label = "helmet",
        path = "files/models/helmet.glb",
    ),
    DRONE(
        label = "drone",
        path = "files/models/BusterDrone/scene.gltf",
    ),
    FOX(
        label = "fox",
        path = "files/models/fox/Fox.gltf",
    ),
    SHEEP(
        label = "sheep",
        path = "files/models/sheep/scene.gltf",
    ),
    SHEEP_2(
        label = "sheep 2",
        path = "files/models/sheep2/scene.gltf",
    ),
    LUCY(
        label = "lucy",
        path = "files/models/lucy/lucy.glb",
    ),
}

@Composable
fun GltfViewerScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var assetHandle by remember { mutableIntStateOf(0) }
    var animationCount by remember { mutableIntStateOf(0) }
    var animationIndex by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    var animationTime by remember { mutableFloatStateOf(0f) }
    var selectedAsset by remember { mutableStateOf(SampleGltfAsset.SHEEP) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var orientation by remember { mutableStateOf(OrbitQuaternion()) }
    var cameraDistance by remember { mutableFloatStateOf(4f) }
    var shrinking by rememberSaveable { mutableStateOf(false) }
    var appearing by rememberSaveable { mutableStateOf(false) }
    var shrinkElapsedSeconds by rememberSaveable { mutableFloatStateOf(0f) }
    var hidden by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(engine, selectedAsset) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (assetHandle > 0) {
            currentEngine.removeGltfFromScene(assetHandle)
            currentEngine.destroyGltfAsset(assetHandle)
            assetHandle = 0
        }
        animationCount = 0
        animationIndex = 0
        notice = null
        val nextHandle = currentEngine.loadGltfAsset(Res.getUri(selectedAsset.path))
        if (nextHandle <= 0) {
            notice = "The bundled ${selectedAsset.label} asset could not be loaded."
            return@LaunchedEffect
        }
        shrinking = false
        appearing = false
        shrinkElapsedSeconds = 0f
        hidden = false
        assetHandle = nextHandle
        currentEngine.transformGltfToUnitCube(assetHandle)
        currentEngine.setGltfTransform(assetHandle, gltfTapVisibleTransform())
        currentEngine.addGltfToScene(assetHandle)
        animationCount = currentEngine.getGltfAnimationCount(assetHandle)
        if (animationCount == 0) {
            notice =
                "${selectedAsset.label} loaded successfully. This asset does not contain animations."
        }
    }

    LaunchedEffect(engine, orientation, cameraDistance) {
        val currentEngine = engine ?: return@LaunchedEffect
        currentEngine.updateCamera(gltfCameraForOrientation(orientation, cameraDistance))
    }

    LaunchedEffect(engine, assetHandle, animationCount, animationIndex, shrinking, appearing, hidden) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (assetHandle <= 0) return@LaunchedEffect
        withFrameSeconds { elapsed, deltaSeconds ->
            if (animationCount > 0) {
                val duration =
                    currentEngine.getGltfAnimationDuration(assetHandle, animationIndex)
                        .coerceAtLeast(0.01f)
                animationTime = elapsed % duration
                currentEngine.applyGltfAnimation(assetHandle, animationIndex, animationTime)
                currentEngine.updateGltfBoneMatrices(assetHandle)
            } else {
                animationTime = 0f
            }

            if (shrinking) {
                shrinkElapsedSeconds += deltaSeconds
                val progress = (shrinkElapsedSeconds / GltfTapVisibilityDurationSeconds).coerceIn(0f, 1f)
                currentEngine.setGltfTransform(
                    assetHandle,
                    gltfTapLocalTransform(progress = progress),
                )
                if (progress >= 1f) {
                    shrinking = false
                    hidden = true
                    currentEngine.setGltfTransform(assetHandle, gltfTapHiddenTransform())
                }
            } else if (appearing) {
                shrinkElapsedSeconds += deltaSeconds
                val progress = (1f - (shrinkElapsedSeconds / GltfTapVisibilityDurationSeconds))
                    .coerceIn(0f, 1f)
                currentEngine.setGltfTransform(
                    assetHandle,
                    gltfTapLocalTransform(progress = progress),
                )
                if (progress <= 0f) {
                    appearing = false
                    hidden = false
                    currentEngine.setGltfTransform(assetHandle, gltfTapVisibleTransform())
                }
            } else if (hidden) {
                currentEngine.setGltfTransform(assetHandle, gltfTapHiddenTransform())
            } else {
                currentEngine.setGltfTransform(assetHandle, gltfTapVisibleTransform())
            }
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "glTF Viewer",
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
                    )
                    .pointerInput(engine, assetHandle, shrinking, appearing, hidden) {
                        detectTapGestures(
                            onTap = { offset ->
                                val currentEngine = engine ?: return@detectTapGestures
                                if (assetHandle <= 0 || shrinking || appearing) return@detectTapGestures
                                if (hidden) {
                                    appearing = true
                                    shrinkElapsedSeconds = 0f
                                    return@detectTapGestures
                                }
                                currentEngine.pickRenderable(offset.x.toInt(), offset.y.toInt()) { result ->
                                    if (result == null || shrinking || appearing || hidden) return@pickRenderable
                                    shrinking = true
                                    shrinkElapsedSeconds = 0f
                                }
                            }
                        )
                    },
                camera = gltfCameraForOrientation(orientation, cameraDistance),
                lights = listOf(
                    LightConfig(
                        type = LightType.SUN,
                        intensity = 85_000f,
                        direction = Float3(0.4f, -1f, -0.6f),
                    ),
                ),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    readyEngine.updateCamera(gltfCameraForOrientation(orientation, cameraDistance))
                    engine = readyEngine
                },
            )
        },
        controls = {
            notice?.let { SampleNotice(it) }
            DropDownWithArrows(
                label = "model",
                options = SampleGltfAsset.entries.map { it.label },
                selectedIndex = SampleGltfAsset.entries.indexOf(selectedAsset),
                onSelectionChanged = { selectedAsset = SampleGltfAsset.entries[it] },
            )
            EnvironmentSelectionField(
                filamentEngine = engine,
                updateNotice = { notice = it },
                selectedBackground = 7
            )
            if (assetHandle > 0 && animationCount > 0) {
                if (animationCount > 1) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { animationIndex = (animationIndex + 1) % animationCount },
                    ) {
                        Text("Next animation")
                    }
                }
                Text("Animation time: ${animationTime.toString().take(4)}s")
            }
        },
    )
}

private fun gltfCameraForOrientation(
    orientation: OrbitQuaternion,
    cameraDistance: Float
): CameraConfig {
    val orbit = orientation.conjugate()
    return CameraConfig(
        position = orbit.rotate(Float3(0f, 0.5f, cameraDistance)),
        lookAt = Float3(0f, 0f, 0f),
        up = orbit.rotate(Float3(0f, 1f, 0f)),
        fovDegrees = 28.0,
    )
}

// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-gltf-viewer
package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import practicalfilament.composeapp.generated.resources.Res
import kotlin.math.sqrt

private const val DemoIblPath = "files/envs/pillars_2k/pillars_2k_ibl.ktx"
private const val DemoSkyboxPath = "files/envs/pillars_2k/pillars_2k_skybox.ktx"

private enum class SampleGltfAsset(
    val label: String,
    val path: String,
) {
    HELMET(
        label = "helmet.glb",
        path = "files/models/helmet.glb",
    ),
    SCENE(
        label = "scene.gltf",
        path = "files/models/BusterDrone/scene.gltf",
    ),
}

@Composable
fun GltfViewerScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var assetHandle by remember { mutableIntStateOf(0) }
    var animationCount by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    var animationTime by remember { mutableFloatStateOf(0f) }
    var selectedAsset by remember { mutableStateOf(SampleGltfAsset.HELMET) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var lastDragPoint by remember { mutableStateOf<Float3?>(null) }
    var orientation by remember { mutableStateOf(initialGltfOrientation()) }

    LaunchedEffect(engine, selectedAsset) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (assetHandle > 0) {
            currentEngine.removeGltfFromScene(assetHandle)
            currentEngine.destroyGltfAsset(assetHandle)
            assetHandle = 0
        }
        animationCount = 0
        notice = null
        val nextHandle = currentEngine.loadGltfAsset(Res.getUri(selectedAsset.path))
        if (nextHandle <= 0) {
            notice = "The bundled ${selectedAsset.label} asset could not be loaded."
            return@LaunchedEffect
        }
        assetHandle = nextHandle
        currentEngine.transformGltfToUnitCube(assetHandle)
        currentEngine.addGltfToScene(assetHandle)
        animationCount = currentEngine.getGltfAnimationCount(assetHandle)
        if (animationCount == 0) {
            notice = "${selectedAsset.label} loaded successfully. This asset does not contain animations."
        }
    }

    LaunchedEffect(engine, orientation) {
        val currentEngine = engine ?: return@LaunchedEffect
        currentEngine.updateCamera(gltfCameraForOrientation(orientation))
    }

    LaunchedEffect(engine, assetHandle, animationCount) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (assetHandle <= 0) return@LaunchedEffect
        if (animationCount == 0) return@LaunchedEffect
        while (true) {
            val time = withFrameNanos { it } / 1_000_000_000f
            val duration = currentEngine.getGltfAnimationDuration(assetHandle, 0).coerceAtLeast(0.01f)
            animationTime = time % duration
            currentEngine.applyGltfAnimation(assetHandle, 0, animationTime)
            currentEngine.updateGltfBoneMatrices(assetHandle)
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "glTF Viewer",
        description = "This follows the Android sample asset setup more closely and now lets you switch between the sample `helmet.glb` and `scene.gltf` bundle.",
        view = {
            FilamentView(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(viewportSize, selectedAsset) {
                        detectDragGestures(
                            onDragStart = { start ->
                                lastDragPoint = start.toGltfArcballPoint(viewportSize)
                            },
                            onDragEnd = {
                                lastDragPoint = null
                            },
                            onDragCancel = {
                                lastDragPoint = null
                            },
                        ) { change, _ ->
                            val previousPoint = lastDragPoint
                                ?: change.previousPosition.toGltfArcballPoint(viewportSize)
                            val currentPoint = change.position.toGltfArcballPoint(viewportSize)
                            if (previousPoint != null && currentPoint != null) {
                                orientation = gltfArcballDelta(previousPoint, currentPoint) * orientation
                                lastDragPoint = currentPoint
                            }
                            change.consume()
                        }
                    },
                camera = gltfCameraForOrientation(orientation),
                lights = listOf(
                    LightConfig(
                        type = LightType.SUN,
                        intensity = 85_000f,
                        direction = Float3(0.4f, -1f, -0.6f),
                    ),
                ),
                backgroundColor = Color(0.03f, 0.03f, 0.05f, 1f),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    val indirectLightHandle = readyEngine.loadIndirectLight(Res.getUri(DemoIblPath))
                    if (indirectLightHandle > 0) {
                        readyEngine.setIndirectLight(indirectLightHandle, intensity = 30_000f)
                    }
                    val skyboxHandle = readyEngine.loadSkybox(Res.getUri(DemoSkyboxPath))
                    if (skyboxHandle > 0) {
                        readyEngine.setSkybox(skyboxHandle)
                    }
                    readyEngine.updateCamera(gltfCameraForOrientation(orientation))
                    engine = readyEngine
                },
            )
        },
        controls = {
            notice?.let { SampleNotice(it) }
            DropDownWithArrows(
                options = SampleGltfAsset.entries.map { it.label },
                selectedIndex = SampleGltfAsset.entries.indexOf(selectedAsset),
                onSelectionChanged = { selectedAsset = SampleGltfAsset.entries[it] },
            )
            if (assetHandle > 0 && animationCount > 0) {
                Text("Animation time: ${animationTime.toString().take(4)}s")
            }
            Text("Drag to orbit the camera, matching the Android sample's touch manipulator.")
        },
    )
}

private data class GltfQuaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    operator fun times(other: GltfQuaternion): GltfQuaternion {
        return multiplyRaw(other).normalized()
    }

    private fun multiplyRaw(other: GltfQuaternion): GltfQuaternion {
        return GltfQuaternion(
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w,
            w = w * other.w - x * other.x - y * other.y - z * other.z,
        )
    }

    fun normalized(): GltfQuaternion {
        val magnitude = sqrt(x * x + y * y + z * z + w * w)
        if (magnitude <= 1e-6f) return GltfQuaternion(0f, 0f, 0f, 1f)
        return GltfQuaternion(x / magnitude, y / magnitude, z / magnitude, w / magnitude)
    }

    fun conjugate(): GltfQuaternion = GltfQuaternion(-x, -y, -z, w)

    fun rotate(vector: Float3): Float3 {
        val quaternionVector = GltfQuaternion(vector.x, vector.y, vector.z, 0f)
        val rotated = multiplyRaw(quaternionVector).multiplyRaw(conjugate())
        return Float3(rotated.x, rotated.y, rotated.z)
    }
}

private fun initialGltfOrientation(): GltfQuaternion {
    return GltfQuaternion(0f, 0f, 0f, 1f)
}

private fun gltfCameraForOrientation(orientation: GltfQuaternion): CameraConfig {
    val orbit = orientation.conjugate()
    return CameraConfig(
        position = orbit.rotate(Float3(0f, 0.5f, 4f)),
        lookAt = Float3(0f, 0f, 0f),
        up = orbit.rotate(Float3(0f, 1f, 0f)),
        fovDegrees = 28.0,
    )
}

private fun gltfArcballDelta(from: Float3, to: Float3): GltfQuaternion {
    val dot = (from gltfDot to).coerceIn(-1f, 1f)
    val cross = from gltfCross to
    val magnitude = cross.gltfLength()
    if (magnitude <= 1e-6f) {
        return if (dot < 0f) {
            val fallbackAxis = if (kotlin.math.abs(from.x) < 0.9f) {
                Float3(1f, 0f, 0f) gltfCross from
            } else {
                Float3(0f, 1f, 0f) gltfCross from
            }.gltfNormalized()
            GltfQuaternion(fallbackAxis.x, fallbackAxis.y, fallbackAxis.z, 0f)
        } else {
            GltfQuaternion(0f, 0f, 0f, 1f)
        }
    }
    return GltfQuaternion(
        x = cross.x,
        y = cross.y,
        z = cross.z,
        w = 1f + dot,
    ).normalized()
}

private fun Offset.toGltfArcballPoint(viewportSize: IntSize): Float3? {
    if (viewportSize.width <= 0 || viewportSize.height <= 0) return null
    val x = ((this.x / viewportSize.width.toFloat()) * 2f) - 1f
    val y = 1f - ((this.y / viewportSize.height.toFloat()) * 2f)
    val lengthSquared = x * x + y * y
    return if (lengthSquared <= 1f) {
        Float3(x, y, sqrt(1f - lengthSquared))
    } else {
        val invLength = 1f / sqrt(lengthSquared)
        Float3(x * invLength, y * invLength, 0f)
    }
}

private infix fun Float3.gltfDot(other: Float3): Float {
    return x * other.x + y * other.y + z * other.z
}

private infix fun Float3.gltfCross(other: Float3): Float3 {
    return Float3(
        x = y * other.z - z * other.y,
        y = z * other.x - x * other.z,
        z = x * other.y - y * other.x,
    )
}

private fun Float3.gltfLength(): Float {
    return sqrt(x * x + y * y + z * z)
}

private fun Float3.gltfNormalized(): Float3 {
    val length = gltfLength()
    if (length <= 1e-6f) return Float3(0f, 0f, 1f)
    return Float3(x / length, y / length, z / length)
}

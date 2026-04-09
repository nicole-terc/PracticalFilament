package dev.nstv.practicalfilament.screen.filament.steps.step3

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.utils.OrbitQuaternion
import dev.nstv.practicalfilament.components.utils.orbitCameraConfig
import dev.nstv.practicalfilament.components.utils.orbitCameraControls
import dev.nstv.practicalfilament.components.utils.orbitDistance
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.screen.marbles.components.MeshSelectionField
import dev.nstv.practicalfilament.screen.marbles.components.NeutralSphereMaterial
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera
import dev.nstv.practicalfilament.screen.marbles.components.SphereMesh
import dev.nstv.practicalfilament.screen.marbles.components.SphereStepLights
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import practicalfilament.composeapp.generated.resources.Res

private const val FilamentStepThreeMinCameraDistance = 2.2f
private const val FilamentStepThreeMaxCameraDistance = 8f

@Composable
internal fun FilamentStepThree(
    modifier: Modifier = Modifier,
) {
    var showLight by remember { mutableStateOf(true) }
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var selectedMesh by remember { mutableStateOf(SphereMesh) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var orientation by remember { mutableStateOf(OrbitQuaternion.Identity) }
    var cameraDistance by remember { mutableStateOf(SingleMarbleCamera.orbitDistance()) }
    var renderableHandle by remember { mutableIntStateOf(0) }

    fun refreshScene(currentEngine: FilamentEngine) {
        if (renderableHandle != 0) {
            currentEngine.removeRenderable(renderableHandle)
            renderableHandle = 0
        }
        val loaded = currentEngine.loadMaterial(NeutralSphereMaterial)
        loaded.parameters.values.forEach { parameter ->
            currentEngine.setMaterialParameter(loaded.instanceHandle, parameter)
        }
        renderableHandle = currentEngine.loadMesh(
            path = Res.getUri(selectedMesh.path),
            materialInstanceHandle = loaded.instanceHandle,
            scale = selectedMesh.scale,
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(engine, orientation, cameraDistance) {
        val currentEngine = engine ?: return@LaunchedEffect
        currentEngine.updateCamera(
            orbitCameraConfig(
                baseCamera = SingleMarbleCamera,
                orientation = orientation,
                distance = cameraDistance,
            )
        )
        currentEngine.requestFrame()
    }

    LaunchedEffect(engine, showLight) {
        val currentEngine = engine ?: return@LaunchedEffect
        currentEngine.clearLights()
        if (showLight) {
            SphereStepLights.forEach(currentEngine::addLight)
        }
        currentEngine.requestFrame()
    }

    SampleScreenLayout(
        title = "3. Geometry",
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
                        minDistance = FilamentStepThreeMinCameraDistance,
                        maxDistance = FilamentStepThreeMaxCameraDistance,
                        enabled = renderableHandle > 0,
                    ),
                camera = orbitCameraConfig(
                    baseCamera = SingleMarbleCamera,
                    orientation = orientation,
                    distance = cameraDistance,
                ),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    refreshScene(readyEngine)
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
            MeshSelectionField(
                selectedMesh = selectedMesh,
                onMeshSelectionChanged = { mesh ->
                    selectedMesh = mesh
                    engine?.let(::refreshScene)
                },
            )
        },
    )
}

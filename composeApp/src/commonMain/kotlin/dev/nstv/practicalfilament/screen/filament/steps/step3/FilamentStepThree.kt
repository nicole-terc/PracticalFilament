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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackgroundFilament
import dev.nstv.practicalfilament.screen.marbles.components.NeutralSphereMaterial
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera
import dev.nstv.practicalfilament.screen.marbles.components.SphereStepLights
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout

@Composable
internal fun FilamentStepThree(
    modifier: Modifier = Modifier,
) {
    var showLight by remember { mutableStateOf(true) }
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }

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
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = SingleMarbleCamera,
                backgroundColor = MarbleUiBackgroundFilament,
                onEngineReady = { readyEngine ->
                    val loaded = readyEngine.loadMaterial(NeutralSphereMaterial)
                    loaded.parameters.values.forEach { parameter ->
                        readyEngine.setMaterialParameter(loaded.instanceHandle, parameter)
                    }
                    readyEngine.createSphereRenderable(
                        materialInstanceHandle = loaded.instanceHandle,
                        radius = 1.1f,
                    )
                    engine = readyEngine
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
        },
    )
}

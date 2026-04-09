package dev.nstv.practicalfilament.screen.filament.steps.step4

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
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.screen.filament.steps.components.FilamentStepView
import dev.nstv.practicalfilament.screen.marbles.components.CeramicPresetIndex
import dev.nstv.practicalfilament.screen.marbles.components.MarblePresets
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackgroundFilament
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera
import dev.nstv.practicalfilament.screen.marbles.components.SphereStepLights
import dev.nstv.practicalfilament.theme.Grid

@Composable
internal fun FilamentStepFour(
    modifier: Modifier = Modifier,
) {
    var showLight by remember { mutableStateOf(false) }
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    val material = MarblePresets[CeramicPresetIndex]

    LaunchedEffect(engine, showLight) {
        val e = engine ?: return@LaunchedEffect
        e.clearLights()
        if (showLight) SphereStepLights.forEach { e.addLight(it) }
        e.requestFrame()
    }

    FilamentStepView(
        modifier = modifier,
        footer = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showLight, onCheckedChange = { showLight = it })
                Text(
                    text = "Show light",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = Grid.One),
                )
            }
        },
    ) {
        FilamentView(
            modifier = Modifier.fillMaxSize(),
            camera = SingleMarbleCamera,
            backgroundColor = MarbleUiBackgroundFilament,
            clipShape = FilamentClipShape.Circle,
            onEngineReady = { e ->
                val loaded = e.loadMaterial(material)
                loaded.parameters.values.forEach {
                    e.setMaterialParameter(loaded.instanceHandle, it)
                }
                e.createSphereRenderable(
                    materialInstanceHandle = loaded.instanceHandle,
                    radius = 1.1f,
                )
                engine = e
            },
        )
    }
}

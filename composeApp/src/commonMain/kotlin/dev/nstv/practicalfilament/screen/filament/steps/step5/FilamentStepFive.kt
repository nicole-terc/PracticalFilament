package dev.nstv.practicalfilament.screen.filament.steps.step5

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.withFrameSeconds
import dev.nstv.practicalfilament.screen.filament.steps.components.FilamentStepView
import dev.nstv.practicalfilament.screen.marbles.components.CeramicPresetIndex
import dev.nstv.practicalfilament.screen.marbles.components.MarbleAliveLights
import dev.nstv.practicalfilament.screen.marbles.components.MarblePresets
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackgroundFilament
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera

@Composable
internal fun FilamentStepFive(
    modifier: Modifier = Modifier,
) {
    val material = MarblePresets[CeramicPresetIndex]
    var engineReady by remember { mutableStateOf<FilamentEngine?>(null) }
    var renderableHandle by remember { mutableIntStateOf(0) }

    LaunchedEffect(engineReady, renderableHandle) {
        val engine = engineReady ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        withFrameSeconds { elapsed, _ ->
            engine.setRenderableRotation(
                renderableHandle,
                -12f + 2f,
                24f + elapsed * 18f,
            )
            engine.requestFrame()
        }
    }

    FilamentStepView(modifier = modifier) {
        FilamentView(
            modifier = Modifier.fillMaxSize(),
            camera = SingleMarbleCamera,
            lights = MarbleAliveLights,
            backgroundColor = MarbleUiBackgroundFilament,
            clipShape = FilamentClipShape.Circle,
            onEngineReady = { engine ->
                val loaded = engine.loadMaterial(material)
                loaded.parameters.values.forEach {
                    engine.setMaterialParameter(loaded.instanceHandle, it)
                }
                engineReady = engine
                renderableHandle = engine.createSphereRenderable(
                    materialInstanceHandle = loaded.instanceHandle,
                    radius = 1.1f,
                )
            },
        )
    }
}

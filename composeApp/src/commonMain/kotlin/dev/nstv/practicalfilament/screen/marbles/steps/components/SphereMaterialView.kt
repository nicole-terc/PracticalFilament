package dev.nstv.practicalfilament.screen.marbles.steps.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.nstv.practicalfilament.filament.withFrameSeconds
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.DefaultSkyboxColor
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.material.Material

@Composable
internal fun SphereMaterialView(
    material: Material,
    camera: CameraConfig,
    lights: List<LightConfig>,
    radius: Float,
    initialRotationX: Float,
    initialRotationY: Float,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = false,
) {
    var engineReady by remember { mutableStateOf<FilamentEngine?>(null) }
    var renderableHandle by remember { mutableIntStateOf(0) }

    LaunchedEffect(engineReady, renderableHandle, initialRotationX, initialRotationY) {
        val engine = engineReady ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        engine.setRenderableRotation(renderableHandle, initialRotationX, initialRotationY)
        engine.requestFrame()
    }

    LaunchedEffect(engineReady, renderableHandle, autoRotate, initialRotationX, initialRotationY) {
        val engine = engineReady ?: return@LaunchedEffect
        if (renderableHandle == 0 || !autoRotate) return@LaunchedEffect
        withFrameSeconds { elapsed, _ ->
            engine.setRenderableRotation(
                renderableHandle,
                initialRotationX + 2f,
                initialRotationY + elapsed * 18f,
            )
            engine.requestFrame()
        }
    }

    FilamentView(
        modifier = modifier,
        camera = camera,
        lights = lights,
        backgroundColor = DefaultSkyboxColor,
        clipShape = FilamentClipShape.Circle,
        onEngineReady = { engine ->
            val loaded = engine.loadMaterial(material)
            loaded.parameters.values.forEach {
                engine.setMaterialParameter(
                    loaded.instanceHandle,
                    it
                )
            }
            engineReady = engine
            renderableHandle = engine.createSphereRenderable(
                materialInstanceHandle = loaded.instanceHandle,
                radius = radius,
            )
        },
    )
}

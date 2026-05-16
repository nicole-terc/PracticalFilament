package dev.nstv.practicalfilament.screen.marbles.steps.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.withFrameSeconds
import dev.nstv.practicalfilament.filament.material.Material
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackground

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
    val useComposeClip = marbleSphereUseComposeClip()
    val composeBackgroundColor = marbleSphereComposeBackgroundColor()
    val visibleCircleFraction = marbleSphereVisibleCircleFraction()

    LaunchedEffect(engineReady, renderableHandle, initialRotationX, initialRotationY) {
        val engine = engineReady ?: return@LaunchedEffect
        if (renderableHandle == 0) return@LaunchedEffect
        engine.setRenderableRotation(renderableHandle, initialRotationX, initialRotationY)
        engine.requestFrame()
    }

    LaunchedEffect(engineReady, lights) {
        val engine = engineReady ?: return@LaunchedEffect
        engine.clearLights()
        lights.forEach(engine::addLight)
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

    Box(
        modifier = modifier
            .then(if (useComposeClip) Modifier.clip(CircleShape) else Modifier)
            .then(composeBackgroundColor?.let { Modifier.background(it) } ?: Modifier),
    ) {
        FilamentView(
            modifier = Modifier.fillMaxSize(),
            camera = camera,
            lights = emptyList(),
            backgroundColor = marbleSphereBackgroundColor(),
            clipShape = marbleSphereNativeClipShape(),
            isOpaque = marbleSphereIsOpaque(),
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
        if (visibleCircleFraction < 0.999f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
            ) {
                drawCircle(color = MarbleUiBackground, radius = size.minDimension * 0.5f, center = center)
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    radius = size.minDimension * 0.5f * visibleCircleFraction,
                    center = center,
                    blendMode = BlendMode.Clear,
                )
            }
        }
    }
}

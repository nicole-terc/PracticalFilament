// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-lit-cube
package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.MorphingMaterial
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.material.loadMaterialOnEngine

@Composable
fun LitCubeScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var cubeHandle by remember { mutableIntStateOf(0) }
    var planeHandle by remember { mutableIntStateOf(0) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var exposure by remember { mutableFloatStateOf(1f) }
    var rotationSpeed by remember { mutableFloatStateOf(28f) }
    var supportNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine, cubeHandle, rotationSpeed) {
        if (cubeHandle == 0) return@LaunchedEffect
        while (true) {
            val time = withFrameNanos { it } / 1_000_000_000f
            engine?.setRenderableRotation(cubeHandle, 20f, time * rotationSpeed)
        }
    }

    LaunchedEffect(engine, exposure) {
        val aperture = 16f
        val shutterSpeed = 1f / 125f
        val sensitivity = 100f / exposure.coerceAtLeast(0.2f)
        engine?.setCameraExposure(aperture, shutterSpeed, sensitivity)
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Lit Cube",
        description = "A shadow-casting cube lit like the original Filament sample, with exposure control and a receiving plane.",
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize().onSizeChanged { viewportSize = it },
                camera = CameraConfig(
                    position = Float3(3f, 2.2f, 5f),
                    lookAt = Float3(0f, 0.4f, 0f),
                    fovDegrees = 32.0,
                ),
                lights = listOf(
                    LightConfig(
                        type = LightType.SUN,
                        intensity = 110_000f,
                        direction = Float3(-0.7f, -1f, -0.5f),
                        castShadows = true,
                    ),
                ),
                backgroundColor = Color(0.04f, 0.05f, 0.07f, 1f),
                onEngineReady = { readyEngine ->
                    val (instanceHandle, _, parameters) = loadMaterialOnEngine(readyEngine, MorphingMaterial)
                    parameters.values.forEach { readyEngine.setMaterialParameter(instanceHandle, it) }
                    engine = readyEngine
                    cubeHandle = readyEngine.createCubeRenderable(instanceHandle, size = 1.5f)
                    planeHandle = readyEngine.createPlaneRenderable(instanceHandle, width = 8f, height = 8f)
                    if (cubeHandle <= 0) {
                        supportNotice = "This sample needs cube geometry support from the platform engine."
                        return@FilamentView
                    }
                    readyEngine.setRenderableTransform(
                        cubeHandle,
                        floatArrayOf(
                            1f, 0f, 0f, 0f,
                            0f, 1f, 0f, 0f,
                            0f, 0f, 1f, 0f,
                            0f, 0.8f, 0f, 1f,
                        ),
                    )
                    readyEngine.setRenderableTransform(
                        planeHandle,
                        floatArrayOf(
                            1f, 0f, 0f, 0f,
                            0f, 1f, 0f, 0f,
                            0f, 0f, 1f, 0f,
                            0f, -1.2f, 0f, 1f,
                        ),
                    )
                    readyEngine.setShadowsEnabled(cubeHandle, castShadows = true, receiveShadows = true)
                    readyEngine.setShadowsEnabled(planeHandle, castShadows = false, receiveShadows = true)
                    readyEngine.setCameraExposure(16f, 1f / 125f, 100f)
                },
            )
        },
        controls = {
            supportNotice?.let { SampleNotice(it) }
            Text("Exposure")
            Slider(
                value = exposure,
                valueRange = 0.4f..2.5f,
                onValueChange = { exposure = it },
            )
            Text("Rotation Speed")
            Slider(
                value = rotationSpeed,
                valueRange = 0f..72f,
                onValueChange = { rotationSpeed = it },
            )
            if (viewportSize != IntSize.Zero) {
                Text("Viewport: ${viewportSize.width} x ${viewportSize.height}")
            }
        },
    )
}

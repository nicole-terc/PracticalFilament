// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-multi-view
package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.components.materials.morphingMaterial
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.filament.ViewportConfig

@Composable
fun MultiViewScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var cubeHandle by remember { mutableIntStateOf(0) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val viewHandles = remember { mutableStateListOf<Int>() }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine, cubeHandle) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (cubeHandle == 0) return@LaunchedEffect
        while (true) {
            val time = withFrameNanos { it } / 1_000_000_000f
            currentEngine.setRenderableRotation(cubeHandle, 15f, time * 22f)
        }
    }

    LaunchedEffect(engine, viewportSize) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (viewportSize == IntSize.Zero) return@LaunchedEffect
        if (viewHandles.isNotEmpty()) {
            viewHandles.forEach(currentEngine::removeView)
            viewHandles.clear()
        }
        val halfWidth = viewportSize.width / 2
        val halfHeight = viewportSize.height / 2
        val cameraConfigs = listOf(
            CameraConfig(position = Float3(0f, 0f, 4f), lookAt = Float3(0f, 0f, 0f), fovDegrees = 30.0),
            CameraConfig(position = Float3(4f, 1.5f, 0f), lookAt = Float3(0f, 0f, 0f), fovDegrees = 35.0),
            CameraConfig(position = Float3(0f, 4f, 0.1f), lookAt = Float3(0f, 0f, 0f), fovDegrees = 28.0),
            CameraConfig(position = Float3(-3f, 2f, -3f), lookAt = Float3(0f, 0f, 0f), fovDegrees = 32.0),
        )
        val viewports = listOf(
            ViewportConfig(0, 0, halfWidth, halfHeight),
            ViewportConfig(halfWidth, 0, viewportSize.width - halfWidth, halfHeight),
            ViewportConfig(0, halfHeight, halfWidth, viewportSize.height - halfHeight),
            ViewportConfig(halfWidth, halfHeight, viewportSize.width - halfWidth, viewportSize.height - halfHeight),
        )
        viewports.forEachIndexed { index, viewport ->
            val handle = currentEngine.createView(viewport)
            if (handle <= 0) {
                notice = "Additional Filament views are not available on this platform engine."
                return@LaunchedEffect
            }
            currentEngine.setViewCamera(handle, cameraConfigs[index])
            currentEngine.setViewPostProcessing(handle, index != 2)
            currentEngine.setViewBlendMode(handle, translucent = false)
            viewHandles += handle
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Multi-View",
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize().onSizeChanged { viewportSize = it },
                camera = CameraConfig(position = Float3(0f, 0f, 4f), lookAt = Float3(0f, 0f, 0f)),
                lights = listOf(
                    LightConfig(
                        type = LightType.SUN,
                        intensity = 95_000f,
                        direction = Float3(0.4f, -1f, -0.6f),
                    ),
                ),
                backgroundColor = Color(0.03f, 0.03f, 0.03f, 1f),
                onEngineReady = { readyEngine ->
                    val loaded = readyEngine.loadMaterial(morphingMaterial())
                    loaded.parameters.values.forEach {
                        readyEngine.setMaterialParameter(loaded.instanceHandle, it)
                    }
                    engine = readyEngine
                    cubeHandle = readyEngine.createCubeRenderable(loaded.instanceHandle, size = 1.6f)
                    if (cubeHandle <= 0) {
                        notice = "This sample currently needs both cube and multi-view engine support."
                    }
                },
            )
        },
        controls = {
            notice?.let { SampleNotice(it) }
            Text("Active views: ${viewHandles.size}")
        },
    )
}

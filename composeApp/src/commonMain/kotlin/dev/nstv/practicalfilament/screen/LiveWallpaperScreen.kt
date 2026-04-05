// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-live-wallpaper
package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.theme.Grid

@Composable
fun LiveWallpaperScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var skyboxHandle by remember { mutableIntStateOf(-1) }

    LaunchedEffect(engine, skyboxHandle) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (skyboxHandle < 0) return@LaunchedEffect
        while (true) {
            withFrameNanos { frameTimeNanos ->
                val seconds = frameTimeNanos / 1_000_000_000.0f
                val hue = (seconds % 10f) / 10f * 360f
                val (r, g, b) = hsvToRgb(hue, 1f, 1f)
                currentEngine.setSkyboxColor(skyboxHandle, r, g, b, 1f)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        FilamentView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            camera = CameraConfig(),
            lights = emptyList(),
            onEngineReady = { readyEngine ->
                engine = readyEngine
                readyEngine.setCameraExposure(16f, 1f / 125f, 100f)
                val handle = readyEngine.createColorSkybox()
                readyEngine.setSkybox(handle)
                skyboxHandle = handle
            },
        )
        SetAsWallpaperButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Grid.Two),
        )
    }
}

@Composable
expect fun SetAsWallpaperButton(modifier: Modifier = Modifier)

private fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Float, Float, Float> {
    val hue = ((h % 360f) + 360f) % 360f
    val chroma = v * s
    val x = chroma * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val match = v - chroma
    val (r1, g1, b1) = when {
        hue < 60f -> Triple(chroma, x, 0f)
        hue < 120f -> Triple(x, chroma, 0f)
        hue < 180f -> Triple(0f, chroma, x)
        hue < 240f -> Triple(0f, x, chroma)
        hue < 300f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    return Triple(r1 + match, g1 + match, b1 + match)
}

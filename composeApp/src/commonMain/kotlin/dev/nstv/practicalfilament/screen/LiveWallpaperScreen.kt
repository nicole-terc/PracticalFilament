// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-live-wallpaper
package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import practicalfilament.composeapp.generated.resources.Res

private const val LiveWallpaperIblPath = "files/envs/pillars_2k/pillars_2k_ibl.ktx"
private const val LiveWallpaperSkyboxPath = "files/envs/pillars_2k/pillars_2k_skybox.ktx"

private val LiveWallpaperLights = listOf(
    LightConfig(
        type = LightType.SUN,
        intensity = 85_000f,
        direction = Float3(0.4f, -1f, -0.6f),
    ),
)

@Composable
fun LiveWallpaperScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var rainbowSkyboxHandle by remember { mutableIntStateOf(-1) }
    var environmentSkyboxHandle by remember { mutableIntStateOf(-1) }
    var indirectLightHandle by remember { mutableIntStateOf(-1) }
    var assetHandle by remember { mutableIntStateOf(0) }
    var animationCount by remember { mutableIntStateOf(0) }
    var animationTime by remember { mutableFloatStateOf(0f) }
    var notice by remember { mutableStateOf<String?>(null) }
    var selectedPreset by remember { mutableStateOf(LiveWallpaperPreset.default) }

    LaunchedEffect(engine, selectedPreset) {
        val currentEngine = engine ?: return@LaunchedEffect
        notice = null
        animationCount = 0
        animationTime = 0f

        if (assetHandle > 0) {
            currentEngine.removeGltfFromScene(assetHandle)
            currentEngine.destroyGltfAsset(assetHandle)
            assetHandle = 0
        }

        if (!selectedPreset.usesModel) {
            if (rainbowSkyboxHandle <= 0) {
                rainbowSkyboxHandle = currentEngine.createColorSkybox()
            }
            if (rainbowSkyboxHandle > 0) {
                currentEngine.setSkybox(rainbowSkyboxHandle)
                if (selectedPreset == LiveWallpaperPreset.CONFIGURED_SKY) {
                    currentEngine.setSkyboxColor(rainbowSkyboxHandle, 0f, 0f, 0f, 1f)
                }
            }
            currentEngine.updateCamera(selectedPreset.cameraAt(seconds = 0f))
            currentEngine.requestFrame()
            return@LaunchedEffect
        }

        if (indirectLightHandle <= 0) {
            indirectLightHandle = currentEngine.loadIndirectLight(Res.getUri(LiveWallpaperIblPath))
        }
        if (indirectLightHandle > 0) {
            currentEngine.setIndirectLight(indirectLightHandle, intensity = 30_000f)
        }

        if (environmentSkyboxHandle <= 0) {
            environmentSkyboxHandle = currentEngine.loadSkybox(Res.getUri(LiveWallpaperSkyboxPath))
        }
        if (environmentSkyboxHandle > 0) {
            currentEngine.setSkybox(environmentSkyboxHandle)
        }

        val nextHandle = currentEngine.loadGltfAsset(Res.getUri(selectedPreset.assetPath ?: return@LaunchedEffect))
        if (nextHandle <= 0) {
            notice = "The bundled ${selectedPreset.label} asset could not be loaded."
            currentEngine.requestFrame()
            return@LaunchedEffect
        }

        assetHandle = nextHandle
        currentEngine.transformGltfToUnitCube(assetHandle)
        currentEngine.addGltfToScene(assetHandle)
        animationCount = currentEngine.getGltfAnimationCount(assetHandle)
        if (animationCount == 0) {
            notice = "${selectedPreset.label} loaded successfully. This preset is currently static."
        }
        currentEngine.updateCamera(selectedPreset.cameraAt(seconds = 0f))
        currentEngine.requestFrame()
    }

    LaunchedEffect(engine, selectedPreset, rainbowSkyboxHandle, assetHandle, animationCount) {
        val currentEngine = engine ?: return@LaunchedEffect
        if (selectedPreset == LiveWallpaperPreset.CONFIGURED_SKY) {
            if (rainbowSkyboxHandle > 0) {
                currentEngine.setSkyboxColor(rainbowSkyboxHandle, 0f, 0f, 0f, 1f)
                currentEngine.requestFrame()
            }
            return@LaunchedEffect
        }
        if (!selectedPreset.usesModel) {
            if (rainbowSkyboxHandle <= 0) return@LaunchedEffect
            while (true) {
                val seconds = withFrameNanos { it } / 1_000_000_000f
                val hue = liveWallpaperHueAt(seconds)
                val (r, g, b) = hsvToRgb(hue, 1f, 1f)
                currentEngine.setSkyboxColor(rainbowSkyboxHandle, r, g, b, 1f)
                currentEngine.requestFrame()
            }
        }

        if (assetHandle <= 0) return@LaunchedEffect
        while (true) {
            val seconds = withFrameNanos { it } / 1_000_000_000f
            currentEngine.updateCamera(selectedPreset.cameraAt(seconds))
            if (animationCount > 0) {
                val duration = currentEngine.getGltfAnimationDuration(assetHandle, 0).coerceAtLeast(0.01f)
                animationTime = seconds % duration
                currentEngine.applyGltfAnimation(assetHandle, 0, animationTime)
                currentEngine.updateGltfBoneMatrices(assetHandle)
            }
            currentEngine.requestFrame()
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "Live Wallpaper",
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = CameraConfig(),
                lights = LiveWallpaperLights,
                backgroundColor = Color(0f, 0f, 0f, 1f),
                onEngineReady = { readyEngine ->
                    engine = readyEngine
                    readyEngine.setCameraExposure(16f, 1f / 125f, 100f)
                    if (rainbowSkyboxHandle <= 0) {
                        rainbowSkyboxHandle = readyEngine.createColorSkybox()
                    }
                    if (rainbowSkyboxHandle > 0 && !selectedPreset.usesModel) {
                        readyEngine.setSkybox(rainbowSkyboxHandle)
                    }
                },
            )
        },
        controls = {
            notice?.let { SampleNotice(it) }
            if (selectedPreset == LiveWallpaperPreset.CONFIGURED_SKY) {
                SampleNotice("Configured Sky uses the saved settings from the Sky sample.")
            }
            DropDownWithArrows(
                options = LiveWallpaperPreset.entries.map { it.label },
                selectedIndex = LiveWallpaperPreset.entries.indexOf(selectedPreset),
                onSelectionChanged = { selectedPreset = LiveWallpaperPreset.entries[it] },
            )
            if (selectedPreset.usesModel && animationCount > 0) {
                androidx.compose.material3.Text("Animation time: ${animationTime.toString().take(4)}s")
            }
            SetAsWallpaperButton(
                selectedPreset = selectedPreset,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
expect fun SetAsWallpaperButton(
    selectedPreset: LiveWallpaperPreset,
    modifier: Modifier = Modifier,
)

@Composable
expect fun SetSkyAsWallpaperButton(
    config: SkyWallpaperConfig,
    modifier: Modifier = Modifier,
)

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

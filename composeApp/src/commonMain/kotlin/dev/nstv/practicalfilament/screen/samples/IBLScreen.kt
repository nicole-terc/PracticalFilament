// Migrated sample from https://github.com/google/filament/tree/main/android/samples/sample-image-based-lighting
package dev.nstv.practicalfilament.screen.samples

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout
import dev.nstv.practicalfilament.components.materials.redballMaterial
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Color
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.theme.Grid
import practicalfilament.composeapp.generated.resources.Res

private const val IblIndirectLightPath = "files/envs/pillars_2k/pillars_2k_ibl.ktx"
private const val IblSkyboxPath = "files/envs/pillars_2k/pillars_2k_skybox.ktx"

private val IblLights = listOf(
    LightConfig(
        type = LightType.SUN,
        intensity = 95_000f,
        color = Color(1f, 0.97f, 0.94f),
        direction = Float3(0.5f, -1f, -0.8f),
        castShadows = true,
    ),
)

@Composable
fun IBLScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var sphereHandle by remember { mutableIntStateOf(0) }
    var indirectLightHandle by remember { mutableIntStateOf(0) }
    var rotationSpeed by remember { mutableFloatStateOf(18f) }
    var lightIntensity by remember { mutableFloatStateOf(55_000f) }

    LaunchedEffect(engine, sphereHandle, rotationSpeed) {
        if (sphereHandle == 0) return@LaunchedEffect
        while (true) {
            val time = withFrameNanos { it } / 1_000_000_000f
            engine?.setRenderableRotation(
                handle = sphereHandle,
                rotationXDegrees = 12f,
                rotationYDegrees = time * rotationSpeed,
            )
        }
    }

    SampleScreenLayout(
        modifier = modifier,
        title = "IBL",
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = CameraConfig(
                    position = Float3(0f, 0.25f, 4f),
                    lookAt = Float3(0f, 0f, 0f),
                    fovDegrees = 28.0,
                ),
                lights = IblLights,
                backgroundColor = Color(0.02f, 0.03f, 0.05f, 1f),
                onEngineReady = { readyEngine ->
                    val loaded = readyEngine.loadMaterial(redballMaterial())
                    loaded.parameters.values.forEach {
                        readyEngine.setMaterialParameter(loaded.instanceHandle, it)
                    }
                    indirectLightHandle =
                        readyEngine.loadIndirectLight(Res.getUri(IblIndirectLightPath))
                    readyEngine.setIndirectLight(
                        indirectLightHandle,
                        intensity = lightIntensity,
                    )
                    readyEngine.setSkybox(readyEngine.loadSkybox(Res.getUri(IblSkyboxPath)))
                    engine = readyEngine
                    sphereHandle =
                        readyEngine.createSphereRenderable(loaded.instanceHandle, radius = 1f)
                },
            )
        },
        controls = {
            Text(
                text = "Rotation Speed",
                modifier = Modifier.padding(top = Grid.One),
            )
            Slider(
                value = rotationSpeed,
                valueRange = 0f..60f,
                onValueChange = { rotationSpeed = it },
            )
            Text(
                text = "IBL Intensity",
                modifier = Modifier.padding(top = Grid.One),
            )
            Slider(
                value = lightIntensity,
                valueRange = 10_000f..80_000f,
                onValueChange = {
                    lightIntensity = it
                    if (indirectLightHandle > 0) {
                        engine?.setIndirectLight(indirectLightHandle, it)
                    }
                },
            )
        },
    )
}

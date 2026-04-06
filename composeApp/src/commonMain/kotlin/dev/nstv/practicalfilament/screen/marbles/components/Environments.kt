package dev.nstv.practicalfilament.screen.marbles.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows
import practicalfilament.composeapp.generated.resources.Res


internal data class EnvironmentOption(
    val label: String,
    val iblPath: String? = null,
    val skyboxPath: String? = null,
)

internal data class LoadedEnvironment(
    val indirectLightHandle: Int,
    val skyboxHandle: Int,
)

internal val MarbleTextureBackgrounds = listOf(
    EnvironmentOption(label = "None"),
    environmentOption("flower_road_2k"),
    environmentOption("flower_road_no_sun_2k"),
    environmentOption("graffiti_shelter_2k"),
    environmentOption("lightroom_14b"),
    environmentOption("noon_grass_2k"),
    environmentOption("parking_garage_2k"),
    environmentOption("pillars_2k"),
    environmentOption("studio_small_02_2k"),
    environmentOption("syferfontein_18d_clear_2k"),
    environmentOption("the_sky_is_on_fire_2k"),
    environmentOption("venetian_crossroads_2k"),
)

internal fun environmentOption(name: String): EnvironmentOption {
    return EnvironmentOption(
        label = name,
        iblPath = "files/envs/$name/${name}_ibl.ktx",
        skyboxPath = "files/envs/$name/${name}_skybox.ktx",
    )
}

private const val MarbleTextureEnvironmentIntensity = 50_000f


@Composable
internal fun EnvironmentSelectionField(
    filamentEngine: FilamentEngine?,
    modifier: Modifier = Modifier,
    backgrounds: List<EnvironmentOption> = MarbleTextureBackgrounds,
    selectedBackground: Int = 0,
    updateNotice: (String?) -> Unit = {},
){
    var selectedBackgroundIndex by remember { mutableIntStateOf(selectedBackground) }

    ObserveBackgroundIndex(
        filamentEngine = filamentEngine,
        selectedBackgroundIndex = selectedBackgroundIndex,
        updateNotice = updateNotice,
    )
    DropDownWithArrows(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Grid.One),
        options = backgrounds.map { it.label },
        selectedIndex = selectedBackgroundIndex,
        label = "Background",
        onSelectionChanged = { selectedBackgroundIndex = it },
    )
}

@Composable
private fun ObserveBackgroundIndex(
    filamentEngine: FilamentEngine?,
    selectedBackgroundIndex: Int,
    updateNotice: (String?) -> Unit,
) {
    var environmentHandles by remember { mutableStateOf<Map<String, LoadedEnvironment>>(emptyMap()) }
    var colorSkyboxHandle by remember { mutableIntStateOf(0) }
    var activeIndirectLightHandle by remember { mutableIntStateOf(0) }

    LaunchedEffect(filamentEngine, selectedBackgroundIndex) {
        val engine = filamentEngine ?: return@LaunchedEffect

        var currentColorSkyboxHandle = colorSkyboxHandle
        if (currentColorSkyboxHandle == 0) {
            currentColorSkyboxHandle = engine.createColorSkybox()
            if (currentColorSkyboxHandle > 0) {
                engine.setSkyboxColor(
                    currentColorSkyboxHandle,
                    r = 0.16f,
                    g = 0.18f,
                    b = 0.27f,
                    a = 1f,
                )
                colorSkyboxHandle = currentColorSkyboxHandle
            }
        }

        val background = MarbleTextureBackgrounds[selectedBackgroundIndex]
        if (background.iblPath == null || background.skyboxPath == null) {
            if (activeIndirectLightHandle > 0) {
                engine.setIndirectLight(activeIndirectLightHandle, 0f)
            }
            if (currentColorSkyboxHandle > 0) {
                engine.setSkybox(currentColorSkyboxHandle)
            }
            updateNotice(null)
            engine.requestFrame()
            return@LaunchedEffect
        }

        var updatedEnvironmentHandles = environmentHandles
        val loadedEnvironment = updatedEnvironmentHandles[background.label] ?: run {
            val indirectLightHandle = engine.loadIndirectLight(Res.getUri(background.iblPath))
            val skyboxHandle = engine.loadSkybox(Res.getUri(background.skyboxPath))
            if (indirectLightHandle <= 0 || skyboxHandle <= 0) {
                updateNotice("The ${background.label} background could not be loaded.")
                return@LaunchedEffect
            }
            LoadedEnvironment(
                indirectLightHandle = indirectLightHandle,
                skyboxHandle = skyboxHandle,
            ).also { loaded ->
                updatedEnvironmentHandles = updatedEnvironmentHandles + (background.label to loaded)
            }
        }

        environmentHandles = updatedEnvironmentHandles
        if (activeIndirectLightHandle > 0 &&
            activeIndirectLightHandle != loadedEnvironment.indirectLightHandle
        ) {
            engine.setIndirectLight(activeIndirectLightHandle, 0f)
        }
        engine.setIndirectLight(
            loadedEnvironment.indirectLightHandle,
            intensity = MarbleTextureEnvironmentIntensity,
        )
        engine.setSkybox(loadedEnvironment.skyboxHandle)
        activeIndirectLightHandle = loadedEnvironment.indirectLightHandle
        updateNotice(null)
        engine.requestFrame()
    }
}

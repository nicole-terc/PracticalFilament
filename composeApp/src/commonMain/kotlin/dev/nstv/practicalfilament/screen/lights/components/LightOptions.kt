package dev.nstv.practicalfilament.screen.lights.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.FilamentColor
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.LightType
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows

internal data class LightOption(
    val label: String,
    val lights: List<LightConfig>,
)

internal val LightPresets = listOf(
    LightOption(label = "None", lights = emptyList()),
    LightOption(
        label = "Directional – Front",
        lights = listOf(
            LightConfig(
                type = LightType.DIRECTIONAL,
                color = FilamentColor(1f, 0.98f, 0.95f),
                intensity = 75_000f,
                direction = Float3(-0.24f, -0.38f, -1f),
                castShadows = true,
            ),
        ),
    ),
    LightOption(
        label = "Directional – Top",
        lights = listOf(
            LightConfig(
                type = LightType.DIRECTIONAL,
                color = FilamentColor(1f, 0.98f, 0.95f),
                intensity = 75_000f,
                direction = Float3(0f, -1f, 0f),
                castShadows = true,
            ),
        ),
    ),
    LightOption(
        label = "Sun",
        lights = listOf(
            LightConfig(
                type = LightType.SUN,
                color = FilamentColor(1f, 0.95f, 0.89f),
                intensity = 110_000f,
                direction = Float3(0.45f, -1f, -0.72f),
                sunAngularRadius = 1.7f,
                sunHaloSize = 12f,
                sunHaloFalloff = 90f,
                castShadows = true,
            ),
        ),
    ),
    LightOption(
        label = "Sun – Low Angle",
        lights = listOf(
            LightConfig(
                type = LightType.SUN,
                color = FilamentColor(1f, 0.8f, 0.5f),
                intensity = 90_000f,
                direction = Float3(0.9f, -0.3f, -0.3f),
                sunAngularRadius = 2.5f,
                sunHaloSize = 15f,
                sunHaloFalloff = 60f,
                castShadows = true,
            ),
        ),
    ),
    LightOption(
        label = "Point – Above",
        lights = listOf(
            LightConfig(
                type = LightType.POINT,
                color = FilamentColor(1f, 1f, 1f),
                intensity = 1_000_000f,
                position = Float3(0f, 3f, 0f),
                falloffRadius = 10f,
            ),
        ),
    ),
    LightOption(
        label = "Point – Front",
        lights = listOf(
            LightConfig(
                type = LightType.POINT,
                color = FilamentColor(1f, 1f, 1f),
                intensity = 1_000_000f,
                position = Float3(0f, 0f, 4f),
                falloffRadius = 10f,
            ),
        ),
    ),
    LightOption(
        label = "Spot – Tight",
        lights = listOf(
            LightConfig(
                type = LightType.SPOT,
                color = FilamentColor(1f, 1f, 1f),
                intensity = 2_000_000f,
                position = Float3(0f, 4f, 0f),
                direction = Float3(0f, -1f, 0f),
                falloffRadius = 10f,
                innerConeAngle = 0.1f,
                outerConeAngle = 0.25f,
                castShadows = true,
            ),
        ),
    ),
    LightOption(
        label = "Spot – Wide",
        lights = listOf(
            LightConfig(
                type = LightType.SPOT,
                color = FilamentColor(1f, 1f, 1f),
                intensity = 2_000_000f,
                position = Float3(0f, 4f, 0f),
                direction = Float3(0f, -1f, 0f),
                falloffRadius = 10f,
                innerConeAngle = 0.5f,
                outerConeAngle = 0.8f,
                castShadows = true,
            ),
        ),
    ),
    LightOption(
        label = "Spot – Angled",
        lights = listOf(
            LightConfig(
                type = LightType.SPOT,
                color = FilamentColor(1f, 1f, 1f),
                intensity = 2_000_000f,
                position = Float3(-3f, 4f, 2f),
                direction = Float3(0.6f, -0.8f, -0.3f),
                falloffRadius = 12f,
                innerConeAngle = 0.2f,
                outerConeAngle = 0.4f,
                castShadows = true,
            ),
        ),
    ),
    LightOption(
        label = "Warm + Cool",
        lights = listOf(
            LightConfig(
                type = LightType.POINT,
                color = FilamentColor(1f, 0.6f, 0.2f),
                intensity = 800_000f,
                position = Float3(-2f, 1f, 2f),
                falloffRadius = 8f,
            ),
            LightConfig(
                type = LightType.POINT,
                color = FilamentColor(0.2f, 0.5f, 1f),
                intensity = 800_000f,
                position = Float3(2f, -1f, 2f),
                falloffRadius = 8f,
            ),
        ),
    ),
    LightOption(
        label = "Three-Point",
        lights = listOf(
            LightConfig(
                type = LightType.DIRECTIONAL,
                color = FilamentColor(1f, 0.98f, 0.95f),
                intensity = 75_000f,
                direction = Float3(-0.5f, -0.5f, -1f),
            ),
            LightConfig(
                type = LightType.POINT,
                color = FilamentColor(0.6f, 0.7f, 1f),
                intensity = 400_000f,
                position = Float3(2.5f, 1f, 1f),
                falloffRadius = 10f,
            ),
            LightConfig(
                type = LightType.POINT,
                color = FilamentColor(1f, 0.9f, 0.8f),
                intensity = 200_000f,
                position = Float3(0f, -1f, -2f),
                falloffRadius = 10f,
            ),
        ),
    ),
)

@Composable
internal fun LightSelectionField(
    filamentEngine: FilamentEngine?,
    modifier: Modifier = Modifier,
    presets: List<LightOption> = LightPresets,
    initialSelectedIndex: Int = 1,
    enabled: Boolean = true,
) {
    var selectedPresetIndex by remember { mutableIntStateOf(initialSelectedIndex) }
    ObserveLightPreset(
        filamentEngine = filamentEngine,
        selectedPresetIndex = selectedPresetIndex,
        presets = presets,
        enabled = enabled,
    )
    DropDownWithArrows(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Grid.One),
        options = presets.map { it.label },
        selectedIndex = selectedPresetIndex,
        label = "Lights",
        onSelectionChanged = { selectedPresetIndex = it },
    )
}

@Composable
private fun ObserveLightPreset(
    filamentEngine: FilamentEngine?,
    selectedPresetIndex: Int,
    presets: List<LightOption>,
    enabled: Boolean,
) {
    LaunchedEffect(filamentEngine, selectedPresetIndex, enabled) {
        val engine = filamentEngine ?: return@LaunchedEffect
        engine.clearLights()
        if (!enabled) {
            engine.requestFrame()
            return@LaunchedEffect
        }
        val preset = presets.getOrNull(selectedPresetIndex) ?: return@LaunchedEffect
        preset.lights.forEach { engine.addLight(it) }
        engine.requestFrame()
    }
}

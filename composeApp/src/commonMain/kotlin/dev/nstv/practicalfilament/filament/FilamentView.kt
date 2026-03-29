package dev.nstv.practicalfilament.filament

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun FilamentView(
    modifier: Modifier = Modifier,
    camera: CameraConfig = CameraConfig(),
    lights: List<LightConfig> = listOf(LightConfig(type = LightType.DIRECTIONAL)),
    backgroundColor: Color = Color(0f, 0f, 0f, 1f),
    onEngineReady: (FilamentEngine) -> Unit = {},
)

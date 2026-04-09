package dev.nstv.practicalfilament.filament

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

enum class FilamentHostViewMode {
    Auto,
    Surface,
    Texture,
}

val DefaultSkyboxColor = FilamentColor(
    r = 0.16f,
    g = 0.18f,
    b = 0.27f,
    a = 1f,
)

@Composable
expect fun FilamentView(
    modifier: Modifier = Modifier,
    camera: CameraConfig = CameraConfig(),
    lights: List<LightConfig> = listOf(LightConfig(type = LightType.DIRECTIONAL)),
    backgroundColor: FilamentColor = DefaultSkyboxColor,//MaterialTheme.colorScheme.background.toFilamentColor(),
    clipShape: FilamentClipShape? = null,
    isOpaque: Boolean = true,
    hostViewMode: FilamentHostViewMode = FilamentHostViewMode.Auto,
    onEngineReady: (FilamentEngine) -> Unit = {},
)

sealed interface FilamentClipShape {
    data object Circle : FilamentClipShape
    data class RoundedRect(val cornerRadius: Dp) : FilamentClipShape
}

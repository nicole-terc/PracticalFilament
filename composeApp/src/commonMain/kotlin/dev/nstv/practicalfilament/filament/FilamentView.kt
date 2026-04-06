package dev.nstv.practicalfilament.filament

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

enum class FilamentHostViewMode {
    Auto,
    Surface,
    Texture,
}

@Composable
expect fun FilamentView(
    modifier: Modifier = Modifier,
    camera: CameraConfig = CameraConfig(),
    lights: List<LightConfig> = listOf(LightConfig(type = LightType.DIRECTIONAL)),
    backgroundColor: FilamentColor = FilamentColor(0f, 0f, 0f, 1f),
    clipShape: FilamentClipShape? = null,
    isOpaque: Boolean = true,
    hostViewMode: FilamentHostViewMode = FilamentHostViewMode.Auto,
    onEngineReady: (FilamentEngine) -> Unit = {},
)

sealed interface FilamentClipShape {
    data object Circle : FilamentClipShape
    data class RoundedRect(val cornerRadius: Dp) : FilamentClipShape
}

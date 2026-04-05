package dev.nstv.practicalfilament.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun SetAsWallpaperButton(
    selectedPreset: LiveWallpaperPreset,
    modifier: Modifier,
) {
    Text(
        text = "Set as Wallpaper is not supported on iOS",
        modifier = modifier,
    )
}

@Composable
actual fun SetSkyAsWallpaperButton(
    config: SkyWallpaperConfig,
    modifier: Modifier,
) {
    Text(
        text = "Set as Wallpaper is not supported on iOS",
        modifier = modifier,
    )
}

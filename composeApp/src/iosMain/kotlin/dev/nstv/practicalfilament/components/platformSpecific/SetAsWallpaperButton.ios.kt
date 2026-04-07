package dev.nstv.practicalfilament.components.platformSpecific

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.screen.scenes.sky.SkyWallpaperConfig
import dev.nstv.practicalfilament.screen.wallpaper.LiveWallpaperPreset

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

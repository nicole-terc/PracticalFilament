package dev.nstv.practicalfilament.components.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.screen.sky.SkyWallpaperConfig
import dev.nstv.practicalfilament.screen.wallpaper.LiveWallpaperPreset

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

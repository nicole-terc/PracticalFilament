package dev.nstv.practicalfilament.components.platformSpecific

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.nstv.practicalfilament.screen.scenes.sky.SkyWallpaperConfig
import dev.nstv.practicalfilament.screen.wallpaper.LiveWallpaperPreferences
import dev.nstv.practicalfilament.screen.wallpaper.LiveWallpaperPreset

private const val LiveWallpaperServiceClassName =
    "dev.nstv.practicalfilament.FilamentLiveWallpaperService"

@Composable
actual fun SetAsWallpaperButton(
    selectedPreset: LiveWallpaperPreset,
    modifier: Modifier,
) {
    val context = LocalContext.current

    Button(
        modifier = modifier,
        onClick = {
            LiveWallpaperPreferences.saveSelectedPreset(context, selectedPreset)
            launchWallpaperPicker(context)
        },
    ) {
        Text("Set as Wallpaper")
    }
}

@Composable
actual fun SetSkyAsWallpaperButton(
    config: SkyWallpaperConfig,
    modifier: Modifier,
) {
    val context = LocalContext.current

    Button(
        modifier = modifier,
        onClick = {
            LiveWallpaperPreferences.saveSelectedPreset(context, LiveWallpaperPreset.CONFIGURED_SKY)
            LiveWallpaperPreferences.saveSkyConfig(context, config)
            launchWallpaperPicker(context)
        },
    ) {
        Text("Set as Wallpaper")
    }
}

fun launchWallpaperPicker(context: Context) {
    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
        putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context.packageName, LiveWallpaperServiceClassName),
        )
    }
    context.startActivity(intent)
}

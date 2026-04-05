package dev.nstv.practicalfilament.screen

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

private const val LiveWallpaperServiceClassName = "dev.nstv.practicalfilament.FilamentLiveWallpaperService"

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
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context.packageName, LiveWallpaperServiceClassName),
                )
            }
            context.startActivity(intent)
        },
    ) {
        Text("Set as Wallpaper")
    }
}

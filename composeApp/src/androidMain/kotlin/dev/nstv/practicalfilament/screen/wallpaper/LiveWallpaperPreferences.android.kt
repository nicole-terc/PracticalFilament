package dev.nstv.practicalfilament.screen.wallpaper

import android.content.Context
import dev.nstv.practicalfilament.screen.scenes.sky.SkyWallpaperConfig

object LiveWallpaperPreferences {
    private const val PreferencesName = "live_wallpaper_preferences"
    private const val SelectedPresetKey = "selected_preset"
    private const val SkyConfigKey = "sky_config"

    fun loadSelectedPreset(context: Context): LiveWallpaperPreset {
        val prefs = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        return LiveWallpaperPreset.fromStorageKey(prefs.getString(SelectedPresetKey, null))
    }

    fun saveSelectedPreset(context: Context, preset: LiveWallpaperPreset) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(SelectedPresetKey, preset.storageKey)
            .apply()
    }

    fun loadSkyConfig(context: Context): SkyWallpaperConfig {
        val prefs = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        return SkyWallpaperConfig.deserialize(prefs.getString(SkyConfigKey, null))
    }

    fun saveSkyConfig(context: Context, config: SkyWallpaperConfig) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(SkyConfigKey, config.serialize())
            .apply()
    }
}

package dev.nstv.practicalfilament.screen

import android.content.Context

object LiveWallpaperPreferences {
    private const val PreferencesName = "live_wallpaper_preferences"
    private const val SelectedPresetKey = "selected_preset"

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
}

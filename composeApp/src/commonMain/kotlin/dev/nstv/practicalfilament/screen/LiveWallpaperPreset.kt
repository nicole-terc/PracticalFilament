package dev.nstv.practicalfilament.screen

import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.Float3

private const val LiveWallpaperDefaultCycleSeconds = 10f

enum class LiveWallpaperPreset(
    val storageKey: String,
    val label: String,
    val assetPath: String? = null,
    private val orbitRadius: Float = 4f,
    private val orbitHeight: Float = 0.35f,
    private val fieldOfViewDegrees: Double = 30.0,
) {
    RAINBOW(
        storageKey = "rainbow",
        label = "Rainbow Sky",
        fieldOfViewDegrees = 45.0,
    ),
    CONFIGURED_SKY(
        storageKey = "configured_sky",
        label = "Configured Sky",
        fieldOfViewDegrees = 45.0,
    ),
    HELMET(
        storageKey = "helmet",
        label = "Damaged Helmet",
        assetPath = "files/models/helmet.glb",
        orbitRadius = 4.2f,
        orbitHeight = 0.25f,
        fieldOfViewDegrees = 28.0,
    ),
    DRONE(
        storageKey = "drone",
        label = "Buster Drone",
        assetPath = "files/models/BusterDrone/scene.gltf",
        orbitRadius = 4.8f,
        orbitHeight = 0.7f,
        fieldOfViewDegrees = 30.0,
    ),
    ;

    val usesModel: Boolean
        get() = assetPath != null

    fun cameraAt(seconds: Float): CameraConfig {
        @Suppress("UNUSED_PARAMETER")
        val ignored = seconds
        if (!usesModel) {
            return CameraConfig(
                position = Float3(0f, 0f, 4f),
                lookAt = Float3(0f, 0f, 0f),
                up = Float3(0f, 1f, 0f),
                fovDegrees = fieldOfViewDegrees,
            )
        }

        return CameraConfig(
            position = Float3(
                x = 0f,
                y = orbitHeight,
                z = orbitRadius,
            ),
            lookAt = Float3(0f, 0f, 0f),
            up = Float3(0f, 1f, 0f),
            fovDegrees = fieldOfViewDegrees,
        )
    }

    companion object {
        val default: LiveWallpaperPreset = RAINBOW

        fun fromStorageKey(value: String?): LiveWallpaperPreset {
            return entries.firstOrNull { it.storageKey == value } ?: default
        }
    }
}

fun liveWallpaperHueAt(seconds: Float): Float {
    val normalizedSeconds = ((seconds % LiveWallpaperDefaultCycleSeconds) + LiveWallpaperDefaultCycleSeconds) %
        LiveWallpaperDefaultCycleSeconds
    return normalizedSeconds / LiveWallpaperDefaultCycleSeconds * 360f
}

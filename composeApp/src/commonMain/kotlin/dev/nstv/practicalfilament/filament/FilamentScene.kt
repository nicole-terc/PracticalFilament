package dev.nstv.practicalfilament.filament

enum class LightType { DIRECTIONAL, POINT, SPOT }

data class LightConfig(
    val type: LightType,
    val color: Color = Color(1f, 1f, 1f),
    val intensity: Float = 100_000f,
    val position: Float3 = Float3(0f, 0f, 0f),
    val falloffRadius: Float = 1f,
    val direction: Float3 = Float3(0f, -1f, 0f),
    val innerConeAngle: Float = 0.5f,
    val outerConeAngle: Float = 0.7f,
)

data class CameraConfig(
    val position: Float3 = Float3(0f, 0f, 5f),
    val lookAt: Float3 = Float3(0f, 0f, 0f),
    val up: Float3 = Float3(0f, 1f, 0f),
    val fovDegrees: Double = 45.0,
    val near: Double = 0.1,
    val far: Double = 100.0,
)

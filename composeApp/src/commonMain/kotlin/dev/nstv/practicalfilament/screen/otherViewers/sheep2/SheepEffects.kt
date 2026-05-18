package dev.nstv.practicalfilament.screen.otherViewers.sheep2

import dev.nstv.practicalfilament.filament.Float3
import kotlin.math.cos
import kotlin.math.sin

internal const val SheepScatterDistance = 5.2f
internal const val SheepScatterUpBias = 0.55f

internal fun buildScatterAndShrinkTransform(
    explosionOffset: Float3,
    progress: Float,
    distanceScale: Float = 1f,
): FloatArray {
    val t = smoothStep(progress)
    if (t <= 1e-4f) {
        return identityMatrix4()
    }
    val shrink = (1f - t).coerceAtLeast(0f)
    return multiplyMatrix4(
        translationMatrix(
            x = explosionOffset.x * t * distanceScale,
            y = explosionOffset.y * t * distanceScale,
            z = explosionOffset.z * t * distanceScale,
        ),
        scaleMatrix(shrink, shrink, shrink),
    )
}

internal fun sheepScatterOffset(
    anchor: Float3,
    index: Int,
    distance: Float = SheepScatterDistance,
    upwardBias: Float = SheepScatterUpBias,
): Float3 {
    val angle = index * 2.3999631f
    val fallbackDirection = Float3(
        x = cos(angle),
        y = 0.4f + 0.2f * sin(angle * 1.7f),
        z = sin(angle),
    )
    val direction = Float3(
        x = anchor.x * 1.15f + fallbackDirection.x * 0.34f,
        y = anchor.y + upwardBias + fallbackDirection.y * 0.2f,
        z = anchor.z * 1.15f + fallbackDirection.z * 0.34f,
    ).normalized(default = Float3(0f, 1f, 0f))
    val distanceScale = 1.05f + 0.5f * (0.5f + 0.5f * sin(index * 1.37f))
    return Float3(
        x = direction.x * distance * distanceScale,
        y = direction.y * distance * distanceScale,
        z = direction.z * distance * distanceScale,
    )
}

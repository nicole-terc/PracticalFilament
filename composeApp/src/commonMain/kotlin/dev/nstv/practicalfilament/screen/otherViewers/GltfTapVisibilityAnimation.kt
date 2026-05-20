package dev.nstv.practicalfilament.screen.otherViewers

import dev.nstv.practicalfilament.screen.otherViewers.sheep2.multiplyMatrix4
import dev.nstv.practicalfilament.screen.otherViewers.sheep2.rotationYMatrix
import dev.nstv.practicalfilament.screen.otherViewers.sheep2.scaleMatrix
import dev.nstv.practicalfilament.screen.otherViewers.sheep2.translationMatrix

internal const val GltfTapVisibilityDurationSeconds = 0.32f
internal const val GltfTapVisibilitySpinDegrees = 540f
internal const val GltfTapVisibilityHiddenY = -1_000f

internal fun gltfTapVisibleTransform(): FloatArray =
    gltfTapLocalTransform(progress = 0f)

internal fun gltfTapHiddenTransform(): FloatArray =
    translationMatrix(0f, GltfTapVisibilityHiddenY, 0f)

internal fun gltfTapLocalTransform(
    progress: Float,
    baseRotationDegrees: Float = 0f,
    baseScale: Float = 1f,
): FloatArray {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val visibleScale = (1f - clampedProgress).coerceAtLeast(0f) * baseScale
    val spinDegrees = clampedProgress * GltfTapVisibilitySpinDegrees
    return multiplyMatrix4(
        rotationYMatrix(baseRotationDegrees + spinDegrees),
        scaleMatrix(visibleScale, visibleScale, visibleScale),
    )
}

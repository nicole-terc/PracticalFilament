package dev.nstv.practicalfilament.components.utils

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import dev.nstv.practicalfilament.filament.CameraConfig
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.Float3
import kotlin.math.abs
import kotlin.math.sqrt

data class OrbitQuaternion(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 1f,
) {
    operator fun times(other: OrbitQuaternion): OrbitQuaternion {
        return multiplyRaw(other).normalized()
    }

    fun conjugate(): OrbitQuaternion = OrbitQuaternion(-x, -y, -z, w)

    fun rotate(vector: Float3): Float3 {
        val quaternionVector = OrbitQuaternion(vector.x, vector.y, vector.z, 0f)
        val rotated = multiplyRaw(quaternionVector).multiplyRaw(conjugate())
        return Float3(rotated.x, rotated.y, rotated.z)
    }

    fun normalized(): OrbitQuaternion {
        val magnitude = sqrt(x * x + y * y + z * z + w * w)
        if (magnitude <= 1e-6f) return Identity
        return OrbitQuaternion(x / magnitude, y / magnitude, z / magnitude, w / magnitude)
    }

    private fun multiplyRaw(other: OrbitQuaternion): OrbitQuaternion {
        return OrbitQuaternion(
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w,
            w = w * other.w - x * other.x - y * other.y - z * other.z,
        )
    }

    companion object {
        val Identity = OrbitQuaternion(0f, 0f, 0f, 1f)
    }
}

class OrbitCameraState(
    initialOrientation: OrbitQuaternion = OrbitQuaternion.Identity,
    initialDistance: Float,
) {
    var orientation by mutableStateOf(initialOrientation)
    var distance by mutableFloatStateOf(initialDistance)
}

@Composable
fun rememberOrbitCameraState(
    initialOrientation: OrbitQuaternion = OrbitQuaternion.Identity,
    initialDistance: Float,
): OrbitCameraState = remember {
    OrbitCameraState(
        initialOrientation = initialOrientation,
        initialDistance = initialDistance,
    )
}

fun Modifier.orbitCameraControls(
    viewportSize: IntSize,
    orientation: OrbitQuaternion,
    onOrientationChange: (OrbitQuaternion) -> Unit,
    distance: Float,
    onDistanceChange: (Float) -> Unit,
    minDistance: Float = 2f,
    maxDistance: Float = 8f,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this

    val currentViewportSize by rememberUpdatedState(viewportSize)
    val currentOrientation by rememberUpdatedState(orientation)
    val currentDistance by rememberUpdatedState(distance)
    val currentOnOrientationChange by rememberUpdatedState(onOrientationChange)
    val currentOnDistanceChange by rememberUpdatedState(onDistanceChange)

    pointerInput(Unit) {
        detectTransformGestures(
            panZoomLock = true,
        ) { centroid, pan, zoom, _ ->
            val size = currentViewportSize
            val previousPoint = (centroid - pan).toArcballPoint(size)
            val currentPoint = centroid.toArcballPoint(size)
            if (previousPoint != null && currentPoint != null) {
                currentOnOrientationChange(arcballDelta(previousPoint, currentPoint) * currentOrientation)
            }
            if (zoom > 0f) {
                currentOnDistanceChange((currentDistance / zoom).coerceIn(minDistance, maxDistance))
            }
        }
    }
}

fun Modifier.orbitCameraControls(
    viewportSize: IntSize,
    cameraState: OrbitCameraState,
    baseCamera: CameraConfig,
    engine: FilamentEngine?,
    minDistance: Float = 2f,
    maxDistance: Float = 8f,
    enabled: Boolean = true,
): Modifier = orbitCameraControls(
    viewportSize = viewportSize,
    orientation = cameraState.orientation,
    onOrientationChange = { newOrientation ->
        cameraState.orientation = newOrientation
        engine?.updateCamera(
            orbitCameraConfig(
                baseCamera = baseCamera,
                orientation = newOrientation,
                distance = cameraState.distance,
            ),
        )
        engine?.requestFrame()
    },
    distance = cameraState.distance,
    onDistanceChange = { newDistance ->
        cameraState.distance = newDistance
        engine?.updateCamera(
            orbitCameraConfig(
                baseCamera = baseCamera,
                orientation = cameraState.orientation,
                distance = newDistance,
            ),
        )
        engine?.requestFrame()
    },
    minDistance = minDistance,
    maxDistance = maxDistance,
    enabled = enabled,
)

fun CameraConfig.orbitDistance(): Float = (position - lookAt).length()

fun orbitCameraConfig(
    baseCamera: CameraConfig,
    orientation: OrbitQuaternion,
    distance: Float = baseCamera.orbitDistance(),
): CameraConfig {
    val orbit = orientation.conjugate()
    val eyeDirection = (baseCamera.position - baseCamera.lookAt).normalized()
    val upDirection = baseCamera.up.normalized()
    return baseCamera.copy(
        position = baseCamera.lookAt + orbit.rotate(eyeDirection * distance),
        up = orbit.rotate(upDirection),
    )
}

private fun arcballDelta(from: Float3, to: Float3): OrbitQuaternion {
    val dot = (from dot to).coerceIn(-1f, 1f)
    val cross = from cross to
    val magnitude = cross.length()
    if (magnitude <= 1e-6f) {
        return if (dot < 0f) {
            val fallbackAxis = if (abs(from.x) < 0.9f) {
                Float3(1f, 0f, 0f) cross from
            } else {
                Float3(0f, 1f, 0f) cross from
            }.normalized()
            OrbitQuaternion(fallbackAxis.x, fallbackAxis.y, fallbackAxis.z, 0f)
        } else {
            OrbitQuaternion.Identity
        }
    }
    return OrbitQuaternion(
        x = cross.x,
        y = cross.y,
        z = cross.z,
        w = 1f + dot,
    ).normalized()
}

private fun Offset.toArcballPoint(size: IntSize): Float3? {
    if (size.width <= 0 || size.height <= 0) return null
    val scale = minOf(size.width, size.height).toFloat()
    val x = (2f * this.x - size.width) / scale
    val y = (size.height - 2f * this.y) / scale
    val lengthSquared = x * x + y * y
    return if (lengthSquared <= 1f) {
        Float3(x, y, sqrt(1f - lengthSquared))
    } else {
        val inverseLength = 1f / sqrt(lengthSquared)
        Float3(x * inverseLength, y * inverseLength, 0f)
    }
}

private operator fun Float3.minus(other: Float3): Float3 = Float3(
    x = x - other.x,
    y = y - other.y,
    z = z - other.z,
)

private operator fun Float3.plus(other: Float3): Float3 = Float3(
    x = x + other.x,
    y = y + other.y,
    z = z + other.z,
)

private operator fun Float3.times(scale: Float): Float3 = Float3(
    x = x * scale,
    y = y * scale,
    z = z * scale,
)

private infix fun Float3.dot(other: Float3): Float = x * other.x + y * other.y + z * other.z

private infix fun Float3.cross(other: Float3): Float3 = Float3(
    x = y * other.z - z * other.y,
    y = z * other.x - x * other.z,
    z = x * other.y - y * other.x,
)

private fun Float3.length(): Float = sqrt(x * x + y * y + z * z)

private fun Float3.normalized(): Float3 {
    val length = length()
    if (length <= 1e-6f) return Float3(0f, 0f, 1f)
    return Float3(x / length, y / length, z / length)
}

package dev.nstv.practicalfilament.filament

import androidx.compose.runtime.withFrameNanos as composeWithFrameNanos

const val AnimationTargetFps = 60
private const val AnimationInitialDeltaSeconds = 1f / AnimationTargetFps
private const val AnimationMaxDeltaSeconds = 0.1f // 10 fps floor — prevents spiral-of-death on slow frames
private const val AnimationInitialDeltaNanos = (AnimationInitialDeltaSeconds * 1_000_000_000f).toLong()

/**
 * Core frame loop. Calls [composeWithFrameNanos] every frame and delivers the raw frame timestamp
 * alongside the previous frame's timestamp to the caller-provided accumulator.
 *
 * [previousFrameNanos] is 0L on the first frame so callers can detect the initial tick.
 */
private suspend inline fun frameLoop(crossinline block: (frameNanos: Long, previousFrameNanos: Long) -> Unit) {
    var previousFrameNanos = 0L
    while (true) {
        val frameNanos = composeWithFrameNanos { it }
        block(frameNanos, previousFrameNanos)
        previousFrameNanos = frameNanos
    }
}

/**
 * Runs [block] on every rendered frame with accumulated Float seconds.
 * - [elapsedSeconds]: time accumulated from per-frame deltas — safe to pass to `sin`/`cos` without
 *   Float precision loss. Using the raw timestamp directly (dividing by 1e9f) loses sub-second
 *   precision after ~1000 s of device uptime, causing visible animation stutter.
 * - [deltaSeconds]: wall-clock time since the previous frame, clamped to [AnimationMaxDeltaSeconds].
 */
suspend fun withFrameSeconds(block: (elapsedSeconds: Float, deltaSeconds: Float) -> Unit) {
    var elapsedSeconds = 0f
    frameLoop { frameNanos, previousFrameNanos ->
        val deltaSeconds = if (previousFrameNanos == 0L) {
            AnimationInitialDeltaSeconds
        } else {
            ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceAtMost(AnimationMaxDeltaSeconds)
        }
        elapsedSeconds += deltaSeconds
        block(elapsedSeconds, deltaSeconds)
    }
}

/**
 * Runs [block] on every rendered frame with nanosecond precision.
 * - [elapsedNanos]: time accumulated from per-frame deltas using Long arithmetic — no precision loss.
 * - [deltaNanos]: raw nanosecond delta since the previous frame
 *
 * Use when sub-millisecond accuracy matters
 */
suspend fun withFrameNanos(block: (elapsedNanos: Long, deltaNanos: Long) -> Unit) {
    var elapsedNanos = 0L
    frameLoop { frameNanos, previousFrameNanos ->
        val deltaNanos = if (previousFrameNanos == 0L) {
            AnimationInitialDeltaNanos
        } else {
            frameNanos - previousFrameNanos
        }
        elapsedNanos += deltaNanos
        block(elapsedNanos, deltaNanos)
    }
}

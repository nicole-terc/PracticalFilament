package dev.nstv.practicalfilament.screen.otherViewers.sheep2

import dev.nstv.practicalfilament.filament.Float3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

internal data class Sheep2AnimationControls(
    val animationEnabled: Boolean = true,
    val pulseAmount: Float = 0.65f,
    val noiseAmount: Float = 0.4f,
    val noiseFrequency: Float = 1f,
    val driftAmount: Float = 0.55f,
    val followThroughAmount: Float = 0.65f,
    val noiseSeed: Int = 1,
)

internal fun buildSheep2PieceTransform(
    piece: SheepRigPiece,
    timeSeconds: Float,
    controls: Sheep2AnimationControls,
): FloatArray {
    if (!controls.animationEnabled) {
        return piece.baseTransform.copyOf()
    }

    val rootTransform = buildSheep2RootTransform(timeSeconds, controls)
    if (controls.driftAmount <= 1e-6f &&
        controls.pulseAmount <= 1e-6f &&
        controls.noiseAmount <= 1e-6f
    ) {
        return multiplyMatrix4(rootTransform, piece.baseTransform)
    }

    val laggedTime = max(0f, timeSeconds - sheep2LagSeconds(piece, controls))
    val pulseEnvelope = sheep2PulseEnvelope(laggedTime)
    val signedPulse = ((pulseEnvelope - 0.5f) * 2f).coerceIn(-1f, 1f)
    val radialDirection = piece.anchor.normalized(Float3(0f, 1f, 0f))
    val radialPulse = signedPulse * controls.pulseAmount * piece.radialWeight
    val noise = sheep2NoiseVector(
        anchor = piece.anchor,
        timeSeconds = laggedTime,
        seed = controls.noiseSeed,
        frequency = controls.noiseFrequency,
    )
    val noiseScalar = sheep2NoiseScalar(
        anchor = piece.anchor,
        timeSeconds = laggedTime,
        seed = controls.noiseSeed,
        frequency = controls.noiseFrequency,
    )
    val roleOffset = roleWorldOffset(piece, radialDirection, radialPulse, noise, controls.noiseAmount)
    val localMotion = roleLocalMotion(piece, signedPulse, noise, noiseScalar, controls)
    return multiplyMatrix4(
        rootTransform,
        multiplyMatrix4(
            translationMatrix(roleOffset.x, roleOffset.y, roleOffset.z),
            multiplyMatrix4(piece.baseTransform, localMotion),
        ),
    )
}

internal fun buildSheep2RootTransform(
    timeSeconds: Float,
    controls: Sheep2AnimationControls,
): FloatArray {
    if (!controls.animationEnabled || controls.driftAmount <= 1e-6f) {
        return identityMatrix4()
    }
    val drift = controls.driftAmount
    val x = sin(timeSeconds * 0.52f) * 0.16f * drift
    val y = sin(timeSeconds * 1.14f) * 0.1f * drift + sin(timeSeconds * 0.27f + 0.8f) * 0.04f * drift
    val z = sin(timeSeconds * 0.73f + 0.5f) * 0.08f * drift
    val yaw = sin(timeSeconds * 0.56f + 0.3f) * 7f * drift
    val roll = sin(timeSeconds * 0.94f + 1.1f) * 5f * drift
    val pitch = sin(timeSeconds * 0.63f + 2f) * 4f * drift
    return multiplyMatrix4(
        translationMatrix(x, y, z),
        multiplyMatrix4(
            rotationYMatrix(yaw),
            multiplyMatrix4(
                rotationZMatrix(roll),
                rotationXMatrix(pitch),
            ),
        ),
    )
}

internal fun sheep2PulseEnvelope(timeSeconds: Float): Float {
    val wave = 0.5f + 0.5f * sin(timeSeconds * 2.05f - PI.toFloat() * 0.35f)
    return smoothStep(wave)
}

internal fun sheep2LagSeconds(
    piece: SheepRigPiece,
    controls: Sheep2AnimationControls,
): Float = (0.34f * piece.lagWeight * controls.followThroughAmount).coerceIn(0f, 0.34f)

internal fun sheep2NoiseVector(
    anchor: Float3,
    timeSeconds: Float,
    seed: Int,
    frequency: Float,
): Float3 {
    val freq = 0.45f + frequency.coerceAtLeast(0f) * 1.7f
    val t = timeSeconds * freq
    val px = phase(seed, anchor, 0.17f)
    val py = phase(seed, anchor, 2.31f)
    val pz = phase(seed, anchor, 4.49f)
    val x = layeredWave(anchor, t, px, 1.2f, 0.9f, 1.4f)
    val y = layeredWave(
        Float3(anchor.y, anchor.z, anchor.x),
        t * 1.1f,
        py,
        1.4f,
        1.05f,
        1.15f,
    )
    val z = layeredWave(
        Float3(anchor.z, anchor.x, anchor.y),
        t * 0.93f,
        pz,
        1.35f,
        1.18f,
        0.92f,
    )
    return Float3(
        x = x.coerceIn(-1f, 1f),
        y = y.coerceIn(-1f, 1f),
        z = z.coerceIn(-1f, 1f),
    )
}

internal fun sheep2NoiseScalar(
    anchor: Float3,
    timeSeconds: Float,
    seed: Int,
    frequency: Float,
): Float {
    val phase = phase(seed, anchor, 6.73f)
    val scalar = layeredWave(anchor, timeSeconds * (0.3f + frequency * 0.75f), phase, 0.95f, 0.72f, 1.08f)
    return scalar.coerceIn(-1f, 1f)
}

private fun roleWorldOffset(
    piece: SheepRigPiece,
    radialDirection: Float3,
    radialPulse: Float,
    noise: Float3,
    noiseAmount: Float,
): Float3 {
    val noiseScale = noiseAmount * when (piece.role) {
        SheepPieceRole.FLUFF_CORE -> 0.035f
        SheepPieceRole.FLUFF_SHELL -> 0.09f
        SheepPieceRole.HEAD -> 0.05f
        SheepPieceRole.EYE -> 0.022f
        SheepPieceRole.PUPIL -> 0.018f
        SheepPieceRole.LEG -> 0.055f
        SheepPieceRole.GLASSES -> 0.03f
    }
    val pulseOffset = when (piece.role) {
        SheepPieceRole.FLUFF_CORE -> Float3(0f, radialPulse * 0.04f, 0f)
        SheepPieceRole.FLUFF_SHELL -> radialDirection * (radialPulse * 0.18f)
        SheepPieceRole.HEAD -> Float3(
            x = radialDirection.x * radialPulse * 0.03f,
            y = radialPulse * 0.05f,
            z = radialDirection.z * radialPulse * 0.02f,
        )
        SheepPieceRole.EYE -> Float3(0f, radialPulse * 0.01f, 0f)
        SheepPieceRole.PUPIL -> Float3(noise.x * 0.004f, noise.y * 0.004f, 0f)
        SheepPieceRole.LEG -> Float3(0f, -abs(radialPulse) * 0.07f, 0f)
        SheepPieceRole.GLASSES -> Float3(0f, radialPulse * 0.015f, 0f)
    }
    val noiseOffset = Float3(
        x = noise.x * noiseScale,
        y = noise.y * noiseScale,
        z = noise.z * noiseScale * 0.8f,
    )
    return pulseOffset + noiseOffset
}

private fun roleLocalMotion(
    piece: SheepRigPiece,
    signedPulse: Float,
    noise: Float3,
    noiseScalar: Float,
    controls: Sheep2AnimationControls,
): FloatArray {
    val pulse = controls.pulseAmount
    val noiseAmount = controls.noiseAmount
    return when (piece.role) {
        SheepPieceRole.FLUFF_CORE -> {
            val scale = multiplyMatrix4(
                scaleMatrix(
                    x = 1f + pulse * signedPulse * 0.07f,
                    y = 1f - pulse * signedPulse * 0.12f,
                    z = 1f + pulse * signedPulse * 0.07f,
                ),
                scaleMatrix(
                    x = 1f + noiseScalar * noiseAmount * 0.02f,
                    y = 1f + noiseScalar * noiseAmount * 0.018f,
                    z = 1f + noiseScalar * noiseAmount * 0.02f,
                ),
            )
            scale
        }

        SheepPieceRole.FLUFF_SHELL -> multiplyMatrix4(
            rotationZMatrix(noise.x * noiseAmount * 5f),
            scaleMatrix(
                x = 1f + pulse * signedPulse * 0.08f + noiseScalar * noiseAmount * 0.03f,
                y = 1f - pulse * signedPulse * 0.11f,
                z = 1f + pulse * signedPulse * 0.08f + noiseScalar * noiseAmount * 0.03f,
            ),
        )

        SheepPieceRole.HEAD -> multiplyMatrix4(
            rotationZMatrix(signedPulse * pulse * 5f + noise.z * noiseAmount * 3f),
            multiplyMatrix4(
                rotationXMatrix(signedPulse * pulse * -4.5f),
                scaleMatrix(
                    x = 1f + pulse * signedPulse * 0.03f,
                    y = 1f - pulse * signedPulse * 0.06f,
                    z = 1f + pulse * signedPulse * 0.02f,
                ),
            ),
        )

        SheepPieceRole.EYE -> multiplyMatrix4(
            rotationZMatrix(noise.z * noiseAmount * 3f),
            scaleMatrix(
                x = 1f,
                y = 1f - pulse * max(0f, signedPulse) * 0.12f,
                z = 1f,
            ),
        )

        SheepPieceRole.PUPIL -> multiplyMatrix4(
            translationMatrix(noise.x * noiseAmount * 0.01f, noise.y * noiseAmount * 0.012f, 0f),
            scaleMatrix(
                x = 1f + noiseScalar * noiseAmount * 0.02f,
                y = 1f - pulse * max(0f, signedPulse) * 0.08f,
                z = 1f,
            ),
        )

        SheepPieceRole.LEG -> multiplyMatrix4(
            rotationXMatrix(signedPulse * pulse * -10f + noise.z * noiseAmount * 4f),
            scaleMatrix(
                x = 1f,
                y = 1f + max(0f, signedPulse) * pulse * 0.12f,
                z = 1f,
            ),
        )

        SheepPieceRole.GLASSES -> multiplyMatrix4(
            rotationZMatrix(signedPulse * pulse * 4f + noise.z * noiseAmount * 2f),
            rotationXMatrix(noise.y * noiseAmount * 2f),
        )
    }
}

private fun layeredWave(
    anchor: Float3,
    time: Float,
    phase: Float,
    s1: Float,
    s2: Float,
    s3: Float,
): Float {
    val a = sin(anchor.x * 1.17f * s1 + anchor.y * 1.63f + anchor.z * 0.87f + time * 1.21f + phase)
    val b = sin(anchor.x * 2.41f + anchor.y * 0.74f * s2 - anchor.z * 1.35f + time * 1.77f + phase * 1.3f)
    val c = sin(-anchor.x * 1.73f + anchor.y * 2.12f + anchor.z * 1.43f * s3 + time * 0.93f + phase * 0.77f)
    return (a + b * 0.6f + c * 0.4f) / 2f
}

private fun phase(seed: Int, anchor: Float3, salt: Float): Float {
    val raw = sin(
        seed * 12.9898f +
            anchor.x * 78.233f +
            anchor.y * 37.719f +
            anchor.z * 19.913f +
            salt * 43.123f,
    ) * 43_758.547f
    return fract(raw) * PI.toFloat() * 2f
}

private fun fract(value: Float): Float = value - floor(value)

internal fun smoothStep(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

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

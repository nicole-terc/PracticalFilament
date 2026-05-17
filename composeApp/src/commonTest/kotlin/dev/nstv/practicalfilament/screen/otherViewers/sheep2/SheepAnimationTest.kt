package dev.nstv.practicalfilament.screen.otherViewers.sheep2

import dev.nstv.practicalfilament.filament.Float3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SheepAnimationTest {
    private val samplePiece = SheepRigPiece(
        handle = 1,
        role = SheepPieceRole.FLUFF_SHELL,
        baseTransform = translationMatrix(0.2f, 0.4f, -0.1f),
        anchor = Float3(0.2f, 0.4f, -0.1f),
        phaseOffset = 0.8f,
        radialWeight = 0.7f,
        lagWeight = 0.6f,
    )

    @Test
    fun `noise field is deterministic`() {
        val first = sheep2NoiseVector(
            anchor = Float3(0.25f, -0.2f, 0.5f),
            timeSeconds = 1.4f,
            seed = 11,
            frequency = 1.2f,
        )
        val second = sheep2NoiseVector(
            anchor = Float3(0.25f, -0.2f, 0.5f),
            timeSeconds = 1.4f,
            seed = 11,
            frequency = 1.2f,
        )

        assertEquals(first, second)
        assertEquals(
            sheep2NoiseScalar(Float3(0.25f, -0.2f, 0.5f), 1.4f, 11, 1.2f),
            sheep2NoiseScalar(Float3(0.25f, -0.2f, 0.5f), 1.4f, 11, 1.2f),
        )
    }

    @Test
    fun `zero amplitudes return base transform`() {
        val transform = buildSheep2PieceTransform(
            piece = samplePiece,
            timeSeconds = 2.5f,
            controls = Sheep2AnimationControls(
                animationEnabled = true,
                pulseAmount = 0f,
                noiseAmount = 0f,
                driftAmount = 0f,
                followThroughAmount = 0f,
                noiseFrequency = 1f,
                noiseSeed = 3,
            ),
        )

        assertContentEquals(samplePiece.baseTransform, transform)
    }

    @Test
    fun `pulse envelope stays bounded`() {
        repeat(200) { index ->
            val value = sheep2PulseEnvelope(index / 20f)
            assertTrue(value in 0f..1f)
        }
    }

    @Test
    fun `generated transforms remain finite`() {
        val transform = buildSheep2PieceTransform(
            piece = samplePiece.copy(role = SheepPieceRole.LEG, lagWeight = 1f, radialWeight = 1f),
            timeSeconds = 3.4f,
            controls = Sheep2AnimationControls(
                animationEnabled = true,
                pulseAmount = 1f,
                noiseAmount = 1f,
                driftAmount = 1f,
                followThroughAmount = 1f,
                noiseFrequency = 2f,
                noiseSeed = 19,
            ),
        )

        assertTrue(transform.all { it.isFinite() })
    }

    @Test
    fun `lag stays bounded`() {
        val controls = Sheep2AnimationControls(
            animationEnabled = true,
            followThroughAmount = 1f,
        )

        val lag = sheep2LagSeconds(samplePiece.copy(lagWeight = 1f), controls)

        assertTrue(lag in 0f..0.34f)
    }

    @Test
    fun `follow through motion stays within a sane distance`() {
        val transform = buildSheep2PieceTransform(
            piece = samplePiece.copy(role = SheepPieceRole.GLASSES, lagWeight = 1f),
            timeSeconds = 2.1f,
            controls = Sheep2AnimationControls(
                animationEnabled = true,
                pulseAmount = 1f,
                noiseAmount = 1f,
                driftAmount = 1f,
                followThroughAmount = 1f,
                noiseFrequency = 2f,
                noiseSeed = 5,
            ),
        )
        val translation = extractTranslation(transform)

        assertTrue(abs(translation.x) < 1.5f)
        assertTrue(abs(translation.y) < 1.5f)
        assertTrue(abs(translation.z) < 1.5f)
    }
}

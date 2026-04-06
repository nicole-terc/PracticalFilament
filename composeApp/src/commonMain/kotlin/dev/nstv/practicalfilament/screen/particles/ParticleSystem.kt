package dev.nstv.practicalfilament.screen.particles

import dev.nstv.practicalfilament.filament.Float2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var px: Float,
    var py: Float,
    var pz: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var tx: Float = 0f,
    var ty: Float = 0f,
    var tz: Float = 0f,
    val phaseOffset: Float,
)

class ParticleSystem(
    val count: Int,
    seed: Int = 1,
) {
    private val random = Random(seed)

    val particles: Array<Particle> = Array(count) {
        val scatter = randomScatterPoint()
        Particle(
            px = scatter.x,
            py = scatter.y,
            pz = scatter.z,
            tx = scatter.x,
            ty = scatter.y,
            tz = scatter.z,
            phaseOffset = random.nextFloat() * (PI.toFloat() * 2f),
        )
    }

    val fixedIndices: ShortArray = ShortArray(count * 6).apply {
        var vertexIndex = 0
        var indexOffset = 0
        repeat(count) {
            this[indexOffset++] = vertexIndex.toShort()
            this[indexOffset++] = (vertexIndex + 2).toShort()
            this[indexOffset++] = (vertexIndex + 1).toShort()
            this[indexOffset++] = (vertexIndex + 1).toShort()
            this[indexOffset++] = (vertexIndex + 2).toShort()
            this[indexOffset++] = (vertexIndex + 3).toShort()
            vertexIndex += 4
        }
    }

    fun setTargets(positions: List<Float2>) {
        if (positions.isEmpty()) {
            setScatterTargets()
            return
        }

        particles.forEachIndexed { index, particle ->
            val sourceIndex = if (positions.size >= count) {
                (index * positions.size / count).coerceIn(0, positions.lastIndex)
            } else {
                index % positions.size
            }
            val base = positions[sourceIndex]
            val jitterScale = if (positions.size < count) 0.012f else 0.0025f
            val jitterAngle = particle.phaseOffset + index * 0.6180339f
            particle.tx = base.x + cos(jitterAngle) * jitterScale
            particle.ty = base.y + sin(jitterAngle) * jitterScale
            particle.tz = 0f
        }
    }

    fun setScatterTargets() {
        particles.forEach { particle ->
            val scatter = randomScatterPoint()
            particle.tx = scatter.x
            particle.ty = scatter.y
            particle.tz = scatter.z
        }
    }

    fun snapToTargets(resetVelocity: Boolean = true) {
        particles.forEach { particle ->
            particle.px = particle.tx
            particle.py = particle.ty
            particle.pz = particle.tz
            if (resetVelocity) {
                particle.vx = 0f
                particle.vy = 0f
                particle.vz = 0f
            }
        }
    }

    fun step(
        dt: Float,
        spring: Float,
        damping: Float,
        time: Float,
        pulseAmplitude: Float = 0f,
        pulseFrequency: Float = 4.5f,
    ) {
        val frameDt = dt.coerceIn(0f, 0.05f)
        particles.forEach { particle ->
            val targetZ = particle.tz + sin(time * pulseFrequency + particle.phaseOffset) * pulseAmplitude

            particle.vx = particle.vx * damping + (particle.tx - particle.px) * spring * frameDt
            particle.vy = particle.vy * damping + (particle.ty - particle.py) * spring * frameDt
            particle.vz = particle.vz * damping + (targetZ - particle.pz) * spring * frameDt

            particle.px += particle.vx * frameDt
            particle.py += particle.vy * frameDt
            particle.pz += particle.vz * frameDt
        }
    }

    fun buildVertexBuffer(radius: Float): ByteArray {
        val vertices = FloatArray(count * 20)
        var offset = 0

        particles.forEach { particle ->
            val left = particle.px - radius
            val right = particle.px + radius
            val top = particle.py + radius
            val bottom = particle.py - radius

            vertices[offset++] = left
            vertices[offset++] = top
            vertices[offset++] = particle.pz
            vertices[offset++] = 0f
            vertices[offset++] = 1f

            vertices[offset++] = right
            vertices[offset++] = top
            vertices[offset++] = particle.pz
            vertices[offset++] = 1f
            vertices[offset++] = 1f

            vertices[offset++] = left
            vertices[offset++] = bottom
            vertices[offset++] = particle.pz
            vertices[offset++] = 0f
            vertices[offset++] = 0f

            vertices[offset++] = right
            vertices[offset++] = bottom
            vertices[offset++] = particle.pz
            vertices[offset++] = 1f
            vertices[offset++] = 0f
        }

        return vertices.toByteArray()
    }

    private fun randomScatterPoint(): ScatterPoint {
        val theta = random.nextFloat() * (PI.toFloat() * 2f)
        val phi = random.nextFloat() * PI.toFloat()
        val radius = 0.7f + random.nextFloat() * 1.0f

        val x = cos(theta) * sin(phi) * radius
        val y = cos(phi) * radius * 0.7f
        val z = sin(theta) * sin(phi) * radius * 0.45f
        return ScatterPoint(x = x, y = y, z = z)
    }
}

private data class ScatterPoint(
    val x: Float,
    val y: Float,
    val z: Float,
)

private fun FloatArray.toByteArray(): ByteArray {
    val bytes = ByteArray(size * Float.SIZE_BYTES)
    forEachIndexed { index, value ->
        val bits = value.toBits()
        val byteOffset = index * Float.SIZE_BYTES
        bytes[byteOffset] = (bits and 0xFF).toByte()
        bytes[byteOffset + 1] = ((bits shr 8) and 0xFF).toByte()
        bytes[byteOffset + 2] = ((bits shr 16) and 0xFF).toByte()
        bytes[byteOffset + 3] = ((bits shr 24) and 0xFF).toByte()
    }
    return bytes
}

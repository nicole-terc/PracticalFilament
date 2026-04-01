package dev.nstv.practicalfilament.filament

internal fun FloatArray.toByteArray(): ByteArray {
    val bytes = ByteArray(size * Float.SIZE_BYTES)
    forEachIndexed { index, value ->
        val bits = value.toBits()
        val offset = index * Float.SIZE_BYTES
        bytes[offset] = (bits and 0xFF).toByte()
        bytes[offset + 1] = ((bits shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((bits shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((bits shr 24) and 0xFF).toByte()
    }
    return bytes
}

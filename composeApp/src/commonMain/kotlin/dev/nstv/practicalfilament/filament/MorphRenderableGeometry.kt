package dev.nstv.practicalfilament.filament

data class MorphRenderableGeometry(
    val positions: FloatArray,
    val uvs: FloatArray,
    val indices: ShortArray,
    val morphTargetPositions: List<FloatArray>,
) {
    val vertexCount: Int
        get() = positions.size / 3

    val morphTargetCount: Int
        get() = morphTargetPositions.size

    init {
        require(positions.size % 3 == 0) { "Positions must contain XYZ triplets" }
        require(uvs.size == vertexCount * 2) { "UV count must match the vertex count" }
        require(morphTargetPositions.all { it.size == positions.size }) {
            "Each morph target must contain the same number of XYZ values as the base positions"
        }
    }

    fun flattenedMorphTargetPositions(): FloatArray {
        val values = FloatArray(morphTargetPositions.sumOf(FloatArray::size))
        var offset = 0
        morphTargetPositions.forEach { target ->
            target.copyInto(values, destinationOffset = offset)
            offset += target.size
        }
        return values
    }
}

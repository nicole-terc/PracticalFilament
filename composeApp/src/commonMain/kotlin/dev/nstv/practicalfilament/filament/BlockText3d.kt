package dev.nstv.practicalfilament.filament

data class BlockTextConfig(
    val text: String,
    val materialInstanceHandle: Int,
    val center: Float3 = Float3(0f, 0f, 0f),
    val cellSize: Float = 0.04f,
    val cellDepth: Float = 0.06f,
    val letterSpacing: Float = 0.035f,
    val wordSpacing: Float = 0.08f,
    val lineSpacing: Float = 0.08f,
    val cubeFill: Float = 0.9f,
)

data class BlockTextLetter(
    val char: Char,
    val handles: List<Int>,
    val center: Float3,
    val width: Float,
)

data class BlockTextResult(
    val handles: List<Int>,
    val letters: List<BlockTextLetter>,
    val width: Float,
    val height: Float,
)

private const val BlockGlyphColumns = 5
private const val BlockGlyphRows = 7
private const val BlockSpaceColumns = 3

private val BlockGlyphs = mapOf(
    'A' to listOf("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
    'B' to listOf("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
    'C' to listOf("01110", "10001", "10000", "10000", "10000", "10001", "01110"),
    'D' to listOf("11110", "10001", "10001", "10001", "10001", "10001", "11110"),
    'E' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
    'F' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "10000"),
    'G' to listOf("01111", "10000", "10000", "10111", "10001", "10001", "01111"),
    'H' to listOf("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
    'I' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "11111"),
    'J' to listOf("00111", "00010", "00010", "00010", "10010", "10010", "01100"),
    'K' to listOf("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
    'L' to listOf("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
    'M' to listOf("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
    'N' to listOf("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
    'O' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    'P' to listOf("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
    'Q' to listOf("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
    'R' to listOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
    'S' to listOf("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
    'T' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
    'U' to listOf("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
    'V' to listOf("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
    'W' to listOf("10001", "10001", "10001", "10101", "10101", "10101", "01010"),
    'X' to listOf("10001", "10001", "01010", "00100", "01010", "10001", "10001"),
    'Y' to listOf("10001", "10001", "01010", "00100", "00100", "00100", "00100"),
    'Z' to listOf("11111", "00001", "00010", "00100", "01000", "10000", "11111"),
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
    '6' to listOf("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
    '.' to listOf("00000", "00000", "00000", "00000", "00000", "00110", "00110"),
    '-' to listOf("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
    '_' to listOf("00000", "00000", "00000", "00000", "00000", "00000", "11111"),
    '!' to listOf("00100", "00100", "00100", "00100", "00100", "00000", "00100"),
    '?' to listOf("01110", "10001", "00001", "00010", "00100", "00000", "00100"),
)

fun FilamentEngine.createBlockText(
    config: BlockTextConfig,
): BlockTextResult {
    val lines = config.text.split('\n')
    val glyphHeight = BlockGlyphRows * config.cellSize
    val height = if (lines.isEmpty()) 0f else {
        lines.size * glyphHeight + (lines.size - 1) * config.lineSpacing
    }
    val lineWidths = lines.map { line ->
        computeBlockLineWidth(
            text = line,
            cellSize = config.cellSize,
            letterSpacing = config.letterSpacing,
            wordSpacing = config.wordSpacing,
        )
    }
    val width = lineWidths.maxOrNull() ?: 0f

    val handles = mutableListOf<Int>()
    val letters = mutableListOf<BlockTextLetter>()
    val topLineCenterY = config.center.y + height * 0.5f - glyphHeight * 0.5f

    lines.forEachIndexed { lineIndex, line ->
        val lineWidth = lineWidths[lineIndex]
        val lineStartX = config.center.x - lineWidth * 0.5f
        val lineCenterY = topLineCenterY - lineIndex * (glyphHeight + config.lineSpacing)
        var cursorX = lineStartX

        line.forEach { rawChar ->
            val advance = blockGlyphAdvance(rawChar, config.cellSize)
            val glyph = glyphFor(rawChar)
            if (glyph != null) {
                val letterHandles = mutableListOf<Int>()
                glyph.forEachIndexed { rowIndex, row ->
                    row.forEachIndexed { columnIndex, cell ->
                        if (cell != '1') return@forEachIndexed
                        val handle = createCubeRenderable(
                            materialInstanceHandle = config.materialInstanceHandle,
                            size = 1f,
                        )
                        if (handle <= 0) return@forEachIndexed
                        val localX = cursorX + columnIndex * config.cellSize + config.cellSize * 0.5f
                        val localY = lineCenterY +
                            ((BlockGlyphRows - 1 - rowIndex) - (BlockGlyphRows - 1) / 2f) * config.cellSize
                        setRenderableTransform(
                            handle,
                            multiplyMatrix4(
                                translationMatrix(localX, localY, config.center.z),
                                scaleMatrix(
                                    config.cellSize * config.cubeFill,
                                    config.cellSize * config.cubeFill,
                                    config.cellDepth,
                                ),
                            ),
                        )
                        handles += handle
                        letterHandles += handle
                    }
                }
                letters += BlockTextLetter(
                    char = rawChar,
                    handles = letterHandles,
                    center = Float3(cursorX + advance * 0.5f, lineCenterY, config.center.z),
                    width = advance,
                )
            }
            cursorX += advance + blockGlyphSpacing(rawChar, config.letterSpacing, config.wordSpacing)
        }
    }

    return BlockTextResult(
        handles = handles,
        letters = letters,
        width = width,
        height = height,
    )
}

private fun glyphFor(char: Char): List<String>? = when {
    char == ' ' -> null
    else -> BlockGlyphs[char.uppercaseChar()] ?: BlockGlyphs['?']
}

private fun blockGlyphAdvance(char: Char, cellSize: Float): Float {
    return if (char == ' ') {
        BlockSpaceColumns * cellSize
    } else {
        BlockGlyphColumns * cellSize
    }
}

private fun blockGlyphSpacing(char: Char, letterSpacing: Float, wordSpacing: Float): Float {
    return if (char == ' ') wordSpacing else letterSpacing
}

private fun computeBlockLineWidth(
    text: String,
    cellSize: Float,
    letterSpacing: Float,
    wordSpacing: Float,
): Float {
    if (text.isEmpty()) return 0f
    var width = 0f
    text.forEachIndexed { index, char ->
        width += blockGlyphAdvance(char, cellSize)
        if (index != text.lastIndex) {
            width += blockGlyphSpacing(char, letterSpacing, wordSpacing)
        }
    }
    return width
}

private fun translationMatrix(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    x, y, z, 1f,
)

private fun scaleMatrix(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    x, 0f, 0f, 0f,
    0f, y, 0f, 0f,
    0f, 0f, z, 0f,
    0f, 0f, 0f, 1f,
)

private fun multiplyMatrix4(left: FloatArray, right: FloatArray): FloatArray {
    val result = FloatArray(16)
    for (column in 0 until 4) {
        for (row in 0 until 4) {
            result[column * 4 + row] =
                left[row] * right[column * 4] +
                    left[4 + row] * right[column * 4 + 1] +
                    left[8 + row] * right[column * 4 + 2] +
                    left[12 + row] * right[column * 4 + 3]
        }
    }
    return result
}

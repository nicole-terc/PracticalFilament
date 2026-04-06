package dev.nstv.practicalfilament.screen.particles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import dev.nstv.practicalfilament.filament.Float2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

actual fun sampleWordPositions(text: String, maxParticles: Int): List<Float2> {
    if (text.isBlank() || maxParticles <= 0) {
        return emptyList()
    }

    val bitmapWidth = 512
    val bitmapHeight = 128
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 96f
    }

    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val scale = min(
        bitmapWidth * 0.9f / bounds.width().coerceAtLeast(1),
        bitmapHeight * 0.7f / bounds.height().coerceAtLeast(1),
    )
    paint.textSize *= scale
    paint.getTextBounds(text, 0, text.length, bounds)

    val drawX = (bitmapWidth - bounds.width()) * 0.5f - bounds.left
    val drawY = (bitmapHeight + bounds.height()) * 0.5f - bounds.bottom
    canvas.drawText(text, drawX, drawY, paint)

    val pixels = IntArray(bitmapWidth * bitmapHeight)
    bitmap.getPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
    bitmap.recycle()

    var solidPixels = 0
    pixels.forEach { pixel ->
        if ((pixel ushr 24) > 32) {
            solidPixels += 1
        }
    }
    if (solidPixels == 0) {
        return emptyList()
    }

    val step = max(1, sqrt(solidPixels.toDouble() / maxParticles.coerceAtLeast(1).toDouble()).toInt())
    val sampled = ArrayList<Point>(maxParticles)
    for (y in 0 until bitmapHeight step step) {
        val rowOffset = y * bitmapWidth
        for (x in 0 until bitmapWidth step step) {
            val pixel = pixels[rowOffset + x]
            if ((pixel ushr 24) > 32) {
                sampled += Point(x.toFloat(), y.toFloat())
            }
        }
    }

    if (sampled.isEmpty()) {
        return emptyList()
    }

    val limited = if (sampled.size <= maxParticles) {
        sampled
    } else {
        List(maxParticles) { index ->
            val sampleIndex = index * sampled.size / maxParticles
            sampled[sampleIndex]
        }
    }

    var minX = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    limited.forEach { point ->
        minX = min(minX, point.x)
        maxX = max(maxX, point.x)
        minY = min(minY, point.y)
        maxY = max(maxY, point.y)
    }

    val centerX = (minX + maxX) * 0.5f
    val centerY = (minY + maxY) * 0.5f
    val width = (maxX - minX).coerceAtLeast(1f)
    val height = (maxY - minY).coerceAtLeast(1f)
    val scaleToWorld = min(1.1f / width, 0.42f / height)

    return limited.map { point ->
        Float2(
            x = (point.x - centerX) * scaleToWorld,
            y = (centerY - point.y) * scaleToWorld,
        )
    }
}

private data class Point(
    val x: Float,
    val y: Float,
)

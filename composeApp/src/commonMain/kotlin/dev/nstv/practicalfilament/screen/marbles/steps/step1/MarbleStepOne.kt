package dev.nstv.practicalfilament.screen.marbles.steps.step1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
internal fun MarbleStepOne(
    modifier: Modifier = Modifier,
    showHighlight: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f),
        ) {
            val radius = size.minDimension * 0.38f
            drawCircle(
                color = Color(0xFF5779AB),
                radius = radius,
                center = center,
            )
            if (showHighlight) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = radius * 0.72f,
                    center = center + Offset(-radius * 0.18f, -radius * 0.16f),
                )
            }
        }
    }
}

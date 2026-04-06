package dev.nstv.practicalfilament.screen.marbles.steps.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.LightConfig
import dev.nstv.practicalfilament.filament.material.Material
import dev.nstv.practicalfilament.screen.marbles.SingleMarbleCamera


@Composable
internal fun SingleMarbleView(
    material: Material,
    modifier: Modifier = Modifier,
    lights: List<LightConfig> = emptyList(),
    autoRotate: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .aspectRatio(1f),
        ) {
            SphereMaterialView(
                modifier = Modifier.fillMaxSize(),
                material = material,
                camera = SingleMarbleCamera,
                lights = lights,
                radius = 1.1f,
                initialRotationX = -12f,
                initialRotationY = 24f,
                autoRotate = autoRotate,
            )
        }
    }
}

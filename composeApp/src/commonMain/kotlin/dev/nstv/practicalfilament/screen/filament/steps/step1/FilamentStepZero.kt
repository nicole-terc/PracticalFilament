package dev.nstv.practicalfilament.screen.filament.steps.step1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.filament.Float3
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout

@Composable
internal fun FilamentStepZero(
    modifier: Modifier = Modifier,
) {

    val ballColor = Color(.21f, 0.1f, 0.61f)

    SampleScreenLayout(
        title = "0. Canvas",
        modifier = modifier,
        showControlsTitle = false,
        view = {
            Box(modifier = Modifier.fillMaxSize()) {
                FilamentView(
                    modifier = Modifier.fillMaxSize(),
                    camera = SingleMarbleCamera,
                    lights = emptyList(),
                )
                Box(
                    modifier = Modifier.size(100.dp).align(Alignment.Center).background(ballColor, shape = CircleShape)
                )
            }
        },
    )
}

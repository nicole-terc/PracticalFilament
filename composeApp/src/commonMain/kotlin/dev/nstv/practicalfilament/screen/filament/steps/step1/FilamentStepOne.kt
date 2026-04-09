package dev.nstv.practicalfilament.screen.filament.steps.step1

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout

@Composable
internal fun FilamentStepOne(
    modifier: Modifier = Modifier,
) {
    var engineReady by remember { mutableStateOf(false) }

    SampleScreenLayout(
        title = "1. Engine",
        modifier = modifier,
        showControlsTitle = false,
        view = {
            Box(modifier = Modifier.fillMaxSize()) {
                FilamentView(
                    modifier = Modifier.fillMaxSize(),
                    camera = SingleMarbleCamera,
                    lights = emptyList(),
                    onEngineReady = { engineReady = true },
                )
                Text(
                    text = if (engineReady) "Engine ready ✓" else "Initializing…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = Grid.One),
                )
            }
        },
    )
}

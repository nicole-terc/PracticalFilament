package dev.nstv.practicalfilament.screen.filament.steps.step2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackgroundFilament
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout

@Composable
internal fun FilamentStepTwo(
    modifier: Modifier = Modifier,
) {
    SampleScreenLayout(
        title = "2. FilamentView",
        modifier = modifier,
        showControlsTitle = false,
        view = {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = SingleMarbleCamera,
                lights = emptyList(),
                backgroundColor = MarbleUiBackgroundFilament,
            )
        },
    )
}

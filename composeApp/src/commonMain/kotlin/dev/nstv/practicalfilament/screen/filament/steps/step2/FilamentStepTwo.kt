package dev.nstv.practicalfilament.screen.filament.steps.step2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.screen.filament.steps.components.FilamentStepView
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackgroundFilament
import dev.nstv.practicalfilament.screen.marbles.components.SingleMarbleCamera

@Composable
internal fun FilamentStepTwo(
    modifier: Modifier = Modifier,
) {
    FilamentStepView(modifier = modifier) {
        FilamentView(
            modifier = Modifier.fillMaxSize(),
            camera = SingleMarbleCamera,
            lights = emptyList(),
            backgroundColor = MarbleUiBackgroundFilament,
            clipShape = FilamentClipShape.Circle,
        )
    }
}

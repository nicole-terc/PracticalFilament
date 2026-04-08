package dev.nstv.practicalfilament.screen.scenes.water

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout

@Composable
fun WaterScreen(
    modifier: Modifier = Modifier,
) {
    var engine by remember { mutableStateOf<FilamentEngine?>(null) }
    var viewportHeightPx by remember { mutableStateOf(0) }

    SampleScreenLayout(
        modifier = modifier,
        title = "Water",
        view = {
            FilamentView {

            }
        }
    )
}

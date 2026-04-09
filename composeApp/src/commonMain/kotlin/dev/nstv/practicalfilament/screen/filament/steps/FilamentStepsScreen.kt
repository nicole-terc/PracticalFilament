package dev.nstv.practicalfilament.screen.filament.steps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nstv.practicalfilament.screen.HideOptions
import dev.nstv.practicalfilament.screen.filament.steps.step1.FilamentStepOne
import dev.nstv.practicalfilament.screen.filament.steps.step2.FilamentStepTwo
import dev.nstv.practicalfilament.screen.filament.steps.step3.FilamentStepThree
import dev.nstv.practicalfilament.screen.filament.steps.step4.FilamentStepFour
import dev.nstv.practicalfilament.screen.filament.steps.step5.FilamentStepFive
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows

private enum class DemoStep(val label: String) {
    ONE("1. Engine"),
    TWO("2. FilamentView"),
    THREE("3. Geometry"),
    FOUR("4. Material"),
    FIVE("5. Lights"),
}

@Composable
fun FilamentStepsScreen(
    modifier: Modifier = Modifier,
) {
    var selectedStep by rememberSaveable { mutableStateOf(DemoStep.ONE) }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        if (!HideOptions) {
            DropDownWithArrows(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Grid.Two),
                options = DemoStep.entries.map { it.label },
                selectedIndex = DemoStep.entries.indexOf(selectedStep),
                onSelectionChanged = { selectedStep = DemoStep.entries[it] },
                textStyle = MaterialTheme.typography.titleMedium,
                loopSelection = true,
            )
            HorizontalDivider()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            key(selectedStep) {
                when (selectedStep) {
                    DemoStep.ONE -> FilamentStepOne()
                    DemoStep.TWO -> FilamentStepTwo()
                    DemoStep.THREE -> FilamentStepThree()
                    DemoStep.FOUR -> FilamentStepFour()
                    DemoStep.FIVE -> FilamentStepFive()
                }
            }
        }
    }
}

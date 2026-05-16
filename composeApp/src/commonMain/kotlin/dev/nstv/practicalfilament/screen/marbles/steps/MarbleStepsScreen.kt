package dev.nstv.practicalfilament.screen.marbles.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.nstv.practicalfilament.screen.HideOptions
import dev.nstv.practicalfilament.screen.marbles.components.CeramicPresetIndex
import dev.nstv.practicalfilament.screen.marbles.components.MarbleAliveLights
import dev.nstv.practicalfilament.screen.marbles.components.MarblePresets
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackground
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiMuted
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiText
import dev.nstv.practicalfilament.screen.marbles.components.NeutralSphereMaterial
import dev.nstv.practicalfilament.screen.marbles.components.SphereStepLights
import dev.nstv.practicalfilament.screen.marbles.steps.components.SingleMarbleView
import dev.nstv.practicalfilament.screen.marbles.steps.step1.MarbleStepOne
import dev.nstv.practicalfilament.screen.marbles.steps.step5.MarbleStepFive
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.DropDownWithArrows

private enum class DemoStep(
    val label: String,
) {
    ONE(
        label = "1. Flat Circle",
    ),
    TWO(
        label = "2. Filament Sphere",
    ),
    THREE(
        label = "3. Tweak Material",
    ),
    FOUR(
        label = "4. Add more Light",
    ),
    FIVE(
        label = "5. UI System",
    ),
}

@Composable
fun MarbleStepsScreen(
    modifier: Modifier = Modifier,
) {
    var selectedStep by rememberSaveable { mutableStateOf(DemoStep.FIVE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MarbleUiBackground),
    ) {
        if (!HideOptions) {
            CompositionLocalProvider(LocalContentColor provides MarbleUiText) {
                DropDownWithArrows(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Grid.Two),
                    options = DemoStep.entries.map { it.label },
                    selectedIndex = DemoStep.entries.indexOf(selectedStep),
                    onSelectionChanged = { selectedStep = DemoStep.entries[it] },
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = MarbleUiText,
                        fontWeight = FontWeight.Medium,
                    ),
                    loopSelection = true,
                )
            }

            HorizontalDivider(color = MarbleUiMuted.copy(alpha = 0.2f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(Grid.Two),
            contentAlignment = Alignment.Center,
        ) {
            when (selectedStep) {
                DemoStep.ONE -> MarbleStepOne()
                DemoStep.TWO -> key(selectedStep) {
                    SingleMarbleView(
                        material = NeutralSphereMaterial,
                        lights = SphereStepLights,
                    )
                }

                DemoStep.THREE -> key(selectedStep) {
                    SingleMarbleView(
                        material = MarblePresets[CeramicPresetIndex],
                        lights = SphereStepLights,
                    )
                }

                DemoStep.FOUR -> key(selectedStep) {
                    SingleMarbleView(
                        material = MarblePresets[CeramicPresetIndex],
                        lights = MarbleAliveLights,
                        autoRotate = true,
                    )
                }

                DemoStep.FIVE -> MarbleStepFive()
            }
        }
    }
}

package dev.nstv.practicalfilament.screen.marbles.comparison

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun EffectsMarblePanel(
    preset: ComparisonPresetSpec,
    effectMode: Android2DEffectMode,
    animationTimeSeconds: Float,
    backgroundEnabled: Boolean,
    modifier: Modifier = Modifier,
)

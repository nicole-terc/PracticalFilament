package dev.nstv.practicalfilament.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
internal actual fun AndroidEffectsMarblePanel(
    preset: ComparisonPresetSpec,
    effectMode: Android2DEffectMode,
    animationTimeSeconds: Float,
    backgroundEnabled: Boolean,
    modifier: Modifier,
) {
    ComparisonPanelCard(
        modifier = modifier,
        title = "Android Effects",
        presetLabel = preset.preset.label,
        note = "RenderEffect and AGSL are Android-only.",
        supportText = "This side of the comparison is unavailable on iOS.",
    ) {
        CircularComparisonStage(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "RenderEffect and AGSL are Android-only. This side of the comparison is unavailable on iOS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MarbleUiText,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

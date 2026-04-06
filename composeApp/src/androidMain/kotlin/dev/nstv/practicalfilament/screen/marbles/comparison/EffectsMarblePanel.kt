package dev.nstv.practicalfilament.screen.marbles.comparison

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
internal actual fun EffectsMarblePanel(
    preset: ComparisonPresetSpec,
    effectMode: Android2DEffectMode,
    animationTimeSeconds: Float,
    backgroundEnabled: Boolean,
    modifier: Modifier,
) {
    val agslSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ComparisonPanelCard(
        modifier = modifier,
        title = "Android Effects",
        presetLabel = preset.preset.label,
        supportText = if (effectMode == Android2DEffectMode.AGSL && !agslSupported) {
            "AGSL requires Android 13+"
        } else {
            null
        },
    ) {
        when {
            effectMode == Android2DEffectMode.AGSL && agslSupported -> {
                AgslMarbleStage(
                    preset = preset,
                    animationTimeSeconds = animationTimeSeconds,
                    backgroundEnabled = backgroundEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> {
                RenderEffectMarbleStage(
                    preset = preset,
                    animationTimeSeconds = animationTimeSeconds,
                    backgroundEnabled = backgroundEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

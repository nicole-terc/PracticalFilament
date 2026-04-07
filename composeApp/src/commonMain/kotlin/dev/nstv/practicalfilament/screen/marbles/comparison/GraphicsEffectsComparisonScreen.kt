package dev.nstv.practicalfilament.screen.marbles.comparison

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.nstv.practicalfilament.filament.withFrameSeconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.filament.FilamentHostViewMode
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiAccent
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiBackground
import dev.nstv.practicalfilament.screen.marbles.components.MarbleUiMuted
import dev.nstv.practicalfilament.theme.Grid
import dev.nstv.practicalfilament.theme.components.SampleScreenLayout

@Composable
fun GraphicsEffectsComparisonScreen(
    modifier: Modifier = Modifier,
) {
    var presentationMode by remember { mutableStateOf(ComparisonPresentationMode.Reveal) }
    var effectMode by remember { mutableStateOf(Android2DEffectMode.AGSL) }
    var selectedPreset by remember { mutableStateOf(ComparisonMaterialPreset.Ceramic) }
    var animationEnabled by remember { mutableStateOf(true) }
    var backgroundEnabled by remember { mutableStateOf(false) }
    var revealFraction by remember { mutableFloatStateOf(0.5f) }
    var animationTimeSeconds by remember { mutableFloatStateOf(0f) }

    val presetSpec = selectedPreset.toSpec()

    LaunchedEffect(animationEnabled) {
        if (!animationEnabled) return@LaunchedEffect
        val startTime = animationTimeSeconds
        withFrameSeconds { elapsed, _ ->
            animationTimeSeconds = startTime + elapsed
        }
    }

    SampleScreenLayout(
        title = "Graphics Effects Comparison",
        modifier = modifier,
        view = {
            ComparisonStage(
                presentationMode = presentationMode,
                preset = presetSpec,
                effectMode = effectMode,
                animationTimeSeconds = animationTimeSeconds,
                backgroundEnabled = backgroundEnabled,
                revealFraction = revealFraction,
                onRevealFractionChanged = { revealFraction = it },
            )
        },
        controls = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = Grid.One),
                verticalArrangement = Arrangement.spacedBy(Grid.Two),
            ) {
                EnumChipRow(
                    label = "View",
                    options = ComparisonPresentationMode.entries,
                    selected = presentationMode,
                    labelFor = { it.label },
                    onSelected = { presentationMode = it },
                )
                EnumChipRow(
                    label = "2D Effect",
                    options = Android2DEffectMode.entries,
                    selected = effectMode,
                    labelFor = { it.label },
                    onSelected = { effectMode = it },
                )
                EnumChipRow(
                    label = "Preset",
                    options = ComparisonMaterialPreset.entries,
                    selected = selectedPreset,
                    labelFor = { it.label },
                    onSelected = { selectedPreset = it },
                )
                EnumChipRow(
                    label = "Animate",
                    options = listOf(true, false),
                    selected = animationEnabled,
                    labelFor = { if (it) "On" else "Off" },
                    onSelected = { animationEnabled = it },
                )
                EnumChipRow(
                    label = "Background",
                    options = listOf(true, false),
                    selected = backgroundEnabled,
                    labelFor = { if (it) "On" else "Off" },
                    onSelected = { backgroundEnabled = it },
                )
                if (presentationMode == ComparisonPresentationMode.Reveal) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Grid.Half),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Reveal",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = "${(revealFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MarbleUiMuted,
                            )
                        }
                        Slider(
                            value = revealFraction,
                            onValueChange = { revealFraction = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MarbleUiAccent,
                                activeTrackColor = MarbleUiAccent,
                                inactiveTrackColor = MarbleUiMuted.copy(
                                    alpha = 0.28f
                                ),
                            ),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ComparisonStage(
    presentationMode: ComparisonPresentationMode,
    preset: ComparisonPresetSpec,
    effectMode: Android2DEffectMode,
    animationTimeSeconds: Float,
    backgroundEnabled: Boolean,
    revealFraction: Float,
    onRevealFractionChanged: (Float) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(Grid.Two),
    ) {
        val isWide = maxWidth >= 720.dp
        val androidPanel: @Composable BoxScope.() -> Unit = {
            EffectsMarblePanel(
                preset = preset,
                effectMode = effectMode,
                animationTimeSeconds = animationTimeSeconds,
                backgroundEnabled = backgroundEnabled,
                modifier = Modifier.fillMaxSize(),
            )
        }
        val filamentPanel: @Composable BoxScope.() -> Unit = {
            key(backgroundEnabled) {
                FilamentComparisonPanel(
                    preset = preset,
                    animationTimeSeconds = animationTimeSeconds,
                    backgroundEnabled = backgroundEnabled,
                    modifier = Modifier.fillMaxSize(),
                    hostViewMode = if (presentationMode == ComparisonPresentationMode.Reveal) {
                        FilamentHostViewMode.Texture
                    } else {
                        FilamentHostViewMode.Auto
                    },
                )
            }
        }

        when (presentationMode) {
            ComparisonPresentationMode.Split -> {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(Grid.Two),
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize(), content = androidPanel)
                        Box(modifier = Modifier.weight(1f).fillMaxSize(), content = filamentPanel)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Grid.Two),
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), content = androidPanel)
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), content = filamentPanel)
                    }
                }
            }

            ComparisonPresentationMode.Reveal -> {
                RevealComparisonStage(
                    modifier = Modifier.fillMaxSize(),
                    revealFraction = revealFraction,
                    onRevealFractionChanged = onRevealFractionChanged,
                    bottomLayer = androidPanel,
                    topLayer = filamentPanel,
                )
            }
        }
    }
}

@Composable
private fun RevealComparisonStage(
    revealFraction: Float,
    onRevealFractionChanged: (Float) -> Unit,
    bottomLayer: @Composable BoxScope.() -> Unit,
    topLayer: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    var stageWidthPx by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                stageWidthPx = coordinates.size.width.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(stageWidthPx) {
                detectDragGestures(
                    onDragStart = { start ->
                        onRevealFractionChanged((start.x / stageWidthPx).coerceIn(0f, 1f))
                    },
                ) { change, _ ->
                    onRevealFractionChanged((change.position.x / stageWidthPx).coerceIn(0f, 1f))
                }
            },
    ) {
        Box(modifier = Modifier.fillMaxSize(), content = bottomLayer)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RevealLayerShape(revealFraction)),
            content = topLayer,
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineX = size.width * revealFraction
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(lineX, 0f),
                end = Offset(lineX, size.height),
                strokeWidth = 2.dp.toPx(),
            )
            drawCircle(
                color = MarbleUiAccent,
                radius = 18.dp.toPx(),
                center = Offset(lineX, size.height * 0.5f),
            )
            drawCircle(
                color = MarbleUiBackground,
                radius = 15.dp.toPx(),
                center = Offset(lineX, size.height * 0.5f),
            )
            drawLine(
                color = MarbleUiAccent,
                start = Offset(lineX - 5.dp.toPx(), size.height * 0.5f),
                end = Offset(lineX + 5.dp.toPx(), size.height * 0.5f),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

private class RevealLayerShape(
    private val revealFraction: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val width = (size.width * revealFraction.coerceIn(0f, 1f)).coerceIn(0f, size.width)
        return Outline.Rectangle(Rect(0f, 0f, width, size.height))
    }
}

@Composable
private fun <T> EnumChipRow(
    label: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Grid.Half),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Grid.One),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(labelFor(option)) },
                )
            }
        }
    }
}

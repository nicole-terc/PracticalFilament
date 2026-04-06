package dev.nstv.practicalfilament.screen.marbles.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nstv.practicalfilament.filament.FilamentClipShape
import dev.nstv.practicalfilament.filament.FilamentEngine
import dev.nstv.practicalfilament.filament.FilamentHostViewMode
import dev.nstv.practicalfilament.filament.FilamentView
import dev.nstv.practicalfilament.screen.marbles.ComparisonMarbleCamera
import dev.nstv.practicalfilament.screen.marbles.MarbleAliveLights
import dev.nstv.practicalfilament.screen.marbles.MarbleUiAccent
import dev.nstv.practicalfilament.screen.marbles.MarbleUiBackground
import dev.nstv.practicalfilament.screen.marbles.MarbleUiBackgroundFilament
import dev.nstv.practicalfilament.screen.marbles.MarbleUiMuted
import dev.nstv.practicalfilament.screen.marbles.MarbleUiText
import dev.nstv.practicalfilament.theme.Grid
import practicalfilament.composeapp.generated.resources.Res

private const val ComparisonIblPath = "files/envs/studio_small_02_2k/studio_small_02_2k_ibl.ktx"

@Composable
internal fun FilamentComparisonPanel(
    preset: ComparisonPresetSpec,
    animationTimeSeconds: Float,
    backgroundEnabled: Boolean,
    modifier: Modifier = Modifier,
    hostViewMode: FilamentHostViewMode = FilamentHostViewMode.Auto,
) {
    ComparisonPanelCard(
        modifier = modifier,
        title = "Filament",
        presetLabel = preset.preset.label,
    ) {
        var engineReady by remember { mutableStateOf<FilamentEngine?>(null) }
        var renderableHandle by remember { mutableIntStateOf(0) }

        LaunchedEffect(engineReady, backgroundEnabled) {
            val engine = engineReady ?: return@LaunchedEffect
            if (backgroundEnabled) {
                val indirectLightHandle = engine.loadIndirectLight(Res.getUri(ComparisonIblPath))
                if (indirectLightHandle > 0) {
                    engine.setIndirectLight(indirectLightHandle, intensity = 35_000f)
                    engine.requestFrame()
                }
            }
        }

        LaunchedEffect(engineReady, preset.preset) {
            val engine = engineReady ?: return@LaunchedEffect
            if (renderableHandle != 0) {
                engine.removeRenderable(renderableHandle)
                renderableHandle = 0
            }

            val loaded = engine.loadMaterial(preset.filamentMaterial)
            loaded.parameters.values.forEach { engine.setMaterialParameter(loaded.instanceHandle, it) }
            renderableHandle = engine.createSphereRenderable(
                materialInstanceHandle = loaded.instanceHandle,
                radius = 1.16f,
            )
            engine.requestFrame()
        }

        CircularComparisonStage(modifier = Modifier.fillMaxSize()) {
            FilamentView(
                modifier = Modifier.fillMaxSize(),
                camera = ComparisonMarbleCamera,
                lights = MarbleAliveLights,
                backgroundColor = MarbleUiBackgroundFilament,
                clipShape = FilamentClipShape.Circle,
                hostViewMode = hostViewMode,
                onEngineReady = { engineReady = it },
            )
        }
    }
}

@Composable
internal fun ComparisonPanelCard(
    title: String,
    presetLabel: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    supportText: String? = null,
    stageScale: Float = 0.82f,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MarbleUiBackground.copy(alpha = 0.98f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0E1520),
                            Color(0xFF16212F),
                        ),
                        start = Offset.Zero,
                        end = Offset(1200f, 900f),
                    ),
                )
                .padding(Grid.Two),
            verticalArrangement = Arrangement.spacedBy(Grid.One),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MarbleUiText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PresetChip(label = presetLabel)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stageScale)
                        .aspectRatio(1f),
                    content = content,
                )
            }

            note?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MarbleUiMuted,
                )
            }
            supportText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MarbleUiAccent,
                )
            }
        }
    }
}

@Composable
internal fun CircularComparisonStage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF233244),
                        Color(0xFF101721),
                    ),
                    center = Offset(220f, 180f),
                    radius = 820f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PresetChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MarbleUiAccent.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MarbleUiAccent,
        )
    }
}
